package com.oconeco.crawler

import com.oconeco.analysis.BaseAnalyzer
import com.oconeco.models.FSFile
import com.oconeco.models.FSFolder
import com.oconeco.models.FSObject
import com.oconeco.models.SavableObject
import com.oconeco.persistence.BaseClient
import com.oconeco.persistence.SolrSystemClient
import groovy.io.FileType
import groovy.io.FileVisitResult
import org.apache.logging.log4j.LogManager
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList

import java.nio.file.Path

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
    static log = LogManager.getLogger(this.class.name);
//    String crawlName        // todo -- remove this from class, leave as arg in methods where appropriate
    String locationName
    String osName
//    BaseDifferenceChecker differenceChecker
    SolrDifferenceChecker differenceChecker
    BaseClient persistenceClient

    // todo -- fix these fied lists, put in constructor, or move to diff checker
    String checkFolderFields = SolrSystemClient.DEFAULT_FIELDS_TO_CHECK.join(' ')
    String checkFileFields = SolrSystemClient.DEFAULT_FIELDS_TO_CHECK.join(' ')

    LocalFileSystemCrawler(String locationName, BaseClient persistenceClient, BaseDifferenceChecker diffChecker) {
        log.debug "${this.class.simpleName} constructor with location:$locationName, Client($persistenceClient) DiffChecker($differenceChecker)"
        this.locationName = locationName
        osName = System.getProperty("os.name")
        // moved this from config file to here in case we start supporting remote filesystems...? (or WSL...?)
        this.differenceChecker = diffChecker
        this.persistenceClient = persistenceClient
    }


//    /**
//     * helper method to accept various inputs,and return the filesystem object (File/dir) to actuallly crawl
//     * @param source
//     * @param namePatterns
//     * @return
//     */
//    File getStartDirectory(def source) {
//        File startDir = null
//        if (source instanceof Path) {
//            log.info "got a proper path (${source.class.name}): $source"
//            startDir = source.toFile()
//        } else if (source instanceof File) {
//            startDir = source
//        } else if (source instanceof String) {
//            startDir = new File(source)
//        } else {
//            log.warn "Unknown source type (${source.class.name}, trying a blind cast..."
//            startDir = ((Path) source)
//        }
//        return startDir
//    }


    def getSolrDocCount(String crawlName = '', String q = '*:*', String filterQuery = '') {
        SolrQuery solrQuery = new SolrQuery(q)
        solrQuery.setFields('*')
        solrQuery.setRows(1)
        solrQuery.setFacet(true)
        solrQuery.setFacetMinCount(1)
        solrQuery.addFacetField(SolrSystemClient.FLD_TYPE)
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
        def facets = response.getFacetField(SolrSystemClient.FLD_TYPE)
        log.debug "\t\tLocalFSCrawler getSolrDocCount: $numFound -- $this"
        Map map = [numFound: numFound]
        facets.getValues()?.each {
            String field = it.name
            long cnt = it.count
            map.put(field, cnt)
        }
        return map
    }


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

        log.info "\t\tcrawlFolders() loc=$locationName:cn=$crawlName -> Starting crawl of folder: ($startDir) [existing solrFolderDocs:${existingSolrFolderDocs?.size()}]"
        Path startPath = startDir.toPath()   // Path so we can 'relativize` to get relative depth of this crawl
        FSFolder currentFolder = null

        def doPreDir = {
            File f = it
            log.info "====PRE dir:  $it"
            FileVisitResult fvr = FileVisitResult.CONTINUE      // set default to continue/process
            boolean accessible = f.exists() && f.canRead() && f.canExecute()

            int depth = getRelativeDepth(startPath, f)
            currentFolder = new FSFolder(f, null, this.locationName, crawlName)
            boolean shouldIgnore = analyzer.shouldIgnore(currentFolder)

            if (accessible) {
                if (shouldIgnore) {
                    log.info "\t\t----IGNORABLE folder:($currentFolder)"
                    fvr = FileVisitResult.SKIP_SUBTREE
                } else {
                    log.debug "\t\t----Not ignorable folder: $currentFolder"
                    def folderResults = crawlFolderFiles(currentFolder, analyzer)
                    //NOte compareFSFolderToSavedDocMap will save diff status in object, for postDir assessment/analysis
                    DifferenceStatus differenceStatus = differenceChecker.compareFSFolderToSavedDocMap(currentFolder, existingSolrFolderDocs)
                    // continue through tree, even if this folder needs no updating
                    fvr = FileVisitResult.CONTINUE
                }

            } else {
                fvr = FileVisitResult.SKIP_SUBTREE
                log.warn "Folder: $currentFolder is not accessible, skip subtree: $f"
            }
            return fvr
        }

        def doPostDir = {
            log.debug "\t\tPost dir: $it"
            boolean shouldUpdate = differenceChecker.shouldUpdate(differenceStatus)
            if (shouldUpdate) {
                log.debug "\t\tcrawlFolderFiles results:($folderResults)"
                def doAnalysisresults = analyzer.analyze(currentFolder)
                log.debug "\t\tdoAnalysisresults: $doAnalysisresults to currentFolder: $currentFolder"
            } else {
                log.info "\t\tdifferenceChecker says we can skip this ($currentFolder) folder, it is up to date"
            }

        }

        Map options = [type   : FileType.DIRECTORIES, preDir: doPreDir, postDir: doPostDir,
                       preRoot: true, postRoot: true, visitRoot: true,]

        long cnt = 0
        startDir.traverse(options) { File folder ->
            cnt++
            FSFolder childFolder = new FSFolder(folder, null, locationName, crawlName)
            boolean ignoreFolder = analyzer.shouldIgnore(childFolder)
            if (childFolder.ignore) {
                log.debug "---- $cnt) Ignoring child folder: $childFolder"
                results.skipped << childFolder
            } else {
                DifferenceStatus differenceStatus = differenceChecker.compareFSFolderToSavedDocMap(currentFolder, existingSolrFolderDocs)
//                DifferenceStatus differenceStatus = differenceChecker.compare(currentFolder, existingSolrFolderDocs)
                boolean shouldUpdate = differenceChecker.shouldUpdate(differenceStatus)
                if (shouldUpdate) {
                    log.debug "\t\tcrawlFolderFiles results:($folderResults)"
//                    List doAnalysisresults = analyzer.doAnalysis(currentFolder)
                    def doAnalysisresults = analyzer.analyze(currentFolder)
                    log.debug "\t\tdoAnalysisresults: $doAnalysisresults to currentFolder: $currentFolder"

                    List<SavableObject> savableObjects = currentFolder.gatherSavableObjects()
                    def response = persistenceClient.saveObjects(savableObjects)
                    log.info "\t\t$cnt) ++++Save folder (${currentFolder.path}:${currentFolder.depth}) -- Differences:${differenceStatus.differences} -- response: $response"
                    results.updated << currentFolder

                    def archiveFiles = currentFolder.children.each { FSObject fsObject ->
                        if (fsObject.archive) {
                            List<SavableObject> archEntries = ((FSFile) fsObject).gatherArchiveEntries()
                            def responseArch = persistenceClient.saveObjects(savableObjects)
                            log.debug "\t\tSolr response saving archive entries: $responseArch"
                        }
                    }
                    log.debug "\t\tArhive files in folder($currentFolder): $archiveFiles"

                } else {
                    results.current << currentFolder
                    log.debug "\t\t$cnt) no need to update: $differenceStatus -- persistence ad source are current"
                }
            }
        }
        return results
    }


    /**
     * Crawl the folder and use the analyzer to determin what folder contents (files, subdirs) to add as 'children'
     * @param fsFolder
     * @param analyzer
     * @return the folder's (newly added?) children
     */
    List<SavableObject> crawlFolderFiles(FSFolder fsFolder, BaseAnalyzer analyzer) {
        log.info "\t\tcall to crawlFolderFiles($fsFolder, ${analyzer.class.simpleName})..."
//        Map<String, Map<String, Object>> results = [:]
        int countAdded = 0
        if (fsFolder.children) {
            log.warn "FSFolder ($fsFolder) already has defined 'children': ${fsFolder.children}, this seems bad!!"
        } else {
            log.debug "\t\tinitialize empty children list for FSFolder ($fsFolder) -- as expected "
            fsFolder.children = []
        }
        if (fsFolder.size) {
            log.warn "FSFolder($fsFolder) size already set??? reseting to 0 as we crawlFolderFiles (with ignore patterns)..."
        }
        fsFolder.size = 0

        int cnt = 0
        if (fsFolder.thing instanceof File) {
            File folder = fsFolder.thing
            folder.eachFile { File f ->
                cnt++
                SavableObject child = null
                if (f.isDirectory()) {
                    child = new FSFolder(f, fsFolder, locationName, fsFolder.crawlName)
                } else {
                    child = new FSFile(f, fsFolder, locationName, fsFolder.crawlName)
                }

//                Map<String, Map<String, Object>> aResults = analyzer.analyze(child)
//                results.put(f, aResults)
                if (child.ignore) {
                    log.debug "\t\t\t\tignoring child: $child -- analyzer results:($aResults)"
                } else {
                    countAdded++
                    log.debug "\t\tAdding child:($child) to folder($fsFolder)"
                    fsFolder.children << child
                    if (child.type == FSFile.TYPE) {
                        fsFolder.size += child.size
                        // todo -- revisit this semi-hidden folder size counting, is there a better/more explicit way?
                    } else {
                        log.debug "\t\tskip incrementing parent folder size, becuase this ($child) is not a File (???)"
                    }
                }
            }
            def rc = fsFolder.buildDedupString()
            log.debug "\t\tBuilt fsFolder($fsFolder) dedup string(${fsFolder.dedup})"
        } else {
            log.warn "fsFolder.thing(${fsFolder.thing}) is not a file!! bug??"
        }
        return fsFolder.children
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
            log.info "\t\tNo previous/existing solr docs found: $sq"
        } else {
            log.debug "\t\tGet existing solr folder docs map, size: ${docs.size()} -- Filters: ${sq.getFilterQueries()}"
        }
        Map<String, SolrDocument> docsMap = docs.groupBy { it.getFirstValue('id') }
        return docsMap
    }


    @Override
    public String toString() {
        return "LocalFileSystemCrawler{" +
                "locationName='" + locationName + '\'' +
                ", osName='" + osName + '\'' +
                ", differenceChecker=" + differenceChecker +
                ", persistenceClient=" + persistenceClient +
                ", analyzer=" + analyzer +
                '}';
    }
}
