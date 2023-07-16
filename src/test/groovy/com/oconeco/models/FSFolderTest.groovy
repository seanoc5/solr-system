package com.oconeco.models

import com.oconeco.analysis.BaseAnalyzer
import com.oconeco.helpers.Constants
import org.apache.solr.common.SolrInputDocument
import spock.lang.Specification

import java.util.regex.Pattern

class FSFolderTest extends Specification {
    String locationName = 'spock'
    String crawlName = 'test'
//    def startFolder = Path.of(getClass().getResource('/content').toURI());
    File startFolder = new File(getClass().getResource('/content').toURI())
    Pattern ignoreFolders = Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE]
    Pattern ignoreFiles = Constants.DEFAULT_FILENAME_PATTERNS[Constants.LBL_IGNORE]
    FSFolder parentFolder = new FSFolder(startFolder, null, locationName, crawlName)

    def "should build basic FSFolder"() {
        when:
        FSFolder fsFolder = new FSFolder(startFolder, null, locationName, crawlName)
//        fsFolder.addFolderDetails()
        String pid = SavableObject.buildId(locationName, startFolder.parent.replaceAll('\\\\', '/'))

        then:
        fsFolder.parentId == pid
        fsFolder.parent == null
        fsFolder.childGroups == []
        fsFolder.childItems == []
        fsFolder.name == 'content'           // could test actual size, but that might change as we update test folder contents
        fsFolder.lastModifiedDate != null
        fsFolder.childItems != null
        fsFolder.childGroups != null

        fsFolder.owner == null
    }

    def "should build basic FSFolder with folder details"() {
        when:
        FSFolder fsFolder = new FSFolder(startFolder, null, locationName, crawlName)
        fsFolder.addFileDetails()
        String pid = SavableObject.buildId(locationName, startFolder.parent.replaceAll('\\\\', '/'))

        then:
        fsFolder.owner != null
        fsFolder.size != null
        fsFolder.size == 0
        fsFolder.lastModifiedDate != null
        fsFolder.lastAccessDate != null
        fsFolder.parent == null
        fsFolder.parentId == pid

        fsFolder.childItems == []
        fsFolder.childGroups == []
    }


    def "should build basic FSFolder with parentObject"() {
        when:
        FSFolder fsFolder = new FSFolder(startFolder, parentFolder, locationName, crawlName)
        fsFolder.addFileDetails()

        then:
        fsFolder.parent == parentFolder
        fsFolder.parentId == parentFolder.id
    }


    def "items should have date and size as well as unique path:size combo"() {
        given:
        long testSize = 1234
        FSFolder fsFolder = new FSFolder(startFolder, parentFolder, locationName, crawlName)
        fsFolder.size = testSize
        fsFolder.buildDedupString()

        when:
        String dedup = fsFolder.type + ':' + fsFolder.name + '::' + testSize

        then:
        fsFolder.dedup.startsWith(dedup)
        SavableObject.buildDedupString(fsFolder.type, fsFolder.name, testSize) == dedup
    }


    def "basic folder load with ignore check toSolrInputDocument"() {
        given:
        FSFolder fsFolder = new FSFolder(startFolder, parentFolder, locationName, crawlName)
        Map<String, Object> details = fsFolder.addFileDetails()

        when:
        SolrInputDocument sid = fsFolder.toPersistenceDocument()
        List<String> fieldNAmes = sid.getFieldNames().toList()
        Set<String> detailKeys = details.keySet()
        List<String> expectedKeys = ['lastAccessDate', 'createdDate', 'owner']

        then:
        sid != null
        detailKeys.size() == expectedKeys.size()
        detailKeys.containsAll(expectedKeys)
        fieldNAmes.size() >= 15

    }


    def "load and analyze content folder with configLocate.groovy"() {
        given:
        ConfigObject config = new ConfigSlurper().parse(getClass().getResource('/configLocate.groovy'))
        FSFolder startFolder = new FSFolder(startFolder, parentFolder, locationName, crawlName)
        BaseAnalyzer analyzer = new BaseAnalyzer()

        when:
        def results = analyzer.analyze(startFolder)

        then:
        results instanceof Map<String, Map<String, Object>>
        results.keySet()[0] == 'track'     // todo -- fixme??
    }


    def "check overlapping start folders"() {
        given:
        def dataSources = [
                localFolders: [
                        MyDocuments: '/home/sean/Documents',
                        MyDesktop  : '/home/sean/Desktop',
                        MyPictures : '/home/sean/Pictures',
                        SeanHome   : '/home/sean',
                ]
        ]

        when:
        // todo -- complete thing...
        int foo = 2

        then:
        foo == 2

    }

}
