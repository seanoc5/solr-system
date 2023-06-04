package com.oconeco.helpers


import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.io.FilenameUtils
import org.apache.log4j.Logger

import java.nio.file.Files
import java.util.regex.Pattern
/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   9/10/22, Saturday
 * @description:
 */

class ArchiveUtils {
    static final Logger log = Logger.getLogger(this.class.name)

    static ArchiveInputStream getArchiveInputStream(File archiveFile) {
        log.info "Process archive file: $archiveFile"
        ArchiveInputStream ais = null
        InputStream inputStream = null
        String name = archiveFile.name
        String fileType = Files.probeContentType(archiveFile.toPath())
        String ext = FilenameUtils.getExtension(name)
        log.info "\t\tFile type: ($fileType) and extension: $ext"
        if (name.endsWith('tar.gz') || name.endsWith('tgz')) {
            inputStream = new GzipCompressorInputStream(archiveFile.newInputStream());
            ais = new TarArchiveInputStream(inputStream)
        } else if (fileType.equalsIgnoreCase('application/zip') || name.endsWith('zip')) {
            ais = new ZipArchiveInputStream(archiveFile.newInputStream());
        } else if (name.endsWith('bz2')) {
            inputStream = new BZip2CompressorInputStream(archiveFile.newInputStream());
            ais = new TarArchiveInputStream(inputStream)
        } else {
            log.warn "Unknown archive type: $archiveFile"
        }
        return ais
    }


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
}
