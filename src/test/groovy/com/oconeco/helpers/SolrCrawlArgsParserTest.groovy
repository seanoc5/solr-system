package com.oconeco.helpers

import spock.lang.Specification

class SolrCrawlArgsParserTest extends Specification {
    def "GetOptions"() {
        given:
        URL cfgUrl = this.class.getClassLoader().getResource('configs/configLocate.groovy')

        when:
        String[] args = ['--config=./resources/configs/configLocate.groovy',
        '-f/home/sean/Documents,/home/sean/Desktop',
        '-n"Locate Documents"',
        '-w']

        SolrCrawlArgsParser scc = new SolrCrawlArgsParser(this.class.name, args)

        List files = scc.options.fs

        then:
        scc.options != null
        scc.config !=null
        files.size()==2

    }

    def "GetConfig"() {
    }
}
