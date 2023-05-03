package com.oconeco.analysis

import com.oconeco.models.FolderFS
import org.apache.solr.common.SolrInputDocument
import spock.lang.Specification

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

// todo -- remove? Moved responsibility to Object to use Analyzer as a helper, rather than the analyzer driving
class FolderAnalyzerTest extends Specification {

    def "basic folder type analysis"() {
        given:
        URL cfgUrl = getClass().getResource('/configLocate.groovy')
        ConfigSlurper slurper = new ConfigSlurper()
        ConfigObject config = slurper.parse(cfgUrl)

        FolderAnalyzer analyzer = new FolderAnalyzer(config)

        File folder = new File(getClass().getResource('/content').toURI())
        FolderFS folderFS = new FolderFS(folder)

        when:
        List<String> assignedLabels = folderFS.analyze(analyzer)
        Map groupedLabels = assignedLabels.groupBy { it }
        List contentGroup = groupedLabels['content']

        then:
        folderFS.assignedTypes != null
        folderFS.assignedTypes.size() == 1
        folderFS.assignedTypes[0] == 'content'

        folderFS.childAssignedTypes.size() >= 19
        groupedLabels.size() >= 9
        groupedLabels.keySet()[0] == 'archive'
        groupedLabels['archive'].size() == 4

    }

    def "folder.toSolrInputDocument analysis"() {
        given:
        ConfigObject config = new ConfigSlurper().parse(getClass().getResource('/configLocate.groovy'))
        FolderFS folderFS = new FolderFS(new File(getClass().getResource('/content').toURI()))
        FolderAnalyzer analyzer = new FolderAnalyzer(config)
        List<String> labels = folderFS.analyze(analyzer)

        when:
        List<SolrInputDocument> solrdocs = folderFS.toSolrInputDocumentList()

        then:
//        solrdocs.size() == 19   // brittle testing?
        solrdocs[0].getField('type_s').value == 'Folder'
        solrdocs[0].getField('name_s').value == 'content'

        solrdocs[1].getField('type_s').value == 'File'
        solrdocs[1].getField('name_s').value == 'compressedchart-0.1.0.tar.gz'
        solrdocs[1].getFieldValues('assignedTypes_ss').size() == 1
        solrdocs[1].getFieldValues('assignedTypes_ss') == ['archive']


    }

}
