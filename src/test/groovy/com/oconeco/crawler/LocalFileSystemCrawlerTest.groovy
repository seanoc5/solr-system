package com.oconeco.crawler

import com.oconeco.helpers.Constants
import com.oconeco.models.FSFile
import com.oconeco.models.FSFolder
import com.oconeco.models.SavableObject
import com.oconeco.persistence.SolrSystemClient
import org.apache.log4j.Logger
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
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
    Pattern ignoreFolders = Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE]
    Pattern ignoreFiles = Constants.DEFAULT_FILENAME_PATTERNS[Constants.LBL_IGNORE]
    Path startFolder = Path.of(getClass().getResource('/content').toURI());
//    SolrSystemClient mockSolrClient = new SolrSystemClient()
    def mockSolrClient = null
    SolrDocumentList existingSolrFolderDocsBlank = []
    File startFile = new File(getClass().getResource('/content').toURI())
    FSFolder startFSFolder = new FSFolder(startFile, locationName,crawlName,0)
    Map<String, SolrDocument> existingSolrFolderDocsMocked = mockSolrFolderDocs([startFSFolder])
    DifferenceChecker differenceChecker = new DifferenceChecker()

    def "basic localhost crawl of 'content' resource folder"() {
        given:
        LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, crawlName, differenceChecker, mockSolrClient)

        when:
        List<FSFolder> results = crawler.buildCrawlFolders(crawlName, startFolder, ignoreFolders, ignoreFiles, null)
        def toCrawl = results.findAll { !it.ignore }
        def toIgnore = results.findAll { it.ignore }

        List<String> crawlNames = toCrawl?.collect { FSFolder fsFolder -> fsFolder.name }
        def ignoreNames = toIgnore?.collect { FSFolder fsFolder -> fsFolder.name }

        List<String> cnames = ['content', 'testsub', 'subfolder2', 'subfolder3']
        FSFolder firstFsFolderToCrawl = toCrawl[0]
        FSFolder secondFsFolderToCrawl = toCrawl[1]

        then:
        results != null
        results.size() == 4
        toCrawl.size() == 4
        crawlNames.containsAll(cnames)
        firstFsFolderToCrawl.name == 'content'
        firstFsFolderToCrawl.depth == 0
        firstFsFolderToCrawl.type == FSFolder.TYPE
        secondFsFolderToCrawl.depth == 1

        ignoreNames.containsAll(['ignoreMe'])
    }


    def 'basic LocalFileSystemCrawler.crawlFolders'() {
        given:
        LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, crawlName, differenceChecker, mockSolrClient )
//        LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, crawlName)

        when:
        def results = crawler.crawlFolders(crawlName, startFolder.toFile(), ignoreFolders, ignoreFiles,null)

        then:
        results!=null

    }


    def 'basic localhost with child folders and folder FSFiles lists'() {
        given:
        LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, crawlName, differenceChecker, mockSolrClient)
        Map<String, List<SavableObject>> folderFilesMap = [:]
//        List<String> ignoreList = []


        when:
        List<FSFolder> allFolders = crawler.buildCrawlFolders(crawlName, startFolder, ignoreFolders, null)
        Map<String, SolrDocument> existingSolrFolderDocs = mockSolrFolderDocs(allFolders)
        allFolders.each { FSFolder fsFolder ->
            if (fsFolder.ignore) {
                log.info "Skip crawling files in folder: $fsFolder"
                ignoreList << fsFolder
            } else {
                log.info "crawl files in folder: $fsFolder"
                 def folderFiles= crawler.populateFolderFsFiles(fsFolder, ignoreFolders)
                folderFilesMap[fsFolder.name] = folderFiles
                log.info "Folder files: ${folderFiles.size()}"
            }
        }
        List<SavableObject> contentChildren = folderFilesMap['content']
        List<SavableObject> testsubChildren = folderFilesMap['testsub']
        List<SavableObject> subfolder2Children = folderFilesMap['subfolder2']
        List<SavableObject> subfolder3Children = folderFilesMap['subfolder3']

        then:
//        ignoreList[0].name == 'ignoreMe'
        allFolders.size() == 5
        folderFilesMap.keySet().containsAll(['content', 'testsub', 'subfolder2', 'subfolder3'])
        contentChildren.size()==21
        testsubChildren.size() == 1
        subfolder2Children.size() == 3
        subfolder3Children.size() == 1
    }


    def 'basic localhost with archives included'() {
        given:
        LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, crawlName, differenceChecker, mockSolrClient)
        List<FSFolder> crawlFolders = crawler.buildCrawlFolders(crawlName, startFolder, ignoreFolders, null)
        List<FSFile> archiveFSFiles = []
        List<SavableObject> archiveItems = []

        when:
        crawlFolders.each {FSFolder fsFolder ->
            if(!fsFolder.ignore){
                List<FSFile> folderFiles= crawler.populateFolderFsFiles(fsFolder, ignoreFolders)
                List<FSFile> archiveFiles = folderFiles.findAll {it.isArchive()}
                if(archiveFiles?.size()) {
                    archiveFSFiles.addAll(archiveFiles)
                    archiveFiles.each { FSFile fsFile ->
                        def archEntries = fsFile.gatherArchiveEntries()
                        archiveItems.addAll(archEntries)
                    }
                } else {
                    log.debug "\t\t not an archive file, so no archive entries in folder: $fsFolder "
                }
            } else {
                log.debug "\t\t------ Ignore Folder: $fsFolder"
            }
        }

        then:
        archiveFSFiles?.size() == 5
        archiveFSFiles[0].name == 'test.zip'

        archiveItems.size()==174

    }

    def 'check folders that need updating'() {
        given:
        LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, crawlName, differenceChecker, mockSolrClient)
        Map<String, List<SavableObject>> folderFilesMap = [:]
        List<FSFolder> allFolders = crawler.buildCrawlFolders(crawlName, startFolder, ignoreFolders, null)
        Map<String, SolrDocument> existingSolrFolderDocs = mockSolrFolderDocs(allFolders)

        when:
        // hacked approach -- todo -- revisit
        log.info "Re-build crawl Folders now that we have mocked existingSolrFolderDocs..."
        def idKeys = existingSolrFolderDocs.keySet()

        SolrDocument doc0 = existingSolrFolderDocs.get(idKeys[0])
        // expecting first solr doc to be an hour older
        Date date0 = doc0.getFirstValue(SolrSystemClient.FLD_LAST_MODIFIED)
        doc0.setField(SolrSystemClient.FLD_LAST_MODIFIED, new Date(date0.getTime() - 3600*1000))

        SolrDocument doc1 = existingSolrFolderDocs.get(idKeys[1])
        Integer size1 = doc1.getFirstValue(SolrSystemClient.FLD_SIZE)
        doc1.setField(SolrSystemClient.FLD_SIZE, size1 -1)

        List<FSFolder> updatableFolders = crawler.buildCrawlFolders(crawlName, startFolder, ignoreFolders, existingSolrFolderDocs)

        then:
        allFolders!= null
        allFolders.size() == 4

        updatableFolders!=null
        updatableFolders.size() == 2
    }


    /**
     * help call for testing only
     * @param sourceFolders
     * @return
     */
    static Map<String, SolrDocument> mockSolrFolderDocs(List<FSFolder> sourceFolders ){
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
