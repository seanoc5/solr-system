package com.oconeco.models


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
    FSFolder parentFolder = new FSFolder(jsonFile.parentFile, locationName, crawlName, 0)

    def "should create basic FSFile - non-archive"() {
        given:

        when:
        FSFile fsFile = new FSFile(jsonFile, parentFolder, locationName, crawlName)

        then:
        fsFile.id == fsFile.locationName + ':' + fsFile.path
        fsFile.name == jsonName
        fsFile.path.endsWith(jsonFile.name)
        fsFile.type == FSFile.TYPE
        fsFile.thing instanceof File
        fsFile.thing == jsonFile
        fsFile.isArchive() == false
    }


    def "should create basic FSFiles from archive source "() {
        when:
        FSFile zipFsFile = new FSFile(zipFile, parentFolder, locationName, crawlName)
//        FSFile tarFsFile = new FSFile(tarFile, parentFolder, locationName, crawlName)
        List<SavableObject> children = zipFsFile.gatherArchiveEntries()
        List<String> childNames = children.collect { it.name }

        then:
        children != null
        children.size() == 2
        childNames.containsAll(['objects.json', 'get_helm.sh'])

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
        firstSID.getFieldValue(SolrSystemClient.FLD_ID) == "${locationName}:objects.json"
        firstSID.getFieldValue(SolrSystemClient.FLD_TYPE) == 'ArchiveEntry'
        firstSID.getFieldValue(SolrSystemClient.FLD_SIZE) == 72904
        firstSID.getFieldValue(SolrSystemClient.FLD_CRAWL_NAME) == 'test'
        firstSID.getFieldValue(SolrSystemClient.FLD_EXTENSION_SS) == 'json'
        firstSID.getFieldValue(SolrSystemClient.FLD_NAME_SIZE_S) == 'objects.json:72904'
    }
*/

    def "should process tarball"() {
        given:
        FSFile tarFsFile = new FSFile(tarFile, parentFolder, locationName, crawlName)

        when:
        List<SavableObject> children = tarFsFile.gatherArchiveEntries()
        List<String> childNames = children.collect { it.name }
//        def archiveEntries = ArchiveUtils.gatherArchiveEntries(aiszip)
//        aiszip?.close()
//        List<SolrInputDocument> sidList = []
//        archiveEntries.each { ArchiveEntry ae ->
//            if (ae.isDirectory()) {
//                ArchFolder archFolder = new ArchFolder(ae, locationName, crawlName)
//                SolrInputDocument sid = archFolder.toSolrInputDocument()
//                sidList << sid
//            } else {
//                ArchFile archFile = new ArchFile(ae, locationName, crawlName)
//                SolrInputDocument sid = archFile.toSolrInputDocument()
//                sidList << sid
//            }
//        }
//        SolrInputDocument firstSID = sidList[0]
//        String firstPath = firstSID.getFieldValue(SolrSystemClient.FLD_PATH_S)

        then:
        children != null
        children.size() == 40
        childNames.containsAll(['Chart.yaml', 'values.yaml'])

//        archiveEntries != null
//        sidList.size() == 40
//        firstPath != null
//        firstSID.getFieldValue(SolrSystemClient.FLD_ID) == "${locationName}:${firstPath}"
//        firstSID.getFieldValue(SolrSystemClient.FLD_TYPE) == 'ArchiveEntry'
//        firstSID.getFieldValue(SolrSystemClient.FLD_SIZE) == 72904
//        firstSID.getFieldValue(SolrSystemClient.FLD_CRAWL_NAME) == 'test'
//        firstSID.getFieldValue(SolrSystemClient.FLD_EXTENSION_SS) == 'json'
//        firstSID.getFieldValue(SolrSystemClient.FLD_NAME_SIZE_S) == 'objects.json:72904'
    }


/*
    def "should get entries from several different archive types"() {
        when:
        FSFolder fsFolder
//        def tarballEntries = ArchiveUtils.gatherArchiveEntries(targz)
//        def tgzEntries = ArchiveUtils.gatherArchiveEntries(tgz)
//        def zipEntries = ArchiveUtils.gatherArchiveEntries(zip)
//        def bz2Entries = ArchiveUtils.gatherArchiveEntries(bz2)

        then:
        tarballEntries != null
//        tgzEntries != null
//        zipEntries != null
//        bz2Entries != null

        tarballEntries.size() == 123
//        tgzEntries.size()==40
//        zipEntries.size()==2
//        bz2Entries.size()==8
    }
*/


}
