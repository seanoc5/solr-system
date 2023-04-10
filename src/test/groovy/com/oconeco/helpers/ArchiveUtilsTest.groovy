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
    File tarball = new File(getClass().getResource("/content/fuzzywuzzy.tar.gz").toURI())

    def "unzip tarball"() {
        given:
        println("Archive file: ${tarball.absolutePath}")
        InputStream gzi = new GzipCompressorInputStream(tarball.newInputStream());
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
                println "UNKNOWN entry (NOT FILE no Directory): ${entry.name}"
            }
            entries << entry
        }

        then:
        entries.size() == 123
        entries[0].isDirectory()
        entries[0].name == 'fuzzywuzzy/'

        entries[2].isFile()
        entries[2].name == 'fuzzywuzzy/build/pom.xml'

    }


    def "GetArchiveInputStream"() {
        given:
        File targz = new File(getClass().getResource("/content/fuzzywuzzy.tar.gz").toURI())
        println("Tarball Archive file: $targz -> ${targz.absolutePath}")
        File tgz = new File(getClass().getResource("/content/compressedchart-0.1.0.tgz").toURI())
        File zip = new File(getClass().getResource("/content/datasources.zip").toURI())
        File bz2 = new File(getClass().getResource("/content/testsub.tar.bz2").toURI())

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
        tgzEntries.size()==4
        targzEntries.size()==123
        zipEntries.size()==2
        bz2Entries.size()==8
    }
}
