package com.oconeco.models

import spock.lang.Specification

class SavableObjectTest extends Specification {
    String locationName = 'spock'
    String crawlName = 'test'

    def "check  constructor with args"() {
        given:
        File srcFolder = new File(getClass().getResource('/content').toURI())

        when:
        SavableObject object = new SavableObject(srcFolder, null, locationName, crawlName)

        then:
        object !=null
    }

}
