package com.oconeco.crawler

import com.oconeco.models.FSFile
import com.oconeco.models.FSFolder
import com.oconeco.models.SavableObject
import com.oconeco.persistence.SolrSystemClient
import groovy.io.FileType
import groovy.io.FileVisitResult
import org.apache.log4j.Logger
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList

import java.nio.file.Path
import java.util.regex.Pattern
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
//    int statusDepth = 3
    DifferenceChecker differenceChecker
    SolrSystemClient solrSystemClient
    String checkFolderFields = SolrSystemClient.DEFAULT_FIELDS_TO_CHECK.join(' ')
    String checkFileFields = SolrSystemClient.DEFAULT_FIELDS_TO_CHECK.join(' ')


    LocalFileSystemCrawler(String locationName, String crawlName, SolrSystemClient solrSystemClient, DifferenceChecker diffChecker = new DifferenceChecker()) {
        log.debug "${this.class.simpleName} constructor with location:$locationName, name: $crawlName"
        this.crawlName = crawlName
        this.locationName = locationName
        osName = System.getProperty("os.name")
        // moved this from config file to here in case we start supporting remote filesystems...? (or WSL...?)
        this.differenceChecker = diffChecker
        this.solrSystemClient = solrSystemClient
    }


    /**
     * potentially useful approach of getting all crawlable folders before doing any real processing <br>
     * NOTE: consider moving to actual 'crawlFolders'
     * @param crawlName
     * @param source
     * @param ignorePattern
     * @param existingSolrFolderDocs
     * @return
     */
/*
    List<FSFolder> buildCrawlFolders(String crawlName, def source, Pattern ignorePattern, Map<String, SolrDocument> existingSolrFolderDocs) {
        log.debug "\t\t....buildCrawlFolders ($crawlName) with source path:($source) and name ignore pattern: $ignorePattern -- existing folder doc count: ${existingSolrFolderDocs?.size()} (for checking if solr is in sync/updated)"

        // todo -- fix me -- possibly find a more elegant approach to the crawlName processing/remembering
        this.crawlName = crawlName

        File startFolder = getStartDirectory(source)
        List<FSFolder> results = visitFolders(crawlName, startFolder, ignorePattern, existingSolrFolderDocs)
        return results
    }
*/


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
        QueryResponse response = solrSystemClient.query(solrQuery)
        long numFound = response.results.getNumFound()
        log.debug "\t\tLocalFSCrawler getSolrDocCount: $numFound -- $this"
        return numFound
    }


    /**
     * Basic crawl functionality here, with visitor pattern
     * @param crawlName (subset of locationName/source)
     * @param startFolder folder to start crawl
     * @param ignoreFolders regex to indicate we should save the current folder, mark as ignore, and not descend any further
     * @param existingSolrFolderDocs - collection of pre-existing solr docs, for comparison of 'freshness' (need update?)
     * @return map of status counts
     */
   Map<String, List<FSFolder>> crawlFolders(String crawlName, File startFolder, Pattern ignoreFolders, Pattern ignoreFiles) {
       Map<String, List<FSFolder>> results = [:].withDefault {[]}
        Map<String, SolrDocument> existingSolrFolderDocs = getSolrFolderDocs(crawlName)

        log.debug "Starting crawl of folder: ($startFolder)  ..."
        Path startPath = startFolder.toPath()   // Path so we can 'relativize` to get relative depth of this crawl
        FSFolder currentFolder = null

        def doPreDir = {
            File f = it
            log.info "\t\t====PRE dir:  $it"
            FileVisitResult fvr = FileVisitResult.CONTINUE
            boolean accessible = f.exists() && f.canRead() && f.canExecute()

            int depth = getRelativeDepth(startPath, f)
            currentFolder = new FSFolder(f, this.locationName, this.crawlName, ignoreFolders, ignoreFiles, depth)
//            results << currentFolder

            if (accessible) {
                if (it.name ==~ ignoreFolders) {
                    currentFolder.ignore = true
                    log.debug "\t\t----Ignorable folder, AND does not need updating: $currentFolder"
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

        def doPostDir = {log.info "\t\tPost dir: $it" }

        Map options = [
                type     : FileType.DIRECTORIES,
                preDir   : doPreDir,
                postDir  : doPostDir,
                preRoot  : true,
                postRoot : true,
                visitRoot: true,
        ]

       long cnt = 0
       int cntStatusFrequency = 20 // show log message every cntStatusFrequency folders
        startFolder.traverse(options) { File folder ->
            cnt++
            if(cnt % cntStatusFrequency==0){
                log.info "\t\t\t$cnt) traverse folder $folder"     // should there be action here?
            } else {
                log.debug "\t\t$cnt) traverse folder $folder"     // should there be action here?
            }
            DifferenceStatus differenceStatus = differenceChecker.compareFSFolderToSavedDocMap(currentFolder, existingSolrFolderDocs)
            boolean shouldUpdate = differenceChecker.shouldUpdate(differenceStatus)
            if (shouldUpdate) {
//                def savedFiles =
                List<SavableObject> folderObjects = currentFolder.buildChildrenList(ignoreFiles)
                log.debug "Update FSFolder:($currentFolder) -- Objects count: ${folderObjects.size()} -- diffStatus: $differenceStatus"
                folderObjects.add(currentFolder)
                def response = solrSystemClient.saveObjects(folderObjects)
                log.info "\t\t++++Save folder (${currentFolder.toString()}) response: $response -- diffStatus:$differenceStatus"
                results.updated << currentFolder
            } else {
                results.skipped << currentFolder
                log.debug "\t\tno need to update: $differenceStatus"
            }
        }

        return results
    }


/**
 * user filevisitor pattern to get all the folders which are not explicitly ignored
 * @param startFolder
 * @param namePatterns
 * @return map of ignored and not-ignored folders
 *
 * this is a quick/light scan of folders, assumes following processing will order crawl of folders by priority,
 * and even check for updates to skip folders with no apparent changes
 * @deprecated - consider just using crawlFolders(....)
 */
/*
    List<FSFolder> visitFolders(String crawlName, File startFolder, Pattern ignorePattern, Map<String, SolrDocument> existingSolrFolderDocs) {
        HashMap<String, FSFolder> foldersMap = [:]

        // track the starting Path so we can 'relativize` and get relative depth of this crawl
        Path startPath = startFolder.toPath()

        // visit folders - build FSObject
        // check for existing solr doc
        // if present && in sync: skip update
        // else save new or updated
        Map options = [type     : FileType.DIRECTORIES,
                       preDir   : {
                           return doPreDirectory(startPath, crawlName, ignorePattern, existingSolrFolderDocs, foldersMap)
                       },
*/
/*
                       postDir  : {
                           println("Post dir: $it")
                           totalSize = 0
                       },
*//*

                       preRoot  : true,
                       postRoot : true,
                       visitRoot: true,
        ]

        log.debug "Starting crawl of folder: ($startFolder)  ..."

        startFolder.traverse(options) { File folder ->
            log.debug "\t\ttraverse folder $folder"     // should there be action here?
        }

        return foldersMap?.values()?.toList()
    }
*/


// todo -- remove or refactor,... this is a bad approach...?
/*
    def doPreDirectory = { Path startPath, String crawlName, Pattern ignorePattern, Map<String, SolrDocument> existingSolrFolderDocs, LinkedHashMap<String, FSFolder> foldersMap ->
        File f = (File) it
        boolean accessible = f.exists() && f.canRead() && f.canExecute()

        int depth = getRelativeDepth(startPath, f)
        FSFolder fsFolder = new FSFolder(f, this.locationName, this.crawlName, ignoreFolders, ignoreFiles, depth)

        if (accessible) {
            if (it.name ==~ ignorePattern) {
                fsFolder.ignore = true
                log.debug "\t\t----Ignorable folder, AND does not need updating: $fsFolder"
                // todo -- add should update status for ignored folders??
                return FileVisitResult.SKIP_SUBTREE

            } else {
                boolean shouldUpdate = true         // default to true, make check unset if all conditions are met
                DifferenceStatus status
                SolrDocument solrDocument
                // solr doc to compare "saved" info and freshness with this file object

                if (existingSolrFolderDocs) {
                    shouldUpdate = checkShouldUpdate(existingSolrFolderDocs, fsFolder)
                }

                if (shouldUpdate) {                 //fsFolder.ignore = false
                    log.info "\t\t++++ ADDING Crawl folder: $fsFolder"
                    foldersMap.put(f.path, fsFolder)
                } else {
                    log.info "\t\t===='${fsFolder.name}' is current (path:'${fsFolder.path}') no update needed"
                }
            }
        } else {
            log.warn "Got a bad/inaccessible folder? $it"
            return FileVisitResult.SKIP_SUBTREE
        }
    }
*/


/**
 * helper function to check if a given FSFolder is current with existing/saved Solr doc
 * @param existingSolrFolderDocs
 * @param fsFolder
 * @return true of the solr index needs to be updated from the filesystem
 */
    public boolean checkShouldUpdate(Map<String, SolrDocument> existingSolrFolderDocs, FSFolder fsFolder) {
        boolean shouldUpdate = true
        if (existingSolrFolderDocs?.size() > 0) {
            DifferenceStatus status
            SolrDocument solrDocument
            solrDocument = existingSolrFolderDocs.get(fsFolder.id)
            if (solrDocument) {
                // found an existing 'saved' record, check if it looks like we need to update or not
                status = differenceChecker.compareFSFolderToSavedDoc(fsFolder, solrDocument)
                shouldUpdate = differenceChecker.shouldUpdate(status)
                if (shouldUpdate) {
                    log.info "\t\tFolder($fsFolder) needs update: $shouldUpdate"
                } else {
                    log.debug "\t\tFolder($fsFolder) DO NOT need update: $shouldUpdate"
                }
            } else {
                log.info "\t\tNo matching solr doc, defaulting to FSFolder ($fsFolder) DOES NEED to be processed"
            }
        } else {
//            log.debug "no existingSolrFolderDocs to check against, so everything gets updated"
        }
        return shouldUpdate
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
 * Get all the non-ignore files in a folder
 * param folder - folder to get files from
 * param ignoredFolders - ignore pattern
 * return list of files
 */
    List<FSFile> populateFolderFsFiles(FSFolder fSFolder, Pattern ignoreFilesPattern) {
        List<FSFile> fSFilesList = []
        File folder = fSFolder.thing
        if (folder?.exists()) {
            if (folder.canRead()) {
                folder.eachFile(FileType.FILES) { File file ->
                    FSFile fsFile = new FSFile(file, fSFolder, locationName, crawlName)
                    def ignore = fsFile.matchIgnorePattern(ignoreFilesPattern)
                    if (ignore) {
                        log.debug "\t\tIGNORING file: $file"
                        fsFile.ignore = true
                    } else {
                        log.debug "\t\tNOT ignoring file: $file"
//                        fsFile.ignore= false //redundant??
                    }
                    fSFilesList << fsFile
                }
            } else {
                log.warn "\t\tCannot read folder: ${fSFolder.absolutePath}"
            }
        } else {
            log.warn "\t\tFolder (${fSFolder.absolutePath}) does not exist!!"
        }
        if (fSFolder.children) {
            log.warn "FSFolder ($fSFolder) already has 'children' list initiated (is this bad code??): $fSFolder.children"
        } else {
            log.debug "\t\tinitiating fsFolder.children with collected filesList (size:${fSFilesList.size()}"
        }
        fSFolder.children = fSFilesList
        return fSFilesList
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

        QueryResponse response = solrSystemClient.query(sq)
        SolrDocumentList docs = response.results
        if (docs.size() == maxRowsReturned) {
            log.warn "getExistingFolders returned lots of rows (${docs.size()}) which equals our upper limit: ${maxRowsReturned}, this is almost certainly a problem.... ${sq}}"
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
