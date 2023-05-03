package com.oconeco.helpers

import spock.lang.Specification

class SolrCrawlArgParserTest extends Specification {
    def "GetOptions from constructor"() {
        given:
        URL cfgUrl = this.class.getClassLoader().getResource('configs/configLocate.groovy')

        when:
        String[] args = ['--config=./resources/configs/configLocate.groovy',
        '-f/home/sean/Documents,/home/sean/Desktop',
        '-n"Locate Documents"',
        '-w']

        SolrCrawlArgsParser scc = new SolrCrawlArgsParser(this.class.name, args)

        then:
        scc.options != null
        scc.config !=null

    }

//    def "SetConfig"() {
//    }
}
