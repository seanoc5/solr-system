package com.oconeco.crawler

import com.oconeco.analysis.FolderAnalyzer
import groovy.io.FileType
import groovy.io.FileVisitResult
import spock.lang.Specification


import java.util.regex.Pattern

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/28/22, Sunday
 * @description:
 */

class LocalFileCrawlerTest extends Specification {
    File testFolder = new File(getClass().getResource('/content').toURI())
    Pattern ignoreFolders = ~/ignore.*/


    // http://docs.groovy-lang.org/1.8.6/html/groovy-jdk/java/io/File.html#traverse(java.util.Map,%20groovy.lang.Closure)
    def "check groovy File recursion"() {
        given:
        Integer totalSize = 0
        Integer count = 0
        Pattern skipPattern = ~/(ignore.*)/
        List<File> crawlFolders = []
        List<File> skipFolders = []
        List goodFileNames = []
        List allFileNames = []
        List<File> ignoreFiles = []

        Map options = [type     : FileType.DIRECTORIES,
                       // nameFilter   : ~/.*\.groovy/,
                       preDir   : {
                           allFileNames.addAll(it.list())
                           if (it.name ==~ skipPattern) {
                               ((File)it).eachFileRecurse {File f ->
                                   ignoreFiles << f
                               }
                               println "SKIPPING folder: $it -- matches skipPattern: $skipPattern -- files:$ignoreFiles"
                               skipFolders << it
                               return FileVisitResult.SKIP_SUBTREE

                           } else {
                               crawlFolders << it
                               println "ADDING Crawl folder: $it"
                               List<String> fileNames = it.list()
                               goodFileNames.addAll(fileNames)
                               println "\t\t\t${fileNames.size()}) $fileNames -- (${goodFileNames.size()}"
//                               count += goodFileNames.size()
//                               println "\t\t$count) Found ${goodFileNames.size()} files in $it.name totalling $totalSize bytes"
                           }
                       },
                       postDir  : {
                           println("Post dir: $it")
                           totalSize = 0
                       },
                       preRoot  : true,
                       postRoot : true,
                       visitRoot: true,
        ]


        when:
        println "Testing crawl of folder: $testFolder..."
        testFolder.traverse(options) { File folder ->
            totalSize += folder.size()
        }
        int totalFileCount = goodFileNames.size() + skipFolders.size()

        then:
        crawlFolders.size() == 4
        skipFolders.size() == 1
        goodFileNames.size() == 30
//        tot
//        count == 29
    }

    def "GetFoldersToCrawl test content folder"() {
        when:
        List<File> results = LocalFileCrawler.getFoldersToCrawl(testFolder, FolderAnalyzer.DEFAULT_FOLDERNAME_PATTERNS)

        then:
        results.size() == 4
    }

    def "GetFoldersToCrawl home/sean work folder"() {
        given:
        File workFolder = new File('/home/sean/work')

        when:
        List<File> results = LocalFileCrawler.getFoldersToCrawl(workFolder, FolderAnalyzer.DEFAULT_FOLDERNAME_PATTERNS)

        then:
        results.size() > 1190
    }


    def "GetFoldersMap test content folder"() {
//        given:
//        File workFolder = new File('/home/sean/work')

        when:
        def results = LocalFileCrawler.getFoldersToCrawlMap(testFolder, FolderAnalyzer.DEFAULT_FOLDERNAME_PATTERNS)

        then:
        results instanceof Map<String, List<File>>
        results.foldersToIgnore.size() == 1
        results.foldersToIgnore[0] instanceof File
        results.foldersToIgnore[0].name == 'ignoreMe'

        results.foldersToCrawl.size() == 4
        results.foldersToCrawl[2] instanceof File
        results.foldersToCrawl[2].name == 'subFolder2'

    }


/*
    def "GatherFolderFSToCrawl"() {
    }

    def "GetFilesToCrawl"() {
    }

    def "GetFilesToCrawlList"() {
    }
*/
}
