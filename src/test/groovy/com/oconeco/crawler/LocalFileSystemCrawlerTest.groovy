package com.oconeco.crawler


import com.oconeco.helpers.Constants
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
        LocalFileSystemCrawler crawler = new LocalFileSystemCrawler('testCrawl', hostname)
        Map<String, Pattern> namePatterns =  Constants.DEFAULT_FOLDERNAME_PATTERNS

        when:
        def results = crawler.startCrawl(startFolder, namePatterns)
        def toCrawl = results[Constants.TOCRAWL]
        def toIgnore = results[Constants.TOIGNORE]
        List<String> crawlNames = toCrawl.collect{File file ->
            file.name
        }
        def ignoreNames = results[Constants.TOIGNORE].collect { File file -> file.name}
        List<String> cnames = ['content','testsub','subFolder2', 'subfolder3']

        then:
        results.size() == 2
        toCrawl.size() == 4
        crawlNames.containsAll(cnames)

        ignoreNames.containsAll(['ignoreMe'])
    }


 def "hacked test for specific local filesystem"() {
        given:
        long start = System.currentTimeMillis()

        String hostname = InetAddress.getLocalHost().getHostName()
        def userHome = System.getProperty("user.home")
        File startFolder = new File("${userHome}/Documents")
        LocalFileSystemCrawler crawler = new LocalFileSystemCrawler('testSeanHome', hostname)
        Map<String, Pattern> namePatterns =  Constants.DEFAULT_FOLDERNAME_PATTERNS

        when:
        def results = crawler.startCrawl(startFolder, namePatterns)

        long end = System.currentTimeMillis()
        long elapsed = end - start
        println "Elapsed time: ${elapsed}ms (${elapsed/1000} sec)"

        then:
        results.size() == 2
        results.keySet()[0] == Constants.TOCRAWL
        results.keySet()[1] == Constants.TOIGNORE

//        results.keySet() == ['foldersToCrawl', 'ignoreFolders']
    }


}
