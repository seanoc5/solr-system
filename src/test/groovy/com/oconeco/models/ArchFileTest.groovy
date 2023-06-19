package com.oconeco.models


import spock.lang.Specification
// todo -- move this functionality to a constructor in the Crawler, and have the crawler identify archives, and handle them???

class ArchFileTest extends Specification {
    String locationName = 'spock'
    String crawlName = 'test'

    String tarName = 'combinedTestContent.tgz'
    String zipName = 'datasources.zip'

    File zipSrcFile = new File(getClass().getResource("/content/${zipName}").toURI())
    FSFolder parentFolder = new FSFolder(zipSrcFile.parentFile, locationName, crawlName, ignoreFolders, ignoreFiles, 1)
    FSFile fsZipFile = new FSFile(zipSrcFile, parentFolder, locationName, crawlName)
    File tarSrcFile = new File(getClass().getResource("/content/${tarName}").toURI())
    FSFile fstarFile = new FSFile(tarSrcFile, parentFolder, locationName, crawlName)

//    def "check basic constructor"() {
//        when:
//        ArchFolder archFolder = new ArchFolder()
//        List<SavableObject> zipEntries = ArchiveUtils.gatherArchiveEntries(zipSrcFile)
//        List<ArchiveEntry> tarEntries = ArchiveUtils.gatherArchiveEntries(tarSrcFile)
//
//        then:
//        zipEntries != null
//        zipEntries.size() == 2
//        zipEntries[0].name == 'objects.json'
//
//        tarEntries != null
//        tarEntries.size() == 40
//        tarEntries[0].name == 'compressedchart/Chart.yaml'
//
//
//    }

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
