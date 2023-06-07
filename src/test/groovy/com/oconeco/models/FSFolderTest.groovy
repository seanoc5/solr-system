package com.oconeco.models

import com.oconeco.analysis.FolderAnalyzer
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
    FSFolder parentFolder = new FSFolder(startFolder, locationName, crawlName, 1)

    def "should build basic FSFolder"() {
        when:
        FSFolder fsFolder = new FSFolder(startFolder, locationName, crawlName, 1)
        fsFolder.addFolderDetails()

        then:
        fsFolder.owner != null
        fsFolder.size != null
        fsFolder.size > 0
        fsFolder.lastModifiedDate != null
        fsFolder.lastAccessDate != null
//        fsFolder.children.size() == 20
//        fsFolder.countFiles == 20
//        fsFolder.countIgnoredFiles == 1
    }

    def "items should have date and size as well as unique path:size combo"() {
        given:
        FSFolder fsFolder = new FSFolder(startFolder, locationName, crawlName, 1)

        when:
        Map<String, Object> details = fsFolder.addFolderDetails()


        then:
//        fsFolder.children.size() == 20
//        fsFolder.countFiles == 20
//        fsFolder.countSubdirs == 2
//        fsFolder.countIgnoredFiles == 1
        fsFolder.dedup.startsWith('Folder:content::')
        fsFolder.createdDate != null
        fsFolder.lastAccessDate != null
        fsFolder.lastModifiedDate != null
        fsFolder.owner != null
    }


    def "basic folder load with ignore check toSolrInputDocument"() {
        given:
        FSFolder fsFolder = new FSFolder(startFolder, locationName, crawlName, 1)
        Map<String, Object> details = fsFolder.addFolderDetails()

        when:
        SolrInputDocument sid = fsFolder.toSolrInputDocument()
        List<String> fieldNAmes = sid.getFieldNames().toList()
        Set<String> detailKeys = details.keySet()

        then:
        sid != null
        detailKeys.size() == 4
        detailKeys.containsAll(['lastAccessDate', 'lastModifiedDate', 'createdDate', 'owner'])
        fieldNAmes.size() >= 15

    }

    def "folder and files with ignore check toSolrInputDocument"() {
        given:
        FSFolder fsFolder = new FSFolder(startFolder, locationName, crawlName, 1)
        fsFolder.addFolderDetails()
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
//                                      SolrSystemClient.FLD_DEDUP, SolrSystemClient.FLD_EXTENSION_SS,
//                                      SolrSystemClient.FLD_IGNORED_FOLDERS,
//                                      SolrSystemClient.FLD_IGNORED_FILES
        ]

        then:
        sid != null
        fieldNames.containsAll(expectedNames)
        fieldNames.size() >= 15

    }

//    def "basic folder load with ignore check toSolrInputDocument"() {
//        given:
//        File src = new File(getClass().getResource('/content').toURI())
//
//        when:
//        FSFolder folder = new FSFolder(src, 1, ignoreFiles, ignoreFolders)
//        SolrInputDocument sid = folder.toSolrInputDocument()
//        List<SolrInputDocument> sidList = folder.toSolrInputDocumentList()
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


    def "folder load with toSolrInputDocument"() {
        given:
        Pattern ignoreFiles = Constants.DEFAULT_FILENAME_PATTERNS[Constants.LBL_IGNORE]

        when:
        FSFolder fsFolder = new FSFolder(startFolder, locationName, crawlName, 1)
        def details = fsFolder.addFolderDetails()
        List<SavableObject> children = fsFolder.buildChildrenList(ignoreFiles)
        List<SolrInputDocument> solrDocs = fsFolder.toSolrInputDocumentList()
        def ignoredFolders = children.findAll { it.type.containsIgnoreCase('folder') && it.ignore == true }
        def ignoredFiles = children.findAll { it.type.containsIgnoreCase('file') && it.ignore == true }

        then:
        fsFolder.children.size() == 23
        solrDocs.size() == 24
        ignoredFiles.size() == 2
        ignoredFolders.size() == 1
        ignoredFolders[0].name == 'ignoreMe'
    }


    def "load and analyze content folder"() {
        given:
//        File src = new File(getClass().getResource('/content').toURI())
        ConfigObject config = new ConfigSlurper().parse(getClass().getResource('/configLocate.groovy'))
        FolderAnalyzer analyzer = new FolderAnalyzer(config)
        FSFolder startFolder = new FSFolder(startFolder, locationName, crawlName, 1)

        when:
        def results = analyzer.analyze(startFolder)
//        log.info "Analysis results: $results"
//        Map assignmentGroupedFiles = startFolder.children.groupBy { it.assignedTypes[0] }
//        List keys = assignmentGroupedFiles.keySet().toList()

        then:
//        keys.size == 8
//        keys == ['techDev', 'archive', 'office', 'config', 'instructions', 'data', 'control', 'media']
        results != null
        results[0] == 'content'
    }

//
//    def "check overlapping start folders"() {
//        given:
//        def dataSources = [
//                localFolders: [
//                        MyDocuments: '/home/sean/Documents',
//                        MyDesktop  : '/home/sean/Desktop',
//                        MyPictures : '/home/sean/Pictures',
//                        SeanHome   : '/home/sean',
//                ]
//        ]
//
//        when:
//        // todo -- complete thing...
//        int foo = 2
//
//        then:
//        foo == 2
//
//    }
//
//    def "crawl user docs local folders"() {
//        given:
//        long start = System.currentTimeMillis()
//        long maxMem = Runtime.runtime.maxMemory()
//        long totalMem = Runtime.runtime.totalMemory()
//        println("Starting memory: $maxMem -- $totalMem")
//
//        File rootFolder = new File('/home/sean/')
//        List<File> folders = []
//        List<FSFolder> folderFSList = []
//
//        when:
//        rootFolder.eachDirRecurse {
//            folders << it
//            FSFolder folderFS = new FSFolder(it)
//
//            folderFSList << folderFS
////            println(it)
//        }
//
//        long maxMem2 = Runtime.runtime.maxMemory()
//        long totalMem2 = Runtime.runtime.totalMemory()
//        println("after memory: $maxMem2 -- $totalMem2")
//        println("difference memory: ${maxMem2 - maxMem} -- ${totalMem2 - totalMem}")
//
//        long end = System.currentTimeMillis()
//        long elapsed = end - start
//        println "Elapsed time: ${elapsed}ms (${elapsed / 1000} sec)  -- ${folders.size()}"
//
//        then:
//        folders.size() > 10
//    }


}
