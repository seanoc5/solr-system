package com.oconeco.helpers

import spock.lang.Specification

class SolrCrawlArgParserTest extends Specification {
    def "GetOptions from constructor"() {
        given:
        URL cfgUrl = this.class.getClassLoader().getResource('configs/configLocate.groovy')

        when:
        String[] args = ['--config=./resources/configs/configLocate.groovy',
        '-f/home/sean/Documents,/home/sean/Desktop',
        '-n"SpockTest"',
        '-w']

        def config = SolrCrawlArgParser.parse(this.class.name, args)

        then:
        config != null
        config.name == 'SpockTest'

    }

//    def "SetConfig"() {
//    }
}
