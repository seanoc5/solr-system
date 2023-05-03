//package com.oconeco.solr
//
//import org.apache.solr.SolrTestCaseJ4
//import spock.lang.Specification
///**
// * @author :    sean
// * @mailto :    seanoc5@gmail.com
// * @created :   8/28/22, Sunday
// * @description:
// */
//
//class EmbeddedSolrTest extends Specification, SolrTestCaseJ4 {
//    File solrFolder = new File(getClass().getResource('/solr').toURI())
//
//    //
//    def "simple embedded solr test"() {
//        given:
////        SolrTestCaseJ4.initCore("solrhome/conf/solrconfig.xml", "solrhome/conf/schema.xml", "solrhome/");
//        println "Solr home: $solrFolder"
//
//        when:
//        println "init core..."
//        SolrTestCaseJ4.initCore("solrconfig.xml", "schema.xml", solrFolder.path);
//
//        then:
//        false
//    }
//
//}
