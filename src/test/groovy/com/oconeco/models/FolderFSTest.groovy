package com.oconeco.models

import com.oconeco.analysis.FolderAnalyzer
import org.apache.solr.common.SolrInputDocument
import spock.lang.Specification

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

class FolderFSTest extends Specification {
    def "basic folder load"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())

        when:
        FolderFS folder = new FolderFS(src, 1)

        then:
        folder.size == 195194
        folder.countTotal == 20

        folder.filesFS.size() == 18
        folder.countFiles == 18

        folder.childFolders.size() == 2
        folder.countSubdirs == 2
    }


    def "basic folder load with ignore file and folder patterns"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())

        when:
        FolderFS folder = new FolderFS(src, 1, ~/.*\.(pem|go|xml)$/, ~/(ignoreMe|ignoreYou)/)

        then:
        folder.size == 126373
        folder.countTotal == 15

        folder.filesFS.size() == 14
        folder.countFiles == 14

        folder.childFolders.size() == 1
        folder.countSubdirs == 1

        folder.ignoredFolders.size() == 1
    }

    def "folder load with toSolrInputDocument"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())

        when:
        FolderFS folder = new FolderFS(src, 1, ~/.*\.(pem|go|xml)$/, ~/(ignoreMe|subFolder2)/)
        List<SolrInputDocument> solrDocs = folder.toSolrInputDocumentList()

        then:
        folder.filesFS.size()==14
        solrDocs.size() == 15
        folder.childFolders.size()==1
        folder.ignoredFolders.size()==1
        folder.ignoredFiles.size()==4
    }

    def "load and analyze folder"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())
        ConfigObject config = new ConfigSlurper().parse(getClass().getResource('/configLocate.groovy'))
        FolderAnalyzer analyzer = new FolderAnalyzer(config)
        FolderFS folder = new FolderFS(src, 1)

        when:
        def foo = folder.analyze(analyzer)
        Map assignmentGroupedFiles = folder.filesFS.groupBy {it.assignedTypes[0]}
        List keys = assignmentGroupedFiles.keySet().toList()

        then:
        keys.size == 7
        keys == ['archive', 'techDev', 'office', 'config', 'instructions', 'data', 'media']

    }

}
