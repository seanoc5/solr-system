import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import spock.lang.Specification
/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/1/22, Monday
 * @description:
 */

class ArchiveFilesTest extends Specification {

    def "test zip file"() {
        // https://itsallbinary.com/apache-commons-compress-simplest-zip-zip-with-directory-compression-level-unzip/
        given:
        File zip = new File(getClass().getResource('/content/test.zip').toURI())
/*
        ZipArchiveEntry entry = new ZipArchiveEntry();
        entry.setSize(size);
        zipOutput.putArchiveEntry(entry);
        zipOutput.write(contentOfEntry);
        zipOutput.closeArchiveEntry();
*/
        ZipArchiveInputStream archive = new ZipArchiveInputStream(new BufferedInputStream(new FileInputStream(zip)))

        List entries = []

        when:
        ZipArchiveEntry entry;
        while ((entry = archive.getNextZipEntry()) != null) {
            entries << entry
            Long size = entry.size
            Long csize = entry.compressedSize
            int percent = ((size - csize) / size) * 100
            println "${entry.name} (${size}->${csize} ${percent}%) :-: ctime: ${entry.creationTime} -- mtime: ${entry.lastModifiedTime} -- lastAccess: ${entry.lastAccessTime} "
        }
        ArchiveEntry entry1 = entries[1]

        then:
        entries.size() == 5

    }

    def "test tarball tar.gz file"() {
        given:
        List<TarArchiveEntry> entries = []
        File tarball = new File(getClass().getResource('/content/fuzzywuzzy.tar.gz').toURI())

        InputStream fi = tarball.newInputStream()
        InputStream bi = new BufferedInputStream(fi);
        InputStream gzi = new GzipCompressorInputStream(bi);
        ArchiveInputStream tarArchive = new TarArchiveInputStream(gzi)

        when:
        TarArchiveEntry entry;
        while ((entry = tarArchive.getNextTarEntry()) != null) {
            if (entry.isDirectory()) {
                println "Dir: ${entry.name} (${entry.size})"
            } else if (entry.isFile()) {
//                Long rsize = entry.getRealSize()
                Long size = entry.size
//                int percent = ((rsize - csize) / size) * 100
//                println "${entry.name} (${size}->${csize} ${percent}%) :-:  mtime: ${entry.modTime}"
                println "${entry.name} (${size} :-:  mtime: ${entry.modTime}"
//                println "\t\tFile: ${entry.name} (${entry.size})"
            } else {
                println "LBL_UNKNOWN entry (NOT FILE no Directory): ${entry.name}"
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

    def "multiple archive file types some with compression "() {
        given:
        List<String> srcArchFiles = ['compressedchart-0.1.0.tgz', 'datasources.zip', 'testsub.tar.bz2']
        Map<String, Object>  archEntryMap = [:]

        srcArchFiles.each { String archFileName ->
            File f = new File(getClass().getResource("/content/${archFileName}").toURI())
            println("Archive file: $archFileName -> ${f.absolutePath}")
            InputStream gzi = new GzipCompressorInputStream(f.newInputStream());
            ArchiveInputStream tarArchive = new TarArchiveInputStream(gzi)
        }

        when:
        TarArchiveEntry entry;
        while ((entry = tarArchive.getNextTarEntry()) != null) {
            if (entry.isDirectory()) {
                println "Dir: ${entry.name} (${entry.size})"
            } else if (entry.isFile()) {
//                Long rsize = entry.getRealSize()
                Long size = entry.size
//                int percent = ((rsize - csize) / size) * 100
                println "${entry.name} (${size}->${csize} ${percent}%) :-:  mtime: ${entry.modTime}"
                println "${entry.name} (${size} :-:  mtime: ${entry.modTime}"
                println "\t\tFile: ${entry.name} (${entry.size})"
            } else {
                println "LBL_UNKNOWN entry (NOT FILE no Directory): ${entry.name}"
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


//    def getArchiveStream(File file){
//        String ext = FilenameUtils.getExtension(f)
//        if(ext=='zip'
//    }

}
