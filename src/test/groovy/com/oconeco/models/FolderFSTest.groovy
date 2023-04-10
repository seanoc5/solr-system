package com.oconeco.models

import com.oconeco.analysis.FolderAnalyzer
import com.oconeco.persistence.SolrSaver
import org.apache.solr.common.SolrInputDocument
import spock.lang.Specification

import java.util.regex.Pattern

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

class FolderFSTest extends Specification {
    Pattern ignoreFolders = FolderAnalyzer.DEFAULT_FOLDERNAME_PATTERNS.ignore
    Pattern ignoreFiles = FolderAnalyzer.DEFAULT_FILENAME_PATTERNS.ignore

    def "basic folder load"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())

        when:
        FolderFS folder = new FolderFS(src, 1, ignoreFiles, ignoreFolders)

        then:
        folder.size == 727444
        folder.countTotal == 21

        folder.filesFS.size() == 20
        folder.countFiles == 20

        folder.subDirectories.size() == 1
        folder.countSubdirs == 1
    }


    def "basic folder load with ignore file and folder patterns"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())

        when:
        FolderFS folder = new FolderFS(src, 1, ignoreFiles, ignoreFolders)

        then:
        folder.size == 727444       //642754
        folder.countTotal == 21

        folder.filesFS.size() == 20
        folder.countFiles == 20

        folder.subDirectories.size() == 1
        folder.countSubdirs == 1

        folder.ignoredDirectories.size() == 1
        folder.ignoredFolders.size() == 0
    }


    def "recursive folder load with ignore file and folder patterns"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())

        when:
        boolean recurse = true
        FolderFS folderFS = new FolderFS(src, 1, ignoreFiles, ignoreFolders, recurse)

        then:
        folderFS.size == 727444       //642754
        folderFS.countTotal == 21

        folderFS.filesFS.size() == 20
        folderFS.countFiles == 20

        folderFS.subDirectories.size() == 1
        folderFS.countSubdirs == 1

        folderFS.ignoredDirectories.size() == 1
        folderFS.ignoredFolders.size() == 0
    }


    def "recursive folder load get all subfolders"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())

        when:
        boolean recurse = true
        FolderFS folderFS = new FolderFS(src, 1, ignoreFiles, ignoreFolders, recurse)
        List<FolderFS> allSubFolders = folderFS.getAllSubFolders()

        then:
        allSubFolders instanceof List<FolderFS>
        allSubFolders.size() == 3

    }


    def "basic folder load with ignore check toSolrInputDocument"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())

        when:
        FolderFS folder = new FolderFS(src, 1, ignoreFiles, ignoreFolders)
        SolrInputDocument sid = folder.toSolrInputDocument()
        List<SolrInputDocument> sidList = folder.toSolrInputDocumentList()
        SolrInputDocument sidFolder = sidList[0]
        String folderName = sidFolder.getField('name_s').value
        SolrInputDocument sidSolrxml = sidList.find {
            it.getField('name_s').value == 'solrconfig.xml'
        }
        String xmlName = sidSolrxml.getField('name_s').value

        then:
        sid instanceof SolrInputDocument
        sid.size() == 7

        sidList instanceof List<SolrInputDocument>
        sidList.size() == 21
        sidFolder.getFieldValues('type_s')[0] == 'Folder'
        // test 'uniqify' value
        sidFolder.getField(SolrSaver.FLD_NAME_SIZE_S).value == 'content:727444'


        sidSolrxml.getFieldValues('type_s')[0] == 'File'
        sidSolrxml.getField(SolrSaver.FLD_NAME_SIZE_S).value == 'solrconfig.xml:62263'

        xmlName == 'solrconfig.xml'
    }


/*
    def "folder load with toSolrInputDocument"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())

        when:
        FolderFS folder = new FolderFS(src, 1, ignoreFiles, ignoreFolders)
        List<SolrInputDocument> solrDocs = folder.toSolrInputDocumentList()

        then:
        folder.filesFS.size()== 20    //14
        solrDocs.size() == 21
        folder.subDirectories.size()==1
        folder.ignoredFolders.size()==0
        folder.ignoredDirectories.size()==1
        folder.ignoredFiles.size()==1
    }
*/


    def "load and analyze folder"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())
        ConfigObject config = new ConfigSlurper().parse(getClass().getResource('/configLocate.groovy'))
        FolderAnalyzer analyzer = new FolderAnalyzer(config)
        FolderFS startFolder = new FolderFS(src, 1, ignoreFiles, ignoreFolders)

        when:
        def foo = startFolder.analyze(analyzer)
        Map assignmentGroupedFiles = startFolder.filesFS.groupBy { it.assignedTypes[0] }
        List keys = assignmentGroupedFiles.keySet().toList()

        then:
        keys.size == 8
        keys == ['techDev', 'archive', 'office', 'config', 'instructions', 'data', 'control', 'media']
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
        // todo -- complete me...
        int foo = 2

        then:
        foo == 2

    }

    def "crawl user docs local folders"() {
        given:
        long start = System.currentTimeMillis()
        long maxMem = Runtime.runtime.maxMemory()
        long totalMem = Runtime.runtime.totalMemory()
        println("Starting memory: $maxMem -- $totalMem")

        File rootFolder = new File('/home/sean/')
        List<File> folders = []
        List<FolderFS> folderFSList = []

        when:
        rootFolder.eachDirRecurse {
            folders << it
            FolderFS folderFS = new FolderFS(it)

            folderFSList << folderFS
//            println(it)
        }

        long maxMem2 = Runtime.runtime.maxMemory()
        long totalMem2 = Runtime.runtime.totalMemory()
        println("after memory: $maxMem2 -- $totalMem2")
        println("difference memory: ${maxMem2 - maxMem} -- ${totalMem2 - totalMem}")

        long end = System.currentTimeMillis()
        long elapsed = end - start
        println "Elapsed time: ${elapsed}ms (${elapsed/1000} sec)  -- ${folders.size()}"

        then:
        folders.size() > 10
    }
}
