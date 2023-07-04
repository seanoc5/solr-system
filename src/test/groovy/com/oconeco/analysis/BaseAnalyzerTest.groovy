package com.oconeco.analysis

import com.oconeco.helpers.Constants
import com.oconeco.models.FSFile
import com.oconeco.models.FSFolder
import com.oconeco.models.SavableObject
import spock.lang.Specification


/**
 * todo -- shift from testing FSOb
 */
class BaseAnalyzerTest extends Specification {
    String locationName = 'spock'
    String crawlName = 'test'

//    URL cfgUrl = getClass().getResource('/configLocate.groovy')
//    ConfigObject config = new ConfigSlurper().parse(cfgUrl)

    File paramsJson = new File(FileAnalyzerTest.getClassLoader().getResource('./content/params.json').toURI())
    File currencyXml = new File(FileAnalyzerTest.getClassLoader().getResource('./content/currency.xml').toURI())
    File skipme = new File(FileAnalyzerTest.getClassLoader().getResource('./content/skipme.tmp').toURI())

    FSFolder parentFolder = new FSFolder(paramsJson.parentFile, null, locationName, crawlName)

    FSFile paramFS = new FSFile(paramsJson, parentFolder, locationName, crawlName)
    FSFile currencyFS = new FSFile(currencyXml, parentFolder, locationName, crawlName)
    FSFile skipmeFS = new FSFile(skipme, parentFolder, locationName, crawlName)

    Map<String, Map<String, Object>>  group = Constants.DEFAULT_FOLDERNAME_LOCATE.clone()
    Map<String, Map<String, Object>>  item = Constants.DEFAULT_FILENAME_LOCATE.clone()


    def "empty constructor"() {
        when:
        BaseAnalyzer analyzer = new BaseAnalyzer()

        then:
        analyzer != null
        analyzer.ignoreGroup != null
        analyzer.ignoreItem != null
        analyzer.itemNameMap.keySet().size() == 2
        analyzer.groupNameMap.keySet().size() == 2
        analyzer.groupPathMap == null
        analyzer.itemPathMap == null
    }

    def "constructor with basic params"() {
        when:
        BaseAnalyzer analyzer = new BaseAnalyzer(group, item)

        then:
        analyzer != null
        analyzer.ignoreGroup != null
        analyzer.ignoreItem != null
        analyzer.itemNameMap.keySet().size() == 2
        analyzer.groupNameMap.keySet().size() == 2
        analyzer.groupPathMap == null
        analyzer.itemPathMap == null
    }

    def "constructor with folder and file names AND paths maps"() {
        when:
        BaseAnalyzer analyzer = new BaseAnalyzer(group, item, Constants.DEFAULT_FOLDERPATH_MAP, Constants.DEFAULT_FILEPATH_MAP)

        then:
        analyzer != null
        analyzer.ignoreGroup != null
        analyzer.ignoreItem != null
        analyzer.itemNameMap.keySet().size() == 2
        analyzer.groupNameMap.keySet().size() == 2
        analyzer.groupPathMap == Constants.DEFAULT_FOLDERPATH_MAP
        analyzer.itemPathMap == Constants.DEFAULT_FILEPATH_MAP
    }

    def "Analyze 3 test files"() {
        given:
        BaseAnalyzer analyzerLocate = new BaseAnalyzer(Constants.DEFAULT_FOLDERNAME_LOCATE, Constants.DEFAULT_FILENAME_LOCATE)

        when:
        def resultSkipme = analyzerLocate.analyze(skipmeFS)
        def resultParam = analyzerLocate.analyze(paramFS)
        def resultCurrency = analyzerLocate.analyze(currencyFS)

        String parseKey = 'parse'

        then:
        resultParam != null
        resultParam instanceof Map<Map<String, Object>>
        resultParam.size() == 1
        resultParam instanceof   Map<String, Map<String, Object>>
//        (resultParam.get(basicKey) insta)
        resultParam.get(parseKey).keySet().toList().containsAll(['pattern', 'analysis', 'LabelMatch'])
        resultParam.get(parseKey).get('pattern') != null

        resultCurrency.size() == 1

        resultSkipme.size() == 1
        resultSkipme.ignore.size() == 3
        resultSkipme.ignore.pattern ==''
        resultSkipme.ignore.analysis.size() ==0
        resultSkipme.ignore.get(Constants.LABEL_MATCH) == Constants.NO_MATCH


        paramFS.labels != null
        paramFS.labels.size() == 1
        paramFS.labels[0] == parseKey          // todo - change this to last label without a pattern

        currencyFS.labels[0] == Constants.TRACK

        skipmeFS.labels.size() == 1
        skipmeFS.ignore == true
    }


    def "Analyze list of files"() {
        given:
        BaseAnalyzer analyzerLocate = new BaseAnalyzer(Constants.DEFAULT_FOLDERNAME_LOCATE, Constants.DEFAULT_FILENAME_LOCATE)
        List<SavableObject> fileFsList = [paramFS, currencyFS, skipmeFS]

        when:
        def results = analyzerLocate.analyze(fileFsList)

        then:
        results != null
        results instanceof List
        results.size() == 3
    }

    def "check doAnalysis calls"(){
        given:
        BaseAnalyzer analyzerLocate = new BaseAnalyzer(Constants.DEFAULT_FOLDERNAME_LOCATE, Constants.DEFAULT_FILENAME_LOCATE)
        List<SavableObject> fileFsList = [paramFS, currencyFS, skipmeFS]

        when:
        def results = analyzerLocate.analyze(fileFsList)

        then:
        results != null

    }

//    def "TestAnalyze"() {
//    }
}
