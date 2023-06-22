package com.oconeco.crawler

import com.oconeco.analysis.BaseAnalyzer
import com.oconeco.helpers.Constants
import com.oconeco.persistence.BaseClient
import spock.lang.Specification

class SampleCrawlerTest extends Specification {
    String locationName = 'spock'
    String crawlName = 'test'

    URL cfgUrl = getClass().getResource('/configLocate.groovy')
    ConfigObject config = new ConfigSlurper().parse(cfgUrl)

    File contentFolder = new File(getClass().getResource('/content').toURI())
    BaseClient baseClient = new BaseClient()

    BaseAnalyzer analyzer = new BaseAnalyzer(Constants.DEFAULT_FOLDERNAME_LOCATE, Constants.DEFAULT_FILENAME_LOCATE)
    SampleCrawler crawler = new SampleCrawler('', baseClient, new BaseDifferenceChecker(),)

    def "GetSavedDocCount"() {
    }

    def "CrawlGrouping"() {
    }

    def "GetRelativeDepth"() {
    }
}
