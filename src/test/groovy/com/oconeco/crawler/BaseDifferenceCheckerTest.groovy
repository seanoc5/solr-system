package com.oconeco.crawler

import com.oconeco.helpers.Constants
import com.oconeco.models.FSFolder
import com.oconeco.persistence.SolrSystemClient
import org.apache.solr.common.SolrDocument
import spock.lang.Specification

import java.util.regex.Pattern

class BaseDifferenceCheckerTest extends Specification {
    String locationName = 'spock'
    String crawlName = 'test'
    Pattern ignoreFiles = Constants.DEFAULT_FILENAME_PATTERNS[Constants.LBL_IGNORE]
    Pattern ignoreFolders = Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE]


    File startFolder = new File(getClass().getResource('/content').toURI())
    FSFolder fsFolder = new FSFolder(startFolder, null, locationName, crawlName)
//    Map<String, Object> fieldsMap = ["${SolrSystemClient.FLD_ID}": fsFolder.id, "${SolrSystemClient.FLD_TYPE}": "${FSFolder.TYPE}", "${SolrSystemClient.FLD_PATH_S}": '/content', "${SolrSystemClient.FLD_LAST_MODIFIED}": fsFolder.lastModifiedDate]
//    SolrDocument solrDoc = new SolrDocument(fieldsMap)

    def "check when objects should be current"() {
        given:
        BaseDifferenceChecker differenceChecker = new BaseDifferenceChecker()
        SolrDocument sameSolrDoc = new SolrDocument()
        sameSolrDoc.setField(SolrSystemClient.FLD_ID, fsFolder.id)
        sameSolrDoc.setField(SolrSystemClient.FLD_TYPE, fsFolder.type)
        sameSolrDoc.setField(SolrSystemClient.FLD_LAST_MODIFIED, fsFolder.lastModifiedDate)
        sameSolrDoc.setField(SolrSystemClient.FLD_PATH_S, fsFolder.path)
        sameSolrDoc.setField(SolrSystemClient.FLD_SIZE, fsFolder.size)
        sameSolrDoc.setField(SolrSystemClient.FLD_DEDUP, fsFolder.dedup)
        sameSolrDoc.setField(SolrSystemClient.FLD_LOCATION_NAME, fsFolder.locationName)
//        sameSolrDoc.setField(SolrSystemClient.FLD_, fsFolder.)

        when:
        DifferenceStatus status = differenceChecker.compareFSFolderToSavedDoc(fsFolder, sameSolrDoc)
//        def diff = differenceChecker.folderNeedsUpdate()

        then:
        status != null
        status.differentIds == false
        status.differentDedups == false
        status.differentLastModifieds == false
        status.differentLocations == false
        status.differentPaths == false
        status.differentSizes == false
        differenceChecker.shouldUpdate(status) == false
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
        DifferenceStatus status = differenceChecker.compareFSFolderToSavedDoc(fsFolder, diffSolrDoc)
//        def diff = differenceChecker.folderNeedsUpdate()

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
