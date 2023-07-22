package com.oconeco.models

import com.oconeco.analysis.BaseAnalyzer
import com.oconeco.helpers.Constants
import spock.lang.Specification

class FSFileTest extends Specification {
    String locationName = 'spock'
    String crawlName = 'test'

    String jsonName = 'jq.lst'
    String zipName = 'datasources.zip'
    String tarName = 'combinedTestContent.tgz'

    File contentFolder = new File(getClass().getResource("/content").toURI())
    File jsonFile = new File(getClass().getResource("/content/testsub/subfolder2/subfolder3/$jsonName").toURI())
//    File jsonFile = new File(getClass().getResource("/content/${jsonName}").toURI())
    File zipFile = new File(getClass().getResource("/content/${zipName}").toURI())
    File tarFile = new File(getClass().getResource("/content/${tarName}").toURI())

    FSFolder parentFolder = new FSFolder(jsonFile.parentFile, null, locationName, crawlName)

    def "should create basic FSFile"() {
        when:
        FSFile fsFile = new FSFile(jsonFile, null, locationName, crawlName)
        String pid = SavableObject.buildId(locationName, jsonFile.parentFile.path.replaceAll('\\\\', '/'))

        then:
        fsFile.id.startsWith(fsFile.locationName)
        fsFile.id.endsWith(fsFile.name)
        fsFile.name == jsonName
        fsFile.path.endsWith(jsonFile.name)
        fsFile.type == FSFile.TYPE
        fsFile.thing instanceof File
        fsFile.thing == jsonFile
        fsFile.isArchive() == false

        fsFile.parent == null
        fsFile.parentId == pid
    }


    def "should create basic FSFile with parentFolder"() {
        when:
        FSFile fsFile = new FSFile(jsonFile, parentFolder, locationName, crawlName)
        String pid = SavableObject.buildId(locationName, jsonFile.parentFile.path.replaceAll('\\\\', '/'))

        then:
        fsFile.parent == parentFolder
        fsFile.parentId == parentFolder.id
        fsFile.parentId == pid
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
        FSFile fSFile = new FSFile(tarFile, parentFolder, locationName, crawlName)

        when:
        List<SavableObject> children = fSFile.gatherArchiveEntries()
        def groups = children.groupBy { it.type }


        then:
        children != null
        children.size() == 40

        groups.keySet().toList().containsAll(['ArchFile', 'ArchFolder'])
        groups['ArchFile'].size() == 34
        groups['ArchFile'].collect { it.name }.containsAll(['.helmignore', 'ADOPTERS.md'])
        groups['ArchFolder'].size() == 6
        groups['ArchFolder'].collect { it.name }.containsAll(['subtestme', 'content'])
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

    def "analyze content files files with Analyzer configured from configTest "() {
        given:
        ConfigObject config = new ConfigSlurper().parse(getClass().getResource('/configs/configTest.groovy'))
        BaseAnalyzer analyzer = new BaseAnalyzer(config)
        FSFile fsj = new FSFile(jsonFile, null,locationName, crawlName)
        FSFile fst = new FSFile(tarFile, null,locationName, crawlName)
        FSFile fsz = new FSFile(zipFile, null,locationName, crawlName)
        def files = [fsj, fst, fsz]

        when:
        def rj = analyzer.analyze(fsj)
        def rt = analyzer.analyze(fst)
        def rz = analyzer.analyze(fsz)
        def resultsList = analyzer.analyze(files)

        then:
        rj.size()==3
        rj.keySet().containsAll(['track', 'testFiles', 'contentFiles'])
        rt.keySet().containsAll(['archives', 'testFiles', 'contentFiles'])
        resultsList instanceof List<Map<String, Map<String, Object>>>

//        resultsList.keySet().containsAll(['content', 'workFolder'])
//        resultsList.get(content).keySet().containsAll(['pattern', 'analysis', Constants.LABEL_MATCH])
    }




}
