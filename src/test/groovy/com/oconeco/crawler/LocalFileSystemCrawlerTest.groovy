package com.oconeco.crawler


import com.oconeco.helpers.Constants
import com.oconeco.models.FSFolder
import com.oconeco.models.SavableObject
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


    def "basic localhost crawl of 'content' resource folder"() {
        given:
        LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, crawlName)

        when:
        def results = crawler.buildCrawlFolders(crawlName, startFolder, ignorePattern, existingSolrFolderDocs)
        def toCrawl = results.findAll { !it.ignore }
        def toIgnore = results.findAll { it.ignore }

        List<String> crawlNames = toCrawl?.collect { FSFolder fsFolder -> fsFolder.name }
        def ignoreNames = toIgnore?.collect { FSFolder fsFolder -> fsFolder.name }

        List<String> cnames = ['content', 'testsub', 'subfolder2', 'subfolder3']
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


    def 'basic localhost with child folders'() {
        given:
        LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, crawlName)
        Map<String, List<SavableObject>> folderFilesMap = [:]
        List<String> ignoreList = []

        when:
        def results = crawler.buildCrawlFolders(crawlName, startFolder, ignorePattern, existingSolrFolderDocs)
        results.each { FSFolder fsFolder ->
            if (fsFolder.ignore) {
                log.info "Skip crawling files in folder: $fsFolder"
                ignoreList << fsFolder
            } else {
                log.info "crawl files in folder: $fsFolder"
                 def folderFiles= crawler.populateFolderFsFiles(fsFolder, ignorePattern)
                folderFilesMap[fsFolder.name] = folderFiles
                log.info "Folder files: ${folderFiles.size()}"
            }
        }
        List<SavableObject> contentChildren = folderFilesMap['content']
        List<SavableObject> testsubChildren = folderFilesMap['testsub']
        List<SavableObject> subfolder2Children = folderFilesMap['subfolder2']
        List<SavableObject> subfolder3Children = folderFilesMap['subfolder3']

        then:
        ignoreList[0].name == 'ignoreMe'
        results.size() == 5
        folderFilesMap.keySet().containsAll(['content', 'testsub', 'subfolder2', 'subfolder3'])
        contentChildren.size()==21
        testsubChildren.size() == 1
        subfolder2Children.size() == 3
        subfolder3Children.size() == 1
    }


}
