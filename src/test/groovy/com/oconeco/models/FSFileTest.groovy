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

    File jsonFile = new File(getClass().getResource("/content/${jsonName}").toURI())
    File zipFile = new File(getClass().getResource("/content/${zipName}").toURI())
    File tarFile = new File(getClass().getResource("/content/${tarName}").toURI())

    def "should create basic FSFile - non-archive"() {
        given:

        when:
        FSFile fsFile = new FSFile(jsonFile, locationName, crawlName, 1)

        then:
        fsFile.id == fsFile.locationName + ':' + fsFile.path
        fsFile.name == jsonName
        fsFile.path.endsWith(jsonFile.name)
        fsFile.type == FSFile.TYPE
        fsFile.thing instanceof File
        fsFile.thing == jsonFile
        fsFile.isArchive() ==false
    }


    def "should create basic FSFiles from archive source "() {
        when:
        FSFile zipFsFile = new FSFile(zipFile, locationName, crawlName, 1)
        FSFile tarFsFile = new FSFile(tarFile, locationName, crawlName, 1)

        then:
        zipFsFile.id == zipFsFile.locationName + ':' + zipFsFile.path
        zipFsFile.name == zipName
        zipFsFile.path.endsWith(zipName.name)
        zipFsFile.type == FSFile.TYPE
        zipFsFile.thing instanceof File
        zipFsFile.thing == zipFile
        zipFsFile.isArchive() ==true
    }

    // todo - move this logic elsewhere
/*
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
*/

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
                ArchFolder archFolder = new ArchFolder(ae, locationName, crawlName)
                SolrInputDocument sid = archFolder.toSolrInputDocument()
                sidList << sid
            } else {
                ArchFile archFile = new ArchFile(ae, locationName, crawlName)
                SolrInputDocument sid = archFile.toSolrInputDocument()
                sidList << sid
            }
        }
        SolrInputDocument firstSID = sidList[0]
        String firstPath = firstSID.getFieldValue(SolrSaver.FLD_PATH_S)


        then:
        archiveEntries != null
        sidList.size() == 40
        firstPath!=null
        firstSID.getFieldValue(SolrSaver.FLD_ID) == "${locationName}:${firstPath}"
        firstSID.getFieldValue(SolrSaver.FLD_TYPE) == 'ArchiveEntry'
        firstSID.getFieldValue(SolrSaver.FLD_SIZE) == 72904
        firstSID.getFieldValue(SolrSaver.FLD_CRAWL_NAME) == 'test'
        firstSID.getFieldValue(SolrSaver.FLD_EXTENSION_SS) == 'json'
        firstSID.getFieldValue(SolrSaver.FLD_NAME_SIZE_S) == 'objects.json:72904'
    }


}
