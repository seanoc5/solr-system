package com.oconeco.models

import com.oconeco.helpers.ArchiveUtils
import com.oconeco.persistence.SolrSaver
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.solr.common.SolrInputDocument
import spock.lang.Specification

class FSFileTest extends Specification {
    String locationName = 'spock'
    String crawlName = 'test'
    String paramsJsonName = 'params.json'
    String zipName = 'datasources.zip'
    String tarName = 'combinedTestContent.tgz'

    def "should create basic FSFile"() {
        given:
        File testFile = new File(getClass().getResource("/content/${paramsJsonName}").toURI())

        when:
        FSFile fsFile = new FSFile(testFile, locationName, crawlName, 1)

        then:
        fsFile.name == paramsJsonName
        fsFile.type == FSFile.TYPE
        fsFile.thing instanceof File
        fsFile.thing== testFile

    }

    def "should create SolrInputDocument from zipfile"() {
        given:
        def zip = new File(getClass().getResource("/content/${zipName}").toURI())
        ArchiveInputStream aiszip = ArchiveUtils.getArchiveInputStream(zip)

        when:
        def zipEntries = ArchiveUtils.gatherArchiveEntries(aiszip)
        aiszip?.close()
        List<SolrInputDocument> sidList = []
        zipEntries.each { ArchiveEntry ae ->
            if(ae.isDirectory()) {
                FSFolder fsFolder = new FSFolder(ae, locationName, crawlName)
                SolrInputDocument sid = fsFolder.toSolrInputDocument()
                sidList << sid
            } else {
                FSFile fsFile = new FSFile(ae, locationName, crawlName)
                SolrInputDocument sid = fsFile.toSolrInputDocument()
                sidList << sid
            }
        }
        SolrInputDocument firstSID = sidList[0]


        then:
        zipEntries != null
        sidList.size() == 2
        firstSID.getFieldValue(SolrSaver.FLD_ID) == "${locationName}:objects.json"
        firstSID.getFieldValue(SolrSaver.FLD_TYPE) == 'ArchiveEntry'
        firstSID.getFieldValue(SolrSaver.FLD_SIZE) == 72904
        firstSID.getFieldValue(SolrSaver.FLD_CRAWL_NAME) == 'test'
        firstSID.getFieldValue(SolrSaver.FLD_EXTENSION_SS) == 'json'
        firstSID.getFieldValue(SolrSaver.FLD_NAME_SIZE_S) == 'objects.json:72904'
    }

    def "should create SolrInputDocument from tarball"() {
        given:
        def zip = new File(getClass().getResource("/content/${tarName}").toURI())
        ArchiveInputStream aiszip = ArchiveUtils.getArchiveInputStream(zip)

        when:
        def archiveEntries = ArchiveUtils.gatherArchiveEntries(aiszip)
        aiszip?.close()
        List<SolrInputDocument> sidList = []
        archiveEntries.each { ArchiveEntry ae ->
            if(ae.isDirectory()) {
                ArchiveFolder archiveFolder = new ArchiveFolder(ae, locationName, crawlName)
                SolrInputDocument sid = fsFolder.toSolrInputDocument()
                sidList << sid
            } else {
                ArchiveFile archiveFile = new ArchiveFile(ae, locationName, crawlName)
                SolrInputDocument sid = fsFile.toSolrInputDocument()
                sidList << sid
            }
        }
        SolrInputDocument firstSID = sidList[0]


        then:
        archiveEntries != null
        sidList.size() == 2
        firstSID.getFieldValue(SolrSaver.FLD_ID) == "${locationName}:objects.json"
        firstSID.getFieldValue(SolrSaver.FLD_TYPE) == 'ArchiveEntry'
        firstSID.getFieldValue(SolrSaver.FLD_SIZE) == 72904
        firstSID.getFieldValue(SolrSaver.FLD_CRAWL_NAME) == 'test'
        firstSID.getFieldValue(SolrSaver.FLD_EXTENSION_SS) == 'json'
        firstSID.getFieldValue(SolrSaver.FLD_NAME_SIZE_S) == 'objects.json:72904'
    }


}
