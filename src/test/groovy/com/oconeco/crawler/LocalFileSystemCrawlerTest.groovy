package com.oconeco.crawler


import com.oconeco.helpers.Constants
import com.oconeco.models.FSFolder
import org.apache.log4j.Logger
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
    Logger log = Logger.getLogger(this.class.name);
    String locationName = 'spock'
    String crawlName = 'test'
    Pattern ignorePattern = Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE]
    def startFolder = Path.of(getClass().getResource('/content').toURI());
    LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, crawlName)


    def "basic localhost crawl of test folder inside code folder "() {

        when:
        def results = crawler.buildCrawlFolders(startFolder, ignorePattern)
        def toCrawl = results.findAll { !it.ignore }
        def toIgnore = results.findAll { it.ignore }

        List<String> crawlNames = toCrawl?.collect { FSFolder fsFolder -> fsFolder.name }
        def ignoreNames = toIgnore?.collect { FSFolder fsFolder -> fsFolder.name }

        List<String> cnames = ['content', 'testsub', 'subFolder2', 'subfolder3']
        FSFolder firstFsFolderToCrawl = toCrawl[0]

        then:
        results != null
        results.size() == 5
        toCrawl.size() == 4
        crawlNames.containsAll(cnames)
        firstFsFolderToCrawl.name == 'content'
        firstFsFolderToCrawl.depth == 1
        firstFsFolderToCrawl.type == FSFolder.TYPE

        ignoreNames.containsAll(['ignoreMe'])
    }


    def "crawl and process archive files to solr input docs"() {
        given:
        LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, 'testCrawl')
//        Pattern ignoreNamePattern = Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE]

        when:
        def results = crawler.buildCrawlFolders(startFolder, ignorePattern)
        results.each {FSFolder fsFolder ->
            if(fsFolder.ignore) {
                log.info "Skip crawling files in folder: $fsFolder"
            } else{
                log.info "crawl files in folder: $fsFolder"
                def folderFiles = crawler.getFolderFiles(fsFolder, ignorePattern)
                log.info "Folder files: ${folderFiles.size()}"
            }
        }

        then:
        results.size() == 5

    }


}
