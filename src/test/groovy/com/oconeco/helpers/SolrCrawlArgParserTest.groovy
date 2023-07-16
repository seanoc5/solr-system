package com.oconeco.helpers

import spock.lang.Specification

import java.util.regex.Pattern

class SolrCrawlArgParserTest extends Specification {
    String locationName = 'Spock'
    def "GetOptions from constructor"() {
        given:
        URL cfgUrl = this.class.getClassLoader().getResource('configs/configLocate.groovy')
        File cfgFile = new File(cfgUrl.toURI())
        String filePatternLabels = "ignore index analyze office instructions techDev config control data media logs archive compressed web system"
        List<String> fplList = filePatternLabels.split(' ')

        when:
        String[] args = ['--config=./configs/configLocate.groovy',
                         '-fTestDocs:/home/sean/Documents,testDesktop:/home/sean/Desktop',
                         '-n"SpockTest"',
                         '-w']


        def config = SolrCrawlArgParser.parse(this.class.name, args)
        Map<String, Pattern> fnameMap = config.namePatterns.files

        then:
        cfgFile.exists()
        cfgFile.canRead()

        config != null
        fnameMap.keySet().containsAll(['ignore', 'default'])
    }

}
