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

    String jsonName = 'params.json'
    String zipName = 'datasources.zip'
    String tarName = 'combinedTestContent.tgz'

    def "should create basic FSFile"() {
        given:
        File jsonFile = new File(getClass().getResource("/content/${jsonName}").toURI())

        when:
        FSFile fsFile = new FSFile(jsonFile, locationName, crawlName, 1)

        then:
        fsFile.name == jsonName
        fsFile.type == FSFile.TYPE
        fsFile.thing instanceof File
        fsFile.thing == jsonFile

    }

    def "should create SolrInputDocument from zipfile"() {
        given:
        File zip = new File(getClass().getResource("/content/${zipName}").toURI())
        ArchiveInputStream aiszip = ArchiveUtils.getArchiveInputStream(zip)

        when:
        def zipEntries = ArchiveUtils.gatherArchiveEntries(aiszip)
        aiszip?.close()
        List<SolrInputDocument> sidList = []
        zipEntries.each { ArchiveEntry ae ->
            if (ae.isDirectory()) {
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
        boolean archiveFile = ArchiveUtils.isArchiveFileByExtension(zip)

        then:
        archiveFile==true
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
        File tarball = new File(getClass().getResource("/content/${tarName}").toURI())
        ArchiveInputStream aiszip = ArchiveUtils.getArchiveInputStream(tarball)

        when:
        def archiveEntries = ArchiveUtils.gatherArchiveEntries(aiszip)
        aiszip?.close()
        List<SolrInputDocument> sidList = []
        archiveEntries.each { ArchiveEntry ae ->
            if (ae.isDirectory()) {
                ArchFolder archiveFolder = new ArchFolder(ae, locationName, crawlName)
                SolrInputDocument sid = fsFolder.toSolrInputDocument()
                sidList << sid
            } else {
                ArchFile archiveFile = new ArchFile(ae, locationName, crawlName)
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
