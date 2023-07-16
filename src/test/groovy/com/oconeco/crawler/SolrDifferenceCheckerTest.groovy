package com.oconeco.crawler

import com.oconeco.difference.BaseDifferenceChecker
import com.oconeco.difference.SolrDifferenceChecker
import com.oconeco.difference.BaseDifferenceStatus
import com.oconeco.models.FSFolder
import com.oconeco.persistence.SolrSystemClient
import org.apache.solr.common.SolrDocument
import spock.lang.Specification

class SolrDifferenceCheckerTest extends Specification {
    String locationName = 'spock'
    String crawlName = 'test'

    File startFolder = new File(getClass().getResource('/content').toURI())
    FSFolder fsFolder = new FSFolder(startFolder, null, locationName, crawlName)

    def "check when objects should be current"() {
        given:
        SolrDocument sameSolrDoc = new SolrDocument()
        sameSolrDoc.setField(SolrSystemClient.FLD_ID, fsFolder.id)
        sameSolrDoc.setField(SolrSystemClient.FLD_TYPE, fsFolder.type)
        sameSolrDoc.setField(SolrSystemClient.FLD_LAST_MODIFIED, fsFolder.lastModifiedDate)
        sameSolrDoc.setField(SolrSystemClient.FLD_PATH_S, fsFolder.path)
        sameSolrDoc.setField(SolrSystemClient.FLD_SIZE, fsFolder.size)
        sameSolrDoc.setField(SolrSystemClient.FLD_DEDUP, fsFolder.dedup)
        sameSolrDoc.setField(SolrSystemClient.FLD_LOCATION_NAME, fsFolder.locationName)

        SolrDifferenceChecker solrChecker = new SolrDifferenceChecker()

        when:
        BaseDifferenceStatus solrStatus = new BaseDifferenceStatus(fsFolder, sameSolrDoc)
        BaseDifferenceStatus status = solrChecker.compareCrawledGroupToSavedGroup(fsFolder, sameSolrDoc, solrStatus)

        then:
        status != null
        status.differentIds == false
        status.differentDedups == false
        status.differentLastModifieds == false
        status.differentLocations == false
        status.differentPaths == false
        status.differentSizes == false
        solrChecker.shouldUpdate(status) == false
    }


    def "check when objects are different"() {
        given:
        SolrDocument diffSolrDoc = new SolrDocument()
        diffSolrDoc.setField(SolrSystemClient.FLD_ID, fsFolder.id + '!')
        diffSolrDoc.setField(SolrSystemClient.FLD_TYPE, fsFolder.type + '!')
        diffSolrDoc.setField(SolrSystemClient.FLD_LAST_MODIFIED, new Date())
//        diffSolrDoc.setField(SolrSystemClient.Fld_, fsFolder.id)
        diffSolrDoc.setField(SolrSystemClient.FLD_PATH_S, fsFolder.path + '!')
        diffSolrDoc.setField(SolrSystemClient.FLD_SIZE, fsFolder.size + 1)
        diffSolrDoc.setField(SolrSystemClient.FLD_DEDUP, fsFolder.dedup + '!')
        diffSolrDoc.setField(SolrSystemClient.FLD_LOCATION_NAME, fsFolder.locationName + '!')
        BaseDifferenceChecker differenceChecker = new BaseDifferenceChecker()

        when:
        BaseDifferenceStatus status = differenceChecker.compareCrawledGroupToSavedGroup(fsFolder, diffSolrDoc, new BaseDifferenceStatus(fsFolder, diffSolrDoc))

        then:
        status != null
        status.differentIds == true
        status.differentDedups == true
        status.differentLastModifieds == true
        status.differentLocations == true
        status.differentPaths == true
        status.differentSizes == true
        differenceChecker.shouldUpdate(status) == true

    }
}
