package com.oconeco.crawler


import com.oconeco.helpers.Constants
import com.oconeco.models.FSFolder
import groovy.io.FileType
import groovy.io.FileVisitResult
import org.apache.log4j.Logger

import java.nio.file.Path
import java.util.regex.Pattern

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   10/15/22, Saturday
 * @description: class to crawl local filesystems
 */

class LocalFileSystemCrawler {
    static log = Logger.getLogger(this.class.name);
    String crawlName = 'n.a.'
    String locationName = 'n.a.'
    int statusDepth = 3

    LocalFileSystemCrawler(String locationName, String crawlName, int statusDepth = 3) {
//        super(name, location, statusDepth)
        log.info "${this.class.name} constructor with name: $crawlName, location:$locationName, statusDepth:$statusDepth..."
        this.crawlName = crawlName
        this.locationName = locationName
        this.statusDepth = statusDepth
    }

    Map<String, List<FSFolder>> buildCrawlFolders(def source, Map namePatternsFolder = Constants.DEFAULT_FOLDERNAME_PATTERNS) {
        log.info "Start crawl with source path:($source) and name patterns map: $namePatternsFolder"
        File startFolder = getStartDirectory(source)
        Pattern ignorePattern = namePatternsFolder[Constants.LBL_IGNORE]
        Map<String, List<FSFolder>> results = visitFolders(startFolder, ignorePattern)
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
    Map<String,List<FSFolder>> visitFolders(File startFolder, Pattern ignorePattern) {
        List<FSFolder> foldersToCrawl = []
        List<FSFolder> ignoredFolders = []
        Path startPath = startFolder.toPath()
//        Pattern ignorePattern = namePatterns.get(Constants.LBL_IGNORE)

        Map options = [type     : FileType.DIRECTORIES,
                       preDir   : {
                           File f = (File)it
                           Path thisPath = f.toPath()
                           Path reletivePath = startPath.relativize(thisPath)
//                           List parts = reletivePath.
                           int pathElements = reletivePath.getNameCount();
                           FSFolder fsFolder = new FSFolder(it, this.locationName, this.crawlName, pathElements)
                           if (it ==~ ignorePattern) {
                               ignoredFolders << fsFolder
                               log.info "\t\t\tINGOREing folder: $it -- matches ignorePattern: $ignorePattern"
                               return FileVisitResult.SKIP_SUBTREE

                           } else {
                               foldersToCrawl << fsFolder
                               log.info "\t\tADDING Crawl folder: $it"
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
            log.debug "\t\ttraverse folder $folder"
        }
        Map<String,List<FSFolder>> results = [:]           // note: is it possible to initialize with keys that are variables??
        results.put(Constants.LBL_CRAWL, foldersToCrawl)
        results.put(Constants.LBL_IGNORE, ignoredFolders)

        return results
    }


    /**
     * Get all the non-ignore files in a folder
     * param folder - folder to get files from
     * param ignoredFolders - ignore pattern
     * return list of files
     */
    def getFolderFiles(File folder, Pattern ignoreFiles){
        List<File> filesList = []
        if(folder?.exists()){
            if(folder.canRead()) {
                folder.eachFile(FileType.FILES) { File file ->
                    if (shouldIgnore(file, ignoreFiles)) {
                        log.debug "Ignoring file: $file"
                    } else {
                        filesList << file
                    }
                }
            } else {
                log.warn "\t\tCannot read folder: ${folder.absolutePath}"
            }
        } else {
            log.warn "\t\tFolder (${folder.absolutePath}) does not exist!!"
        }
        return filesList
    }
}
