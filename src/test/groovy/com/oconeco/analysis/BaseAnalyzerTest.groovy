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

    URL cfgUrl = getClass().getResource('/configLocate.groovy')
    ConfigObject config = new ConfigSlurper().parse(cfgUrl)

    BaseAnalyzer analyzer = new BaseAnalyzer(Constants.DEFAULT_FOLDERNAME_LOCATE, Constants.DEFAULT_FILENAME_LOCATE)

    File paramsJson = new File(FileAnalyzerTest.getClassLoader().getResource('./content/params.json').toURI())
    File currencyXml = new File(FileAnalyzerTest.getClassLoader().getResource('./content/currency.xml').toURI())
    File skipme = new File(FileAnalyzerTest.getClassLoader().getResource('./content/skipme.tmp').toURI())

    FSFolder parentFolder = new FSFolder(paramsJson.parentFile, null, locationName, crawlName)

    FSFile paramFS = new FSFile(paramsJson, parentFolder, locationName, crawlName)
    FSFile currencyFS = new FSFile(currencyXml, parentFolder, locationName, crawlName)
    FSFile skipmeFS = new FSFile(skipme, parentFolder, locationName, crawlName)


    def "Analyze 3 test files"() {
        when:
        def resultParam = analyzer.analyze(paramFS)
        String basicKey = 'basic'
        def resultCurrency = analyzer.analyze(currencyFS)
        def resultSkipme = analyzer.analyze(skipmeFS)

        then:
        resultParam != null
        resultParam instanceof List
        resultParam.size() == 1
        resultParam[0] instanceof Map<String, Map<String, Object>>
        resultParam[0].keySet().contains(basicKey)
        resultParam[0].get(basicKey).keySet().toList()==['pattern', 'analysis']
        resultParam[0].get(basicKey).get('pattern') !=null
        resultParam[0].get(basicKey).get('analysis') == ['track', 'parse']

        resultCurrency.size() == 1

        resultSkipme != null

        paramFS.labels != null
        paramFS.labels.size()==1
        paramFS.labels[0] == basicKey          // todo - change this to last label without a pattern
        currencyFS.labels[0] == 'default'
        skipmeFS.labels.size()==1
        skipmeFS.labels[0]=='ignore'
    }

    def "Analyze list of files"() {
        when:
        List< SavableObject > fileFsList = [paramFS, currencyFS, skipmeFS]
        def results = analyzer.analyze(fileFsList)

        then:
        results != null
        results instanceof List
        results.size() == 3
    }

    def "TestAnalyze"() {
    }
}
