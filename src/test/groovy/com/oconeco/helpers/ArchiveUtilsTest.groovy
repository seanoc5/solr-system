package com.oconeco.helpers

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import spock.lang.Specification

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   9/10/22, Saturday
 * @description:
 */

class ArchiveUtilsTest extends Specification {
    File targz = new File(getClass().getResource("/content/fuzzywuzzy.tar.gz").toURI())
    File tgz = new File(getClass().getResource("/content/combinedTestContent.tgz").toURI())
    File zip = new File(getClass().getResource("/content/datasources.zip").toURI())
    File bz2 = new File(getClass().getResource("/content/testsub.tar.bz2").toURI())



    def "unzip tarball"() {
        given:
        InputStream gzi = new GzipCompressorInputStream(targz.newInputStream());
        ArchiveInputStream tarArchive = new TarArchiveInputStream(gzi)

        when:
        TarArchiveEntry entry;
        List<ArchiveEntry> entries = []
        while ((entry = tarArchive.getNextTarEntry()) != null) {
            if (entry.isDirectory()) {
                println "Dir: ${entry.name} (${entry.size})"
            } else if (entry.isFile()) {
                println "\t\tFile: ${entry.name} (${entry.size})"
            } else {
                println "LBL_UNKNOWN entry (NOT FILE no Directory): ${entry.name}"
            }
            entries << entry
        }
        tarArchive.close()

        then:
        entries.size() == 123
        entries[0].isDirectory()
        entries[0].name == 'fuzzywuzzy/'

        entries[2].isFile()
        entries[2].name == 'fuzzywuzzy/build/pom.xml'

    }


/*
    def "GetArchiveInputStream"() {
        given:
        println("Tarball Archive file: $targz -> ${targz.absolutePath}")


        when:
        ArchiveInputStream aistgz = ArchiveUtils.getArchiveInputStream(tgz)
        def tgzEntries = ArchiveUtils.gatherArchiveEntries(aistgz)
        aistgz?.close()

        ArchiveInputStream aisTargz = ArchiveUtils.getArchiveInputStream(targz)
        def targzEntries = ArchiveUtils.gatherArchiveEntries(aisTargz)
        aisTargz?.close()

        ArchiveInputStream aiszip = ArchiveUtils.getArchiveInputStream(zip)
        def zipEntries = ArchiveUtils.gatherArchiveEntries(aiszip)
        aiszip?.close()

        ArchiveInputStream aisbz2 = ArchiveUtils.getArchiveInputStream(bz2)
        def bz2Entries = ArchiveUtils.gatherArchiveEntries(aisbz2)
        aisbz2?.close()

        then:
        tgzEntries.size() == 40
        targzEntries.size() == 123
//        zipEntries.size() == 2
//        bz2Entries.size() == 8
    }
*/


}
