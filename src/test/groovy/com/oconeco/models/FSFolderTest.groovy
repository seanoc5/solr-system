package com.oconeco.models

import com.oconeco.analysis.BaseAnalyzer
import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSystemClient
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
        FSFolder fsFolder = new FSFolder(startFolder, null, locationName, crawlName )
//        fsFolder.addFolderDetails()
        String pid = SavableObject.buildId(locationName, startFolder.parent.replaceAll('\\\\', '/'))

        then:
        fsFolder.parentId == pid
        fsFolder.parent == null
        fsFolder.children == null
        fsFolder.size > 1           // could test actual size, but that might change as we update test folder contents
        fsFolder.lastModifiedDate != null

        fsFolder.owner == null
    }

    def "should build basic FSFolder with folder details"() {
        when:
        FSFolder fsFolder = new FSFolder(startFolder, null, locationName, crawlName )
        fsFolder.addFileDetails()
        String pid = SavableObject.buildId(locationName, startFolder.parent.replaceAll('\\\\', '/'))

        then:
        fsFolder.owner != null
        fsFolder.size != null
        fsFolder.size > 0
        fsFolder.lastModifiedDate != null
        fsFolder.lastAccessDate != null
        fsFolder.parent == null
        fsFolder.parentId == pid

        fsFolder.children == null
    }


    def "should build basic FSFolder with parentObject"() {
        when:
        FSFolder fsFolder = new FSFolder(startFolder, parentFolder, locationName, crawlName )
        fsFolder.addFileDetails()

        then:
        fsFolder.parent == parentFolder
        fsFolder.parentId == parentFolder.id
    }


    def "items should have date and size as well as unique path:size combo"() {
        given:
        FSFolder fsFolder = new FSFolder(startFolder, parentFolder, locationName, crawlName )

        when:
        String dedup = fsFolder.type + ':' + fsFolder.name + '::' + fsFolder.size

        then:
        fsFolder.dedup.startsWith(dedup)
    }


    def "basic folder load with ignore check toSolrInputDocument"() {
        given:
        FSFolder fsFolder = new FSFolder(startFolder, parentFolder, locationName, crawlName)
        Map<String, Object> details = fsFolder.addFileDetails()

        when:
        SolrInputDocument sid = fsFolder.toSolrInputDocument()
        List<String> fieldNAmes = sid.getFieldNames().toList()
        Set<String> detailKeys = details.keySet()
        List<String> expectedKeys = ['lastAccessDate', 'createdDate', 'owner']

        then:
        sid != null
        detailKeys.size() == expectedKeys.size()
        detailKeys.containsAll(expectedKeys)
        fieldNAmes.size() >= 15

    }

    def "folder and files with ignore check toSolrInputDocument"() {
        given:
        FSFolder fsFolder = new FSFolder(startFolder, parentFolder, locationName, crawlName)
        fsFolder.addFileDetails()
        fsFolder.buildChildrenList(Constants.DEFAULT_FILENAME_PATTERNS[Constants.LBL_IGNORE])

        when:
        def sid = fsFolder.toSolrInputDocument()
        List<String> fieldNames = sid.getFieldNames().toList()
        List<String> expectedNames = [SolrSystemClient.FLD_ID, SolrSystemClient.FLD_TYPE,
                                      SolrSystemClient.FLD_NAME_S, SolrSystemClient.FLD_NAME_T, SolrSystemClient.FLD_PATH_S, SolrSystemClient.FLD_PATH_T,
                                      SolrSystemClient.FLD_CRAWL_NAME, SolrSystemClient.FLD_LOCATION_NAME, SolrSystemClient.FLD_DEDUP,
                                      SolrSystemClient.FLD_SIZE, SolrSystemClient.FLD_DEPTH,
                                      SolrSystemClient.FLD_LOCATION_NAME,
                                      SolrSystemClient.FLD_LAST_MODIFIED,
//                                      SolrSystemClient.FLD_CHILD_FILENAMES, SolrSystemClient.FLD_CHILD_DIRNAMES,
                                      SolrSystemClient.FLD_DEDUP,
//                                      SolrSystemClient.FLD_IGNORED_FOLDERS, SolrSystemClient.FLD_EXTENSION_SS,
//                                      SolrSystemClient.FLD_IGNORED_FILES
        ]

        then:
        sid != null
        fieldNames.containsAll(expectedNames)
        fieldNames.size() >= 15

    }

//    def "basic startFolder load with ignore check toSolrInputDocument"() {
//        given:
//        File src = new File(getClass().getResource('/content').toURI())
//
//        when:
//        FSFolder startFolder = new FSFolder(src, 1, ignoreFiles, ignoreFolders)
//        SolrInputDocument sid = startFolder.toSolrInputDocument()
//        List<SolrInputDocument> sidList = startFolder.toSolrInputDocumentList()
//        SolrInputDocument sidFolder = sidList[0]
//        String folderName = sidFolder.getField('name_s').value
//        SolrInputDocument sidSolrxml = sidList.find {
//            it.getField('name_s').value == 'solrconfig.xml'
//        }
//        String xmlName = sidSolrxml.getField('name_s').value
//
//        then:
//        sid instanceof SolrInputDocument
//        sid.size() == 9
//
//        sidList instanceof List<SolrInputDocument>
//        sidList.size() == 21
//        sidFolder.getFieldValues('type_s')[0] == 'Folder'
//        // test 'uniqify' value
//        sidFolder.getField(SolrSystemClient.FLD_NAME_SIZE_S).value == 'content:1305281'
//
//
//        sidSolrxml.getFieldValues('type_s')[0] == 'File'
//        sidSolrxml.getField(SolrSystemClient.FLD_NAME_SIZE_S).value == 'solrconfig.xml:62263'
//
//        xmlName == 'solrconfig.xml'
//    }




    def "load and analyze content folder with configLocate.groovy"() {
        given:
//        File src = new File(getClass().getResource('/content').toURI())
        ConfigObject config = new ConfigSlurper().parse(getClass().getResource('/configLocate.groovy'))
//        FolderAnalyzer analyzer = new FolderAnalyzer(config)
        FSFolder startFolder = new FSFolder(startFolder, parentFolder, locationName, crawlName)
        BaseAnalyzer analyzer = new BaseAnalyzer()

        when:
        def results = analyzer.analyze(startFolder)
//        log.info "Analysis results: $results"
//        Map assignmentGroupedFiles = startFolder.children.groupBy { it.assignedTypes[0] }
//        List keys = assignmentGroupedFiles.keySet().toList()

        then:
//        keys.size == 8
//        keys == ['techDev', 'archive', 'office', 'config', 'instructions', 'data', 'control', 'media']
        results instanceof List<Map<String, Map<String, Object>>>
        results[0].keySet()[0] == 'default'     // todo -- fixme??
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
