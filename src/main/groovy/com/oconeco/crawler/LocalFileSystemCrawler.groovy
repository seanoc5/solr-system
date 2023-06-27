package com.oconeco.crawler

import com.oconeco.analysis.BaseAnalyzer
import com.oconeco.models.FSFile
import com.oconeco.models.FSFolder
import com.oconeco.models.SavableObject
import com.oconeco.persistence.BaseClient
import com.oconeco.persistence.SolrSystemClient
import groovy.io.FileType
import groovy.io.FileVisitResult
import org.apache.log4j.Logger
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList

import java.nio.file.Path
import java.util.regex.Matcher
/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   10/15/22, Saturday
 * @description: class to crawl local filesystems
 */

/**
 * Simple local fs crawler, intended to be created for each crawl name/startfolder in a given location
 * review if there is a benefit to having a persistent crawler over multiple folders/names in a given location/source
 */
class LocalFileSystemCrawler {
    static log = Logger.getLogger(this.class.name);
    String crawlName        // todo -- remove this from class, leave as arg in methods where appropriate
    String locationName
    String osName
    BaseDifferenceChecker differenceChecker
    BaseClient persistenceClient
//    String checkFolderFields = SolrSystemClient.DEFAULT_FIELDS_TO_CHECK.join(' ')
//    String checkFileFields = SolrSystemClient.DEFAULT_FIELDS_TO_CHECK.join(' ')
    BaseAnalyzer analyzer

    LocalFileSystemCrawler(String locationName, BaseClient persistenceClient, BaseDifferenceChecker diffChecker, BaseAnalyzer analyzer) {
        log.debug "${this.class.simpleName} constructor with location:$locationName, Client($persistenceClient) DiffChecker($differenceChecker) analyzer($analyzer) "
        this.locationName = locationName
        osName = System.getProperty("os.name")
        // moved this from config file to here in case we start supporting remote filesystems...? (or WSL...?)
        this.differenceChecker = diffChecker
        this.persistenceClient = persistenceClient
        this.analyzer = analyzer
    }


    /**
     * helper method to accept various inputs,and return the filesystem object (File/dir) to actuallly crawl
     * @param source
     * @param namePatterns
     * @return
     */
    File getStartDirectory(def source) {
        File startDir = null
        if (source instanceof Path) {
            log.info "got a proper path (${source.class.name}): $source"
            startDir = source.toFile()
        } else if (source instanceof File) {
            startDir = source
        } else if (source instanceof String) {
            startDir = new File(source)
        } else {
            log.warn "Unknown source type (${source.class.crawlName}, trying a blind cast..."
            startDir = ((Path) source)
        }
        return startDir
    }


    def getSolrDocCount(String crawlName = '', String q = '*:*', String filterQuery = '') {
        SolrQuery solrQuery = new SolrQuery(q)
        solrQuery.setFields('')
        solrQuery.setRows(0)
        if (locationName) {
            solrQuery.addFilterQuery("${SolrSystemClient.FLD_LOCATION_NAME}:\"$locationName\"")
        }
        if (crawlName) {
            solrQuery.addFilterQuery("${SolrSystemClient.FLD_CRAWL_NAME}:\"$crawlName\"")
        }
        if (filterQuery) {
            solrQuery.addFilterQuery(filterQuery)
        }
        QueryResponse response = persistenceClient.query(solrQuery)
        long numFound = response.results.getNumFound()
        log.debug "\t\tLocalFSCrawler getSolrDocCount: $numFound -- $this"
        return numFound
    }


//    Map<String, List<FSFolder>> crawlFolders(String crawlName, File startFolder, Pattern ignoreFolders, Pattern ignoreFiles, boolean compareExistingSolrFolderDocs = true, FolderAnalyzer folderAnalyzer=null, FileAnalyzer fileAnalyzer=null) {
//        Map<String, SolrDocument> existingSolrFolderDocs
//        if (compareExistingSolrFolderDocs) {
//            existingSolrFolderDocs = getSolrFolderDocs(crawlName)
//            log.debug "\t\t$crawlName) compareExistingSolrFolderDocs set to true, existingSolrFolderDocs:(${existingSolrFolderDocs.size()})"
//        } else {
//            log.debug "\t\t$crawlName) compareExistingSolrFolderDocs set to false, so leave existingSolrFolderDocs empty/null to skip, full read/indexing"
//        }
//        Map<String, List<FSFolder>> results = crawlFolders(crawlName, startFolder, ignoreFolders, ignoreFiles, existingSolrFolderDocs, folderAnalyzer, fileAnalyzer)
//        return results
//    }

    /**
     * Basic crawl functionality here, with visitor pattern
     * @param crawlName (subset of locationName/source)
     * @param startDir folder to start crawl
     * @param ignoreFolders regex to indicate we should save the current folder, mark as ignore, and not descend any further
     * @param ignoreFiles regex to indicate any files to not save/index
     * @return map of status counts
     */
    Map<String, List<FSFolder>> crawlFolders(String crawlName, File startDir, Map<String, SolrDocument> existingSolrFolderDocs, BaseAnalyzer analyzer) {
        Map<String, List<FSFolder>> results = [:].withDefault { [] }

        log.info "$locationName:$crawlName -> Starting crawl of folder: ($startDir) [existing solrFolderDocs:${existingSolrFolderDocs?.size()}]"
        Path startPath = startDir.toPath()   // Path so we can 'relativize` to get relative depth of this crawl
        FSFolder currentFolder = null

        def doPreDir = {
            File f = it
            log.debug "\t\t====PRE dir:  $it"
            FileVisitResult fvr = FileVisitResult.CONTINUE
            boolean accessible = f.exists() && f.canRead() && f.canExecute()

            int depth = getRelativeDepth(startPath, f)
            currentFolder = new FSFolder(f, null, this.locationName, this.crawlName)
//            results << currentFolder

            if (accessible) {
                Matcher matcher = (it.name =~ ignoreFolders)
                if (matcher.matches()) {
                    currentFolder.ignore = true
                    log.debug "\t\t----Ignorable folder:($currentFolder) -- matching part: ${matcher.group()} -- Pattern: $ignoreFolders"
                    fvr = FileVisitResult.SKIP_SUBTREE
                } else {
                    log.debug "\t\t----Not ignorable folder: $currentFolder"
                    fvr = FileVisitResult.CONTINUE
                }
            } else {
                fvr = FileVisitResult.SKIP_SUBTREE
                log.warn "Folder: $currentFolder is not accessible, skip subtree: $f"
            }
            return fvr
        }

        def doPostDir = { log.debug "\t\tPost dir: $it" }

        Map options = [
                type     : FileType.DIRECTORIES,
                preDir   : doPreDir,
                postDir  : doPostDir,
                preRoot  : true,
                postRoot : true,
                visitRoot: true,
        ]

        long cnt = 0
        int cntStatusFrequency = 500 // show log message every cntStatusFrequency folders
        startDir.traverse(options) { File folder ->
            cnt++
            if (cnt % cntStatusFrequency == 0) {
                log.info "\t\t\t$cnt) traverse folder $folder"     // should there be action here?
            } else {
                log.debug "\t\t$cnt) traverse folder $folder"     // should there be action here?
            }

            DifferenceStatus differenceStatus = differenceChecker.compareFSFolderToSavedDocMap(currentFolder, existingSolrFolderDocs)
            boolean shouldUpdate = differenceChecker.shouldUpdate(differenceStatus)
            if (currentFolder.ignore) {
                if(shouldUpdate) {
                    log.info "\t\tignore Folder: $currentFolder -- but save basic folder info (with ignore flag set) for reference ($currentFolder.path)"
                    def response = persistenceClient.saveObjects([currentFolder])
                } else {
                    log.debug "\t\tIgnore folder and no need for update: $currentFolder"
                }
                results.skipped << currentFolder
            } else {
                if (shouldUpdate) {
                    List<SavableObject> folderObjects = currentFolder.buildChildrenList(ignoreFiles)
                    log.debug "Update FSFolder:($currentFolder) -- Objects count: ${folderObjects.size()} -- diffStatus: $differenceStatus"
                    if(analyzer){     // todo -- refactor to make folder/file analyser more elegant/readable
                        analyzer.analyze(currentFolder)
                        folderObjects.each{
                            if(it.type==FSFolder.TYPE){
                                analyzer.analyze(it)
                            } else if(it.type== FSFile.TYPE){
                                fileAnalyzer.analyze(it)
                            } else {
                                log.warn "Unknown Savable Object type??? $it"
                            }
                        }
                    }
                    folderObjects.add(currentFolder)
                    def response = persistenceClient.saveObjects(folderObjects)
                    log.info "\t\t++++Save folder (${currentFolder.path}:${currentFolder.depth}) -- Differences:${differenceStatus.differences} -- response: $response"
                    results.updated << currentFolder
                } else {
                    results.skipped << currentFolder
                    log.debug "\t\tno need to update: $differenceStatus"
                }
            }
        }
        return results
    }


    /**
     * helper func to get relative depth (from crawl's start folder)
     * @param startPath - original start path
     * @param f current file,
     * @return count of levels between startpath and current path
     */
    public int getRelativeDepth(Path startPath, File f) {
        Path reletivePath = startPath.relativize(f.toPath())
        String relPath = reletivePath.toString()
        int depth = relPath > '' ? reletivePath.getNameCount() : 0
        depth
    }


    /**
     * get the 'folder' documents for a given crawler (i.e. location name and crawl name)
     * @param crawler
     * @param q
     * @param fq
     * @param fl
     * @return
     */
    Map<String, SolrDocument> getSolrFolderDocs(String crawlName, String fq = "type_s:${FSFolder.TYPE}", String fl = this.checkFolderFields, int maxRowsReturned = SolrSystemClient.MAX_ROWS_RETURNED) {
        SolrQuery sq = new SolrQuery('*:*')
        sq.setRows(maxRowsReturned)
        sq.setFilterQueries(fq)
        // add filter queries to further limit
        String filterLocation = SolrSystemClient.FLD_LOCATION_NAME + ':' + locationName
        sq.addFilterQuery(filterLocation)
        String filterCrawl = SolrSystemClient.FLD_CRAWL_NAME + ':' + crawlName
        sq.addFilterQuery(filterCrawl)

        sq.setFields(fl)

        QueryResponse response = persistenceClient.query(sq)
        SolrDocumentList docs = response.results
        long numFound = response.results.numFound
        if (docs.size() == maxRowsReturned) {
            log.warn "getExistingFolders returned lots of rows (${docs.size()}) which equals our upper limit: ${maxRowsReturned}, this is almost certainly a problem.... ${sq}}"
        } else if (numFound == 0) {
            log.info "No previous/existing solr docs found: $sq"
        } else {
            log.debug "\t\tGet existing solr folder docs map, size: ${docs.size()} -- Filters: ${sq.getFilterQueries()}"
        }
        Map<String, SolrDocument> docsMap = docs.groupBy { it.getFirstValue('id') }
        return docsMap
    }


    @Override
    String toString() {
        return "LocalFileSystemCrawler{" + "crawlName='" + crawlName + '\'' + ", locationName='" + locationName + '\'' + '}';
    }

}
