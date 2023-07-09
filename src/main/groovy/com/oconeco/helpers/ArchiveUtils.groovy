package com.oconeco.helpers

import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.io.FilenameUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger

import java.nio.file.Files
/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   9/10/22, Saturday
 * @description:
 */

class ArchiveUtils {
    static final Logger log = LogManager.getLogger(this.class.name)

    static ArchiveInputStream getArchiveInputStream (File archiveFile) throws IOException {
        log.debug "\t\tProcess archive file: $archiveFile"
        ArchiveInputStream ais = null
        InputStream inputStream = null
        String name = archiveFile.name
        String fileType = Files.probeContentType(archiveFile.toPath())
        String ext = FilenameUtils.getExtension(name)
        log.debug "\t\tgetArchiveInputStream(): File type: ($fileType) and extension: $ext -- file:${archiveFile.absolutePath}"
        if (name.endsWith('tar.gz') || name.endsWith('tgz')) {
            inputStream = new GzipCompressorInputStream(archiveFile.newInputStream());
            ais = new TarArchiveInputStream(inputStream)
        } else if (fileType?.equalsIgnoreCase('application/java-archive') || name?.endsWith('jar') || name?.endsWith('war')) {
            ais = new JarArchiveInputStream(archiveFile.newInputStream());
        } else if (fileType?.equalsIgnoreCase('application/zip') || name?.endsWith('zip')) {
            ais = new ZipArchiveInputStream(archiveFile.newInputStream());
//        } else if (name.endsWith('zip') || name.endsWith('xlsx') || name.endsWith('docx')) {
//            log.warn "Check me!! assuming office *x extensions are zip files"
//            ais = new ZipArchiveInputStream(archiveFile.newInputStream());
        } else if (name.endsWith('bz2')) {
            inputStream = new BZip2CompressorInputStream(archiveFile.newInputStream());
            ais = new TarArchiveInputStream(inputStream)
        } else {
            log.warn "Unknown archive type($fileType), archive file: $archiveFile"
        }
        return ais
    }


/*
    static final Pattern ARCHIVE_EXTENSIONS = Pattern.compile('.*(tar.gz|tgz|tar.z|tar.bz2?|zip)$')

    static boolean isArchiveFileByExtension(File file) {
        String name = file.name.toLowerCase()
        boolean archive = false
//        if(name.endsWith('tar.gz') || name.endsWith('tgz') || name.endsWith('tar.z') || name.endsWith('tar.bz2')){
        if (name ==~ ARCHIVE_EXTENSIONS) {
            archive = true
        }

        return archive
    }
*/
}
