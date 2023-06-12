package com.oconeco.crawler

import com.oconeco.models.FSFile
import com.oconeco.models.FSFolder
import groovy.io.FileType
import groovy.io.FileVisitResult
import org.apache.log4j.Logger
import org.apache.solr.common.SolrDocument

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
    String crawlName        // todo -- remove this from class, leave as arg in methods wher appropriate
    String locationName
    String osName
    int statusDepth = 3
    DifferenceChecker differenceChecker


    LocalFileSystemCrawler(String locationName, String crawlName, int statusDepth = 3, DifferenceChecker diffChecker = new DifferenceChecker()) {
        log.debug "${this.class.simpleName} constructor with location:$locationName, name: $crawlName,  statusDepth:$statusDepth..."
        this.crawlName = crawlName
        this.locationName = locationName
        osName = System.getProperty("os.name")
        // moved this from config file to here in case we start supporting remote filesystems...? (or WSL...?)
        this.statusDepth = statusDepth
        this.differenceChecker = diffChecker
    }


    List<FSFolder> buildCrawlFolders(String crawlName, def source, Pattern ignorePattern, Map<String, SolrDocument> existingSolrFolderDocs) {
        // todo -- fix depth tracking, currently everything is '1'
        log.debug "\t\t....buildCrawlFolders ($crawlName) with source path:($source) and name ignore pattern: $ignorePattern -- existing folder doc count: ${existingSolrFolderDocs?.size()} (for checking if solr is in sync/updated)"

        // todo -- fix me -- possibly find a more elegant approach to the crawlName processing/remembering
        this.crawlName = crawlName

        File startFolder = getStartDirectory(source)
        List<FSFolder> results = visitFolders(crawlName, startFolder, ignorePattern, existingSolrFolderDocs)
        return results
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


    /**
     * user filevisitor pattern to get all the folders which are not explicitly ignored
     * @param startFolder
     * @param namePatterns
     * @return map of ignored and not-ignored folders
     *
     * this is a quick/light scan of folders, assumes following processing will order crawl of folders by priority,
     * and even check for updates to skip folders with no apparent changes
     */
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
                           File f = (File) it
                           boolean accessible = f.exists() && f.canRead() && f.canExecute()

                           Path reletivePath = startPath.relativize(f.toPath())
                           String relPath = reletivePath.toString()
                           int depth = relPath > '' ? reletivePath.getNameCount() : 0
                           FSFolder fsFolder = new FSFolder(f, this.locationName, this.crawlName, depth)

                           if (accessible) {
                               if (it.name ==~ ignorePattern) {
                                   fsFolder.ignore = true
                                   // todo -- add should update status for ignored folders??
                                   log.info "\t\t----Ignorable folder, AND does not need updating: $fsFolder"
                                   return FileVisitResult.SKIP_SUBTREE

                               } else {
                                   boolean shouldUpdate = true
                                   // default to true, make check unset if all conditions are met
                                   DifferenceStatus status
                                   SolrDocument solrDocument
                                   // solr doc to compare "saved" info and freshness with this file object

                                   if (existingSolrFolderDocs) {
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
                                   }

                                   if (shouldUpdate) {
//                                       fsFolder.ignore = false
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
                       },
/*
                       postDir  : {
                           println("Post dir: $it")
                           totalSize = 0
                       },
*/
                       preRoot  : true,
                       postRoot : true,
                       visitRoot: true,
        ]

        log.debug "Starting crawl of folder: ($startFolder)  ..."

        startFolder.traverse(options) { def folder ->
            log.debug "\t\ttraverse folder $folder"     // should there be action here?
        }

        return foldersMap?.values()?.toList()
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
     * consider moving this to analyzer code
     * @param folder
     * @param savedFolderDoc
     * @return
     */
//    boolean folderHasUpdates(FSFolder folder, SolrDocument savedFolderDoc){
//        log.info "Folder to consider: $folder -- Saved info: $savedFolderDoc"
//        return true
//    }
    @Override
    public String toString() {
        return "LocalFileSystemCrawler{" + "crawlName='" + crawlName + '\'' + ", locationName='" + locationName + '\'' + '}';
    }
}
