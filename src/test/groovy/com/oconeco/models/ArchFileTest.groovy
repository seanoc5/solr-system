package com.oconeco.models

import com.oconeco.helpers.ArchiveUtils
import org.apache.solr.common.SolrInputDocument
import spock.lang.Specification

class ArchFileTest extends Specification {
    String locationName = 'spock'
    String crawlName = 'test'

    String tarName = 'combinedTestContent.tgz'
    String zipName = 'datasources.zip'

    File zipSrcFile = new File(getClass().getResource("/content/${zipName}").toURI())
    File tarSrcFile = new File(getClass().getResource("/content/${tarName}").toURI())

    def "check basic constructor"() {

        when:
        def zipEntries = ArchiveUtils.gatherArchiveEntries(zipSrcFile)
        def tarEntries = ArchiveUtils.gatherArchiveEntries(tarSrcFile)

        then:
        zipEntries !=null
        zipEntries.size()==2
        zipEntries[0].name == 'objects.json'

        tarEntries !=null
        tarEntries.size()==40
        tarEntries[0].name=='compressedchart/Chart.yaml'


    }

    def "ToSolrInputDocument"() {
        when:
        List<SolrInputDocument> zipSolrDocs = ArchiveUtils.gatherSolrInputDocs(zipSrcFile, locationName, crawlName, 0)
        List<SolrInputDocument> tarSolrDocs = ArchiveUtils.gatherSolrInputDocs(tarSrcFile, locationName, crawlName, 0)

        then:
        zipSolrDocs !=null
        tarSolrDocs !=null
    }
}
