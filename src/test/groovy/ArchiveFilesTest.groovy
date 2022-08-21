import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import spock.lang.Specification

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/1/22, Monday
 * @description:
 */

class ArchiveFilesTest extends Specification {

    def "walk zip"() {
        given:
        File zip = new File(getClass().getResource('/content/datasources.zip').toURI())
//        ZipArchiveEntry entry = new ZipArchiveEntry(name);
//        entry.setSize(size);
//        zipOutput.putArchiveEntry(entry);
//        zipOutput.write(contentOfEntry);
//        zipOutput.closeArchiveEntry();

        InputStream fi = zip.newInputStream()
        InputStream bi = new BufferedInputStream(fi);
        InputStream gzi = new GzipCompressorInputStream(bi);
        ArchiveInputStream o = new TarArchiveInputStream(gzi) {
        }


        when:
        String name = zip.name

        then:
        name == 'datasources.zip'
    }


}
