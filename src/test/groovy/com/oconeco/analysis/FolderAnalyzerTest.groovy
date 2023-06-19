package com.oconeco.analysis

import com.oconeco.helpers.Constants
import com.oconeco.models.FSFolder
import spock.lang.Specification

import java.util.regex.Pattern

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

// todo -- remove? Moved responsibility to Object to use Analyzer as a helper, rather than the analyzer driving
class FolderAnalyzerTest extends Specification {
    String locationName = 'spock'
    String crawlName = 'test'
    URL cfgUrl = getClass().getResource('/configLocate.groovy')
    ConfigObject config = new ConfigSlurper().parse(cfgUrl)
    FolderAnalyzer analyzer = new FolderAnalyzer(config)
    File folder = new File(getClass().getResource('/content').toURI())
    Pattern ignoreFiles = Constants.DEFAULT_FILENAME_PATTERNS[Constants.LBL_IGNORE]
    Pattern ignoreFolders = Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE]


    def "basic folder type analysis"() {
        when:
        FSFolder fsFolder = new FSFolder(folder, locationName, crawlName, ignoreFolders, ignoreFiles, 1)
        List<String> labels = analyzer.analyze(fsFolder)

        then:
        labels != null
        labels.size() == 1
        labels[0] == 'content'
    }

/*
    def "folder.toSolrInputDocument analysis"() {
        given:
        ConfigObject config = new ConfigSlurper().parse(getClass().getResource('/configLocate.groovy'))
        FSFolder folderFS = new FSFolder(new File(getClass().getResource('/content').toURI()))
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
*/

}
