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
class LocalFileSystemCrawler {
    static log = LogManager.getLogger(this.class.name);
    String locationName
    String osName
    SolrDifferenceChecker differenceChecker
    SolrSystemClient persistenceClient
//    BaseClient persistenceClient

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
        Map<File, FSObject> folderMap = [:]
        FSFolder currentFolder = null
        int cnt = 0

        def doPreDir = {
            cnt++
            File f = it
            log.info "\t====PRE dir:  $it"
            FileVisitResult fvr = FileVisitResult.CONTINUE      // set default to continue/process
            boolean accessible = f.exists() && f.canRead() && f.canExecute()

            File parentFile = f.getParentFile()
            FSFolder parentFolder = folderMap.get(parentFile)
            currentFolder = new FSFolder(f, parentFolder, this.locationName, crawlName)
            def foo1 = folderMap.put(f, currentFolder)
            int depth = getRelativeDepth(startPath, f)
            currentFolder.depth = depth
            boolean shouldIgnore = analyzer.shouldIgnore(currentFolder)

            if (accessible) {
                if (shouldIgnore) {
                    log.info "\t\t----IGNORABLE folder:($currentFolder) matchedLabels:($currentFolder.matchedLabels)"
                    fvr = FileVisitResult.SKIP_SUBTREE
                    results.ignored << currentFolder
                } else {
                    log.debug "\t\t----Not ignorable folder: $currentFolder"
                    fvr = FileVisitResult.CONTINUE

                    // todo -- refactor, this is uber-hacky... for speed skip checking folder sizes, for completeness, crawl files and use the totalled size of non-ignored files
                    if (differenceChecker.checkGroupSizes) {
                        def rc = crawlFolderChildren(currentFolder, analyzer)
                    }

                    //Note compareFSFolderToSavedDocMap will save diff status in object, for postDir assessment/analysis
                    Iterable savedGroup = findMatchingSavedGroup(existingSolrFolderDocs, currentFolder)
                    BaseDifferenceStatus diffStatus = differenceChecker.compareCrawledGroupToSavedGroup(currentFolder, savedGroup)
                    boolean shouldUpdate = differenceChecker.shouldUpdate(diffStatus)

                    // note continue through tree, separate analysis of updating done later
                    if (shouldUpdate) {
                        log.info "\t\t----SHOULD UPDATE folder ($currentFolder) --differences:(${diffStatus.differences}) "

                        if(!differenceChecker.checkGroupSizes) {
                            // todo -- refactor, this is uber-hacky... for speed skip checking folder sizes, for completeness, crawl files and use the totalled size of non-ignored files
                            List<SavableObject> crawlResults = crawlFolderChildren(currentFolder, analyzer)
                            log.debug "\t\tcrawl folder files (count: ${crawlResults.size()}) after comparing (without file sizes): $currentFolder"
                        }

                        def folderAnalysis = analyzer.analyze(currentFolder)
                        List<SavableObject> analyzableChildren = currentFolder.gatherAnalyzableChildren()
                        def doAnalysisresults = analyzer.analyze(analyzableChildren)
                        log.debug "\t\tdoAnalysisresults: $doAnalysisresults to currentFolder: $currentFolder"

                        // --------------- SAVE FOLDER AND CHILDREM
                        def folderAndChildren = analyzableChildren + currentFolder  //.add(currentFolder)
                        def response = persistenceClient.saveObjects(folderAndChildren)
                        log.info "\t\t$cnt) ++++Save folder (${currentFolder.path}--depth:${currentFolder.depth}) -- Children count:(item:${currentFolder.childItems.size()} groups:${currentFolder.childGroups.size()}) -- Differences:${diffStatus.differences} -- response: $response"
                        results.updated << currentFolder

                        // ----------------------- PROCESS ARCHIVE FILES SEPARATE (MEMORY)---------------------
                        // todo -- revisit how to avoid oome, each archive file could be huge, on top of a given (current)folder that might be huge... process archivefiles after folder, and release folder memory...?
                        List<FSObject> archiveFiles = ArchiveUtils.gatherArchiveObjects(currentFolder.childItems)

                        if (archiveFiles?.size() > 0) {
                            log.info "\t....process archive files (count:${archiveFiles.size()}) for folder ($currentFolder)...."
                        } else {
                            log.debug "\t\t no archive files for current folder: $currentFolder"
                        }

                        // release folder/children memory, and move on to processing each archive (virtual folder)
                        currentFolder = null

                        archiveFiles.each { FSFile fSFile ->
                            List<SavableObject> archEntries = ((FSFile) fSFile).gatherArchiveEntries()
                            def responseArch = persistenceClient.saveObjects(archEntries)
                            log.info "\t\t====Solr response saving archive entries: $responseArch (arch entries count: ${archEntries?.size()}) -- from fsFile:($fSFile)"
                        }
                        log.debug "\t\tArhive files in folder($currentFolder): $archiveFiles"

                    } else {
                        log.info "\t\t\t____determined we do NOT NEED TO UPDATE: $currentFolder -- diffStatus:($diffStatus)"
                        results.current << currentFolder
                        //            log.info "\t\t$cnt) no need to update for currentFolder:($currentFolder) -- diffStatus:($differenceStatus) -- source current with saved version"
                    }

                }

            } else {
                fvr = FileVisitResult.SKIP_SUBTREE
                log.warn "Folder: $currentFolder is not accessible, skip subtree: $f"
            }
            return fvr
        }

//        def doPostDir = {
//
//        }


        Map options = [type   : FileType.DIRECTORIES, preDir: doPreDir, // postDir: doPostDir,
                       preRoot: true, postRoot: true, visitRoot: true,]

        // todo -- revisit? is there anything that should happen here, rather than doPreDir??
        startDir.traverse(options) { File folder ->
            log.debug "....traverse thing(folder?): ($folder)"
            if (folder.isDirectory()) {
                log.debug "====traversing folder: $folder"
            } else {
                log.warn "processing file???: $folder (should only be folders in this loop"
            }
        }
        return results
    }


    Map findMatchingSavedGroup(Map<String, SolrDocument> existingSolrFolderDocs, FSFolder currentFolder) {
        SolrDocument savedGroup = existingSolrFolderDocs?.get(currentFolder.id)
        return savedGroup
    }


    /**
     * Crawl the folder and use the analyzer to determin what folder contents (files, subdirs) to add as 'children'
     * @param fsFolder
     * @param analyzer
     * @return the folder's (newly added?) children
     */
    List<SavableObject> crawlFolderChildren(FSFolder fsFolder, BaseAnalyzer analyzer) {
        log.info "\t\t....call to crawlFolderFiles($fsFolder, ${analyzer.class.simpleName})..."
//        Map<String, Map<String, Object>> results = [:]
        List<FSObject> results = []
        if (fsFolder.size) {
            log.warn "FSFolder($fsFolder) size already set??? reseting to 0 as we crawlFolderFiles (with ignore patterns)..."
        }
        fsFolder.size = 0

        int cnt = 0
        if (fsFolder.thing instanceof File) {
            File folder = fsFolder.thing
            if(folder.isDirectory()) {
                folder.eachFile { File f ->
                    cnt++
                    SavableObject child = null
                    if (f.isDirectory()) {
                        child = new FSFolder(f, fsFolder, locationName, fsFolder.crawlName, analyzer.ignoreGroup)
                    } else {
                        child = new FSFile(f, fsFolder, locationName, fsFolder.crawlName, analyzer.ignoreItem)
                    }

                    // include ignored items in return value, but not in child properties
                    results << child

                    if (child.ignore) {
                        log.debug "\t\t\t\t$cnt) ignoring child: $child "
                    } else {
                        log.debug "\t\tAdding child:($child) to folder($fsFolder)"
                        if (child.type == FSFile.TYPE) {
                            fsFolder.childItems << child
                            // update folder 'size' skipping ignored files (patterns)
                            fsFolder.size += child.size
                            // todo -- revisit this semi-hidden folder size counting, is there a better/more explicit way?
                        } else {
                            fsFolder.childGroups << child
                            log.debug "\t\t$cnt) skip incrementing parent folder size, becuase this ($child) is not a File (Should be folder???)"
                        }
                    }
                }
                if (fsFolder.size > 0) {
                    log.debug "\t\t$cnt) folder:($fsFolder) size: ${fsFolder.size}"
                } else {
                    log.info "\t\t$cnt) folder:($fsFolder) size: ${fsFolder.size}"
                }
                def rc = fsFolder.buildDedupString()            // todo -- should this be "hidden" here???
                log.debug "\t\t$cnt) Built fsFolder($fsFolder) dedup string(${fsFolder.dedup})"

//                results = [groups:fsFolder.childGroups, items:fsFolder.childItems]

            } else {
                log.warn "$cnt) crawlFolderFiles($fsFolder) is not a directory somehow....?"
            }
        } else {
            log.warn "$cnt) fsFolder.thing(${fsFolder.thing}) is not a file(much less a directory)!! bug??"
        }
        // todo -- fixme - quick hack while separating child items and groups
        return results
    }


    /**
     * helper func to get relative depth (from crawl's start folder)
     * @param startPath - original start path
     * @param f current file,
     * @return count of levels between startpath and current path
     */
    public int getRelativeDepth(Path startPath, File f) {
        int d = 0
        Path reletivePath = startPath.relativize(f.toPath())
        String relPath = reletivePath.toString()
        if (relPath > '') {
            d = reletivePath.getNameCount()
        }
        return d
    }


    /**
     * get the saved (persisted) group (i.e. folder) documents for a given crawl (i.e. location name and crawl name)
     * @param crawlName
     * @param q
     * @param fq
     * @param fl
     * @return
     */
    Map<String, SolrDocument> getSavedGroupDocs(String crawlName, String fq = "type_s:${FSFolder.TYPE}", String fl = this.checkFolderFields, int maxRowsReturned = SolrSystemClient.MAX_ROWS_RETURNED) {
        SolrQuery sq = new SolrQuery('*:*')
        sq.setRows(maxRowsReturned)
        sq.setFilterQueries(fq)
        // add filter queries to further limit
        // NOTE Crawler objects have an intrinsic connection to a specific crawl "location" (i.e. machine/filesystem, or browser/email account,...)
        String filterLocation = SolrSystemClient.FLD_LOCATION_NAME + ':' + locationName
        sq.addFilterQuery(filterLocation)
        // the crawlName is a subset of the larger location, this is dynamic where location is fixed (for this crawler)
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
