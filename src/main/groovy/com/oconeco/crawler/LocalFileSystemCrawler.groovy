package com.oconeco.crawler

import com.oconeco.analysis.FolderAnalyzer
import groovy.io.FileType
import groovy.io.FileVisitResult
import org.apache.log4j.Logger

import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   10/15/22, Saturday
 * @description: class to crawl local filesystems
 */

class LocalFileSystemCrawler extends BaseCrawler {
    Logger log = Logger.getLogger(this.class.name);

    LocalFileSystemCrawler(String name, String location, int statusDepth=3) {
        super(name, location, statusDepth)
        log.info "${this.class.name} constructor with name: $name, location:$location, statusDepth:$statusDepth..."
    }

    @Override
    def startCrawl(def source, Map namePatterns) {
        log.info "Start crawl with source path:($source) and name patterns map: $namePatterns"
        Path startFolder = getStartFolder(source, namePatterns)
        def results = visitFolders(startFolder, namePatterns)
        return results
    }

    Path getStartFolder(def source, Map<String, Pattern> namePatterns) {
        Path startPath = null
        if (source instanceof Path) {
            log.info "got a proper path (${source.class.name}"
            startPath = source
        } else if (source instanceof File) {
            startPath = (File) source.toPath()
        } else if (source instanceof String) {
            startPath = Paths.get(source)
        } else {
            log.warn "Unknown source type (${source.class.name}, trying a blind cast..."
            startPath = ((Path) source)
        }
        return startPath
    }

    def visitFolders(Path startPath, Map<String, Pattern> namePatterns) {
        List<Path> foldersToCrawl = []
        List<Path> ignoredFolders = []
        Pattern ignorePattern = namePatterns.get(FolderAnalyzer.LBL_IGNORE)
        Map options = [type     : FileType.DIRECTORIES,
                       preDir   : {
//                           allFileNames.addAll(it.list())
                           if (it ==~ ignorePattern) {
                               ignoredFolders << it
                               log.info "INGOREing folder: $it -- matches ignorePattern: $ignorePattern"
//                               log.info "INGOREing folder: $it -- matches ignorePattern: $ignorePattern -- files:$ignoreFiles"
                               return FileVisitResult.SKIP_SUBTREE

                           } else {
                               foldersToCrawl << it
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

        log.info "Starting crawl of folder: ($startPath)  ..."
        startPath.traverse(options) { def folder ->
            log.info "\t\ttraverse folder $foldersToCrawl"
        }
        Map resuts = [foldersToCrawl:foldersToCrawl, ignoredFolders:ignoredFolders]
        return resuts
    }
}
