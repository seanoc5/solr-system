package com.oconeco.models

import spock.lang.Specification

class SavableObjectTest extends Specification {

//    def "check empty constructor"() {
//        when:
//        SavableObject object = new SavableObject()
//
//        then:
//        object !=null
//    }

    def "check basic constructor"() {
        given:
        File srcFolder = new File(getClass().getResource('/content').toURI())

        when:
        SavableObject object = new SavableObject(srcFolder)

        then:
        object !=null
    }

}
