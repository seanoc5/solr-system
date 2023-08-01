package com.oconeco.persistence

import org.apache.solr.client.solrj.response.SolrPingResponse
import org.apache.solr.common.util.NamedList
import spock.lang.Specification

class SolrSystemClientTest extends Specification {
    SolrSystemClient solrSystemClient = new SolrSystemClient()
    String testName = 'foo1'


    def "BuildSolrClient"() {
        when:
//        def  solrClient = solrSystemClient.solrClient
        SolrPingResponse pingResp =solrSystemClient.ping()

        then:
        pingResp.status == 0
        NamedList<Object> respResp = pingResp.getResponse()
        respResp.get('status') == 'OK'
    }

    def "GetClusterStatus"() {
//        given:
//        SolrSystemClient client = new SolrSystemClient()

        when:
        LinkedHashMap<String, Iterable<? extends Object>> statusMap = solrSystemClient.getClusterStatus()

        then:
        statusMap!=null
        statusMap.response != null
        statusMap.liveNodes.size() > 0
    }

    def "GetSchemaInformation"() {
        when:
        def schema = solrSystemClient.getSchemaInformation('solr_system')

        then:
        schema!=null
    }

    def "CreateCollection"() {
        given:

        when:
        def newColl = solrSystemClient.createCollection(testName, 1,1)
        def delResponse = solrSystemClient.deleteCollection(testName)
        def collList = solrSystemClient.getCollections()

        then:
        newColl!=null
        delResponse!=null

    }

    def "CreateConfigSet"() {
        when:
        def configSet = solrSystemClient.createConfigSet(testName)

        then:
        configSet!=null
    }
}
