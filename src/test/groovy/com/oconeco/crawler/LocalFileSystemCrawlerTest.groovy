package com.oconeco.crawler


import com.oconeco.helpers.Constants
import com.oconeco.models.FSFolder
import spock.lang.Specification

import java.nio.file.Path
import java.util.regex.Pattern
/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   10/15/22, Saturday
 * @description:
 */

class LocalFileSystemCrawlerTest extends Specification {
    def "basic localhost crawl of test folder inside code folder "() {
        given:
        String hostname = InetAddress.getLocalHost().getHostName()
        def startFolder = Path.of(getClass().getResource('/content').toURI());
        LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(hostname, 'testCrawl')
        Map<String, Pattern> namePatterns =  Constants.DEFAULT_FOLDERNAME_PATTERNS

        when:
        Map<String, List> results = crawler.buildCrawlFolders(startFolder, namePatterns)
        def toCrawl = results[Constants.LBL_CRAWL]
        def toIgnore = results[Constants.LBL_IGNORE]
        List<String> crawlNames = toCrawl?.collect{ FSFolder fsFolder ->
            fsFolder.name
        }
        def ignoreNames = results[Constants.LBL_IGNORE]?.collect { FSFolder fsFolder -> fsFolder.name}
        List<String> cnames = ['content','testsub','subFolder2', 'subfolder3']
        FSFolder firstFsFolderToCrawl = toCrawl[0]

        then:
        results.size() == 2
        toCrawl.size() == 4
        crawlNames.containsAll(cnames)
        firstFsFolderToCrawl.name == 'content'
        firstFsFolderToCrawl.depth == 1
        firstFsFolderToCrawl.type == FSFolder.TYPE

        ignoreNames.containsAll(['ignoreMe'])
    }


 def "hacked test for specific local filesystem"() {
        given:
        long start = System.currentTimeMillis()

        String hostname = InetAddress.getLocalHost().getHostName()
        def userHome = System.getProperty("user.home")
        File startFolder = new File("${userHome}/Documents")
        LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(hostname, 'testSeanHome')
        Map<String, Pattern> namePatterns =  Constants.DEFAULT_FOLDERNAME_PATTERNS

        when:
        def results = crawler.buildCrawlFolders(startFolder, namePatterns)

        long end = System.currentTimeMillis()
        long elapsed = end - start
        println "Elapsed time: ${elapsed}ms (${elapsed/1000} sec)"

        then:
        results.size() == 2
        results.keySet()[0] == Constants.LBL_CRAWL
        results.keySet()[1] == Constants.LBL_IGNORE

//        results.keySet() == ['foldersToCrawl', 'ignoreFolders']
    }


}
