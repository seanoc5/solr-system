package com.oconeco.crawler

import com.oconeco.analysis.FileSystemAnalyzer
import com.oconeco.helpers.Constants
import com.oconeco.models.FSFile
import com.oconeco.models.FSFolder
import com.oconeco.models.FSObject
import com.oconeco.models.SavableObject
import com.oconeco.persistence.SolrSystemClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import spock.lang.Specification

import java.nio.file.Path

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   10/15/22, Saturday
 * @description:
 */

class LocalFileSystemCrawlerTest extends Specification {
//    Logger log = LogManager.getLogger(this.class.name)
    Logger log = LogManager.getLogger()
    String locationName = 'spock'
    String crawlName = 'test'
    Path startFolder = Path.of(getClass().getResource('/content').toURI())
    SolrSystemClient client = new SolrSystemClient('http://oldie:8983/solr/solr_system')
    SolrDocumentList existingSolrFolderDocsBlank = []
    File startFile = new File(getClass().getResource('/content').toURI())
    FSFolder startFSFolder = new FSFolder(startFile, null, locationName, crawlName)
    Map<String, SolrDocument> existingSolrFolderDocsMocked = mockSolrFolderDocs([startFSFolder])
    BaseDifferenceChecker differenceChecker = new BaseDifferenceChecker()
    FileSystemAnalyzer analyzer = new FileSystemAnalyzer(Constants.DEFAULT_FOLDERNAME_LOCATE, Constants.DEFAULT_FILENAME_LOCATE)


    def 'basic LocalFileSystemCrawler.crawlFolders no existing docs'() {
        given:
        Map<String, Map<String, Object>> folderLabels = Constants.DEFAULT_FOLDERNAME_LOCATE
        Map<String, Map<String, Object>> fileLabels = Constants.DEFAULT_FILENAME_PARSE
        FileSystemAnalyzer analyzerParsing = new FileSystemAnalyzer(folderLabels, fileLabels)
        LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, client, differenceChecker)
        Map<String, SolrDocument> existingSolrFolderDocs = null
        boolean compareExistingSolrFolderDocs = false

        when:
        Map<String, List<FSFolder>> results = crawler.crawlFolders(crawlName, startFolder.toFile(), existingSolrFolderDocs, analyzerParsing)

        then:
        results != null
        results.updated.size() == 4
        results.updated[0].name == 'content'
        results.updated[0].labels == [Constants.TRACK]
        results.updated[0].matchedLabels.get(Constants.TRACK).analysis == [Constants.TRACK]

        results.updated[1].name == 'testsub'
        results.updated[2].name == 'subfolder2'

        results.skipped.size() == 1
    }


    def 'basic LocalFileSystemCrawler.crawlFolders with existing docs'() {
        given:
        LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, client, differenceChecker)

        when:
        def results = crawler.crawlFolders(crawlName, startFolder.toFile(), existingSolrFolderDocsMocked, analyzer)

        then:
        results != null
        results.skipped.size() == 1
        results.current.size() == 1
        results.updated.size() == 3
    }


    def 'process archives in content folder'() {
        given:
        LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, client, differenceChecker)
//        List<FSFile> archiveFSFiles = []
//        List<SavableObject> archiveItems = []

        when:
        List<SavableObject> crawlResults = crawler.crawlFolderFiles(startFSFolder, analyzer)
        def archiveFiles = startFSFolder.children.findAll {FSObject fsObject ->
            fsObject.archive
        }
        FSFile testzip = archiveFiles.find{it.name=='test.zip'}
        def testzipEntries = testzip.gatherArchiveEntries()
        FSFile fuzzy = archiveFiles.find{it.name=='fuzzywuzzy.tar.gz'}
        def fuzzyEntries = fuzzy.gatherArchiveEntries()


        then:
        archiveFiles.size() == 5
        testzipEntries.size() == 5
        fuzzyEntries.size() == 123
//        archiveItems.size() == 174
    }




//    def 'check folders that need updating'() {
//        given:
//        LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, crawlName, client, differenceChecker)
//        Map<String, List<SavableObject>> folderFilesMap = [:]
//        List<FSFolder> allFolders = crawler.buildCrawlFolders(crawlName, startFolder, ignoreFolders, null)
//        Map<String, SolrDocument> existingSolrFolderDocs = mockSolrFolderDocs(allFolders)
//
//        when:
//        // hacked approach -- todo -- revisit
//        log.info "Re-build crawl Folders now that we have mocked existingSolrFolderDocs..."
//        def idKeys = existingSolrFolderDocs.keySet()
//
//        SolrDocument doc0 = existingSolrFolderDocs.get(idKeys[0])
//        // expecting first solr doc to be an hour older
//        Date date0 = doc0.getFirstValue(SolrSystemClient.FLD_LAST_MODIFIED)
//        doc0.setField(SolrSystemClient.FLD_LAST_MODIFIED, new Date(date0.getTime() - 3600*1000))
//
//        SolrDocument doc1 = existingSolrFolderDocs.get(idKeys[1])
//        Integer size1 = doc1.getFirstValue(SolrSystemClient.FLD_SIZE)
//        doc1.setField(SolrSystemClient.FLD_SIZE, size1 -1)
//
//        List<FSFolder> updatableFolders = crawler.buildCrawlFolders(crawlName, startFolder, ignoreFolders, existingSolrFolderDocs)
//
//        then:
//        allFolders!= null
//        allFolders.size() == 4
//
//        updatableFolders!=null
//        updatableFolders.size() == 2
//    }
//

/**
 * help call for testing only
 * @param sourceFolders
 * @return
 */
    static Map<String, SolrDocument> mockSolrFolderDocs(List<FSFolder> sourceFolders) {
        Map<String, SolrDocument> sdocMap = [:]
        sourceFolders.each { FSFolder fsFolder ->
            SolrDocument solrDocument = new SolrDocument()
            solrDocument.setField('id', fsFolder.id)
            solrDocument.setField(SolrSystemClient.FLD_LAST_MODIFIED, fsFolder.lastModifiedDate)
            solrDocument.setField(SolrSystemClient.FLD_SIZE, fsFolder.size)
            solrDocument.setField(SolrSystemClient.FLD_DEDUP, fsFolder.dedup)
            solrDocument.setField(SolrSystemClient.FLD_PATH_S, fsFolder.path)
            solrDocument.setField(SolrSystemClient.FLD_PATH_T, fsFolder.path)
            solrDocument.setField(SolrSystemClient.FLD_NAME_S, fsFolder.name)
            solrDocument.setField(SolrSystemClient.FLD_NAME_T, fsFolder.name)
            solrDocument.setField(SolrSystemClient.FLD_LOCATION_NAME, fsFolder.locationName)
            solrDocument.setField(SolrSystemClient.FLD_CRAWL_NAME, fsFolder.crawlName)
//            solrDocument.setField(SolrSystemClient.FLD_, fsFolder.)
//            solrDocument.setField(SolrSystemClient.FLD_, fsFolder.)
            sdocMap.put(fsFolder.id, solrDocument)
        }
        return sdocMap
    }

}
