package com.oconeco.models

import com.oconeco.analysis.FolderAnalyzer
import org.apache.solr.common.SolrInputDocument
import spock.lang.Specification

class FSFolderTest extends Specification {
    String locationName = 'spock'
    String crawlName = 'test'
//    def startFolder = Path.of(getClass().getResource('/content').toURI());
    File startFolder = new File(getClass().getResource('/content').toURI())

    def "should build basic FSFolder"() {
        when:
        FSFolder fsFolder = new FSFolder(startFolder, locationName, crawlName, 1)
        fsFolder.addFolderDetails()

        then:
        fsFolder.owner != null
        fsFolder.lastModifiedDate != null
        fsFolder.lastAccessDate != null
//        fsFolder.children.size() == 20
//        fsFolder.countFiles == 20
//        fsFolder.countIgnoredFiles == 1
    }

    def "items should have date and size as well as unique path:size combo"() {

        when:
        FSFolder fsFolder = new FSFolder(startFolder, locationName, crawlName, 1)
//        def rc = fsFolder.buildChildrenList()
        fsFolder.addFolderDetails()


        then:
//        fsFolder.children.size() == 20
//        fsFolder.countFiles == 20
//        fsFolder.countSubdirs == 2
//        fsFolder.countIgnoredFiles == 1
        fsFolder.uniquifier.startsWith('Folder:content::')
        fsFolder.createdDate != null
        fsFolder.lastAccessDate != null
        fsFolder.lastModifyDate != null
        fsFolder.owner != null
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
//        sidFolder.getField(SolrSaver.FLD_NAME_SIZE_S).value == 'content:1305281'
//
//
//        sidSolrxml.getFieldValues('type_s')[0] == 'File'
//        sidSolrxml.getField(SolrSaver.FLD_NAME_SIZE_S).value == 'solrconfig.xml:62263'
//
//        xmlName == 'solrconfig.xml'
//    }


    def "folder load with toSolrInputDocument"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())

        when:
        FSFolder folder = new FSFolder(src,)
        List<SolrInputDocument> solrDocs = folder.toSolrInputDocumentList()

        then:
        folder.fsFiles.size() == 20    //14
        solrDocs.size() == 21
        folder.subDirectories.size() == 1
        folder.ignoredFolders.size() == 0
        folder.ignoredDirectories.size() == 1
        folder.ignoredFileNames.size() == 1
    }


    def "load and analyze folder"() {
        given:
//        File src = new File(getClass().getResource('/content').toURI())
        ConfigObject config = new ConfigSlurper().parse(getClass().getResource('/configLocate.groovy'))
        FolderAnalyzer analyzer = new FolderAnalyzer(config)
        FSFolder startFolder = new FSFolder(startFolder, 1, ignoreFiles, ignoreFolders)

        when:
        def foo = startFolder.analyze(analyzer)
        Map assignmentGroupedFiles = startFolder.children.groupBy { it.assignedTypes[0] }
        List keys = assignmentGroupedFiles.keySet().toList()

        then:
        keys.size == 8
        keys == ['techDev', 'archive', 'office', 'config', 'instructions', 'data', 'control', 'media']
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
