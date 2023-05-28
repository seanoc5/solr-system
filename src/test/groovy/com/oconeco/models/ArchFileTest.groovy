package com.oconeco.models

import com.oconeco.helpers.ArchiveUtils
import org.apache.commons.compress.archivers.ArchiveEntry
import spock.lang.Specification
// todo -- move this functionality to a constructor in the Crawler, and have the crawler identify archives, and handle them???

class ArchFileTest extends Specification {
    String locationName = 'spock'
    String crawlName = 'test'

    String tarName = 'combinedTestContent.tgz'
    String zipName = 'datasources.zip'

    File zipSrcFile = new File(getClass().getResource("/content/${zipName}").toURI())
    FSFile fsZipFile = new FSFile(zipSrcFile, locationName, crawlName, 1)
    File tarSrcFile = new File(getClass().getResource("/content/${tarName}").toURI())
    FSFile fstarFile = new FSFile(tarSrcFile, locationName, crawlName, 1)

    def "check basic constructor"() {

        when:
        List<ArchiveEntry> zipEntries = ArchiveUtils.gatherArchiveEntries(zipSrcFile)
        List<ArchiveEntry> tarEntries = ArchiveUtils.gatherArchiveEntries(tarSrcFile)

        then:
        zipEntries !=null
        zipEntries.size()==2
        zipEntries[0].name == 'objects.json'

        tarEntries !=null
        tarEntries.size()==40
        tarEntries[0].name=='compressedchart/Chart.yaml'


    }

/*
    def "ToSolrInputDocument"() {
        when:
        List<SolrInputDocument> zipSolrDocs = ArchiveUtils.gatherSolrInputDocs(zipSrcFile, locationName, crawlName, 0)
        List<SolrInputDocument> tarSolrDocs = ArchiveUtils.gatherSolrInputDocs(tarSrcFile, locationName, crawlName, 0)

        then:
        zipSolrDocs !=null
        tarSolrDocs !=null
    }
*/
}
