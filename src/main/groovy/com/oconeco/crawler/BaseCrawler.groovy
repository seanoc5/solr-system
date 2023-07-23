package com.oconeco.crawler

import com.oconeco.analysis.BaseAnalyzer
import com.oconeco.difference.BaseDifferenceChecker
import com.oconeco.difference.SolrDifferenceChecker
import com.oconeco.difference.BaseDifferenceStatus
import com.oconeco.helpers.ArchiveUtils
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
class BaseCrawler {
    static log = LogManager.getLogger(this.class.name);
    String locationName
    String osName
    SolrDifferenceChecker differenceChecker
    SolrSystemClient persistenceClient
//    BaseClient persistenceClient

    // todo -- fix these fied lists, put in constructor, or move to diff checker
    String checkFolderFields = SolrSystemClient.DEFAULT_FIELDS_TO_CHECK.join(' ')
    String checkFileFields = SolrSystemClient.DEFAULT_FIELDS_TO_CHECK.join(' ')

    BaseCrawler(String locationName, BaseClient persistenceClient, BaseDifferenceChecker diffChecker) {
        log.debug "${this.class.simpleName} constructor with location:$locationName, Client($persistenceClient) DiffChecker($differenceChecker)"
        this.locationName = locationName
        osName = System.getProperty("os.name")
        // moved this from config file to here in case we start supporting remote filesystems...? (or WSL...?)
        this.differenceChecker = diffChecker
        this.persistenceClient = persistenceClient
    }


    /**
     * util function to get a doc count, with logical filters to align with crawling locations with subsets (crawlNames)
     * this should ideally return count information, not real doc-content
     * @param crawlName - subset of larger locationName
     * @param q - optional query - defaults to all docs
     * @param filterQuery - if you need to filter the counts,
     * @return map of numFound (doc count), and helpful facet info (i.e. type count: file, folder, archFile, archFolder...)
     */
    Map<String, Object> getSolrDocCount(String crawlName = '', String q = '*:*', String filterQuery = '') {
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
                String dedupString = currentFolder.buildDedupString()       // create dedup AFTER we have collected relevant file sizes
                if (shouldIgnore) {
                    log.info "\t\t----IGNORABLE folder:($currentFolder) matchedLabels:($currentFolder.matchedLabels)"
                    fvr = FileVisitResult.SKIP_SUBTREE
                } else {
                    log.debug "\t\t----Not ignorable folder: $currentFolder"
                    // todo -- refactor, this is uber-hacky... for speed skip checking folder sizes, for completeness, crawl files and use the totalled size of non-ignored files
                    if(differenceChecker.checkGroupSizes) {
                        List<SavableObject> list = crawlFolderFiles(currentFolder, analyzer)
                        log.debug "\t\tFolder files crawled (WITH checking folder sizes) size: ${list.size()}"
                    }

                    //Note compareFSFolderToSavedDocMap will save diff status in object, for postDir assessment/analysis
                    BaseDifferenceStatus diffStatus = differenceChecker.compareCrawledGroupToSavedGroup(currentFolder, existingSolrFolderDocs)

                    // todo -- refactor, this is uber-hacky... for speed skip checking folder sizes, for completeness, crawl files and use the totalled size of non-ignored files
                    if(!differenceChecker.checkGroupSizes && diffStatus.significantlyDifferent) {
                        log.debug "\t\tcrawl folder files after comparing (without file sizes): $currentFolder"
                        List<SavableObject> list = crawlFolderFiles(currentFolder, analyzer)
                        log.debug "\t\tFolder files crawled (WITHOUT checking folder sizes) size: ${list.size()}"
                    }

                    fvr = FileVisitResult.CONTINUE // note continue through tree, separate analysis of updating done later
                }

            } else {
                fvr = FileVisitResult.SKIP_SUBTREE
                log.warn "Folder: $currentFolder is not accessible, skip subtree: $f"
            }
            return fvr
        }


        Map options = [type   : FileType.DIRECTORIES, preDir: doPreDir, //postDir: doPostDir,
                       preRoot: true, postRoot: true, visitRoot: true,]

        long cnt = 0
        startDir.traverse(options) { File folder ->
            cnt++
            log.info "$cnt) traversing file(folder?): $folder"
            if (folder.isDirectory()) {
                FSFolder childFolder = new FSFolder(folder, null, locationName, crawlName)
                boolean ignoreFolder = analyzer.shouldIgnore(childFolder)
                if (childFolder.ignore) {
                    log.debug "---- $cnt) Ignoring child folder: $childFolder"
                    results.skipped << childFolder
                } else {
                    BaseDifferenceStatus differenceStatus = differenceChecker.compareCrawledDocToSavedDoc(currentFolder, existingSolrFolderDocs)
                    boolean shouldUpdate = differenceChecker.shouldUpdate(differenceStatus)
                    if (shouldUpdate) {
                        List<SavableObject> savableObjects = currentFolder.gatherSavablechildItems()

                        def doAnalysisresults = analyzer.analyze(savableObjects)
                        log.debug "\t\tdoAnalysisresults: $doAnalysisresults to currentFolder: $currentFolder"
                        // --------------- SAVE FOLDER AND CHILDREM
                        def response = persistenceClient.saveObjects(savableObjects)
                        log.info "\t\t$cnt) ++++Save folder (${currentFolder.path}:${currentFolder.depth}) -- Differences:${differenceStatus.differences} -- response: $response"
                        results.updated << currentFolder

                        // ----------------------- PROCESS ARCHIVE FILES SEPARATE (MEMORY)---------------------
                        // todo -- revisit how to avoid oome, each archive file could be huge, on top of a given (current)folder that might be huge... process archivefiles after folder, and release folder memory...?
                        List<FSObject> archiveFiles = ArchiveUtils.gatherArchiveObjects(currentFolder.childItems)
                        // release folder/children memory, and move on to processing each archive (virtual folder)
                        currentFolder = null

                        archiveFiles.each { FSFile fSFile ->
                            if (fSFile.extension.equalsIgnoreCase('jar')) {
                                log.debug "\t\tskipping jar files for archive entry analysis: $fSFile"
                            } else {
                                //todo -- ignore jar files, as they are likely numerous, and we really don't care about their contents...? (for java developers especially)
                                List<SavableObject> archEntries = ((FSFile) fSFile).gatherArchiveEntries()
                                def responseArch = persistenceClient.saveObjects(archEntries)
                                log.debug "\t\t====Solr response saving archive entries: $responseArch --  -- from fsFile:($fSFile)"
                            }
                        }
                        log.debug "\t\tArhive files in folder($currentFolder): $archiveFiles"


                    } else {
                        results.current << currentFolder
                        log.info "\t\t$cnt) no need to update for currentFolder:($currentFolder) -- diffStatus:($differenceStatus) -- source current with saved version"
                    }
                }
            } else {
                log.warn "processing file???: $folder (should only be folders in this loop"
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
        log.debug "\t\t....call to crawlFolderFiles($fsFolder, ${analyzer.class.simpleName})..."
//        Map<String, Map<String, Object>> results = [:]
        int countAdded = 0
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

                if (child.ignore) {
                    log.debug "\t\t\t\tignoring child: $child -- analyzer results:($aResults)"
                } else {
                    countAdded++
                    log.debug "\t\tAdding child:($child) to folder($fsFolder)"
                    if (child.type == FSFile.TYPE) {
                        fsFolder.childItems << child
                        fsFolder.size += child.size
                        // todo -- revisit this semi-hidden folder size counting, is there a better/more explicit way?
                    } else {
                        fsFolder.childGroups << child
                        log.debug "\t\tskip incrementing parent folder size, becuase this ($child) is not a File (Should be folder???)"
                    }
                }
            }
            if(fsFolder.size > 0){
                log.debug "\t\tfolder:($fsFolder) size: ${fsFolder.size}"
            } else {
                log.info "\t\tfolder:($fsFolder) size: ${fsFolder.size}"
            }
            def rc = fsFolder.buildDedupString()
            log.debug "\t\tBuilt fsFolder($fsFolder) dedup string(${fsFolder.dedup})"
        } else {
            log.warn "fsFolder.thing(${fsFolder.thing}) is not a file!! bug??"
        }
        // todo -- fixme - quick hack while separating child items and groups
        return fsFolder.childGroups.addAll(fsFolder.childItems)
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
