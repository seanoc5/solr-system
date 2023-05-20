package com.oconeco.crawler


import com.oconeco.helpers.Constants
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
//    String name = 'n.a.'
//    String location = 'none'
//    int statusDepth = 3

//    LocalFileSystemCrawler(String name, String location, int statusDepth = 3) {
////        super(name, location, statusDepth)
//        log.info "${this.class.name} constructor with name: $name, location:$location, statusDepth:$statusDepth..."
//        this.name = name
//        this.location = location
//        this.statusDepth = statusDepth
//    }

    static def startCrawl(def source, Map namePatternsFolder = Constants.DEFAULT_FOLDERNAME_PATTERNS) {
        log.info "Start crawl with source path:($source) and name patterns map: $namePatternsFolder"
        File startFolder = getStartDirectory(source, namePatternsFolder)
        def results = visitFolders(startFolder, namePatternsFolder)
        return results
    }


    /**
     * helper method to accept various inputs,and return the filesystem object (File/dir) to actuallly crawl
     * @param source
     * @param namePatterns
     * @return
     */
    static File getStartDirectory(def source, Map<String, Pattern> namePatterns) {
        File startDir = null
        if (source instanceof Path) {
            log.info "got a proper path (${source.class.name}): $source"
            startDir = source.toFile()
        } else if (source instanceof File) {
            startDir = source
        } else if (source instanceof String) {
            startDir = new File(source)
        } else {
            log.warn "Unknown source type (${source.class.name}, trying a blind cast..."
            startDir = ((Path) source)
        }
        return startDir
    }


    /**
     * user filevisitor pattern to get all the folders which are not explicitly ignored
     * @param startDir
     * @param namePatterns
     * @return map of ignored and not-ignored folders
     *
     * this is a quick/light scan of folders, assumes following processing will order crawl of folders by priority,
     * and even check for updates to skip folders with no apparent changes
     */
    static def visitFolders(File startDir, Map<String, Pattern> namePatterns) {
        List<Path> foldersToCrawl = []
        List<Path> ignoredFolders = []
        Pattern ignorePattern = namePatterns.get(Constants.LBL_IGNORE)

        Map options = [type     : FileType.DIRECTORIES,
                       preDir   : {
                           if (it ==~ ignorePattern) {
                               ignoredFolders << it
                               log.info "\t\t\tINGOREing folder: $it -- matches ignorePattern: $ignorePattern"
                               return FileVisitResult.SKIP_SUBTREE

                           } else {
                               foldersToCrawl << it
                               log.info "\t\tADDING Crawl folder: $it (${it.class.name})"
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

        log.debug "Starting crawl of folder: ($startDir)  ..."

        startDir.traverse(options) { def folder ->
            log.debug "\t\ttraverse folder $folder"
        }
        Map results = [:]
        results.put(Constants.LBL_CRAWL, foldersToCrawl)
        results.put(Constants.LBL_IGNORE, ignoredFolders)

        return results
    }
//    public static final String FOLDERS_TO_CRAWL = 'crawl'       // todo -- merge these with other similar constants or strings
//    public static final String FOLDERS_TO_IGNORE = 'ignore'
}
