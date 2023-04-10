package com.oconeco.crawler

import com.oconeco.analysis.FolderAnalyzer
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
        Map<String, Pattern> namePatterns =  FolderAnalyzer.DEFAULT_FOLDERNAME_PATTERNS

        when:
        def results = crawler.startCrawl(startFolder, namePatterns)

        then:
        results.size() == 2
        results.keySet()[0] == 'foldersToCrawl'
        results.keySet()[1] == 'ignoredFolders'

//        results.keySet() == ['foldersToCrawl', 'ignoreFolders']
    }


}
