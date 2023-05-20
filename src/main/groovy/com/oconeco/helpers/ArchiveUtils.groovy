package com.oconeco.helpers

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.io.FilenameUtils
import org.apache.log4j.Logger

import java.nio.file.Files
/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   9/10/22, Saturday
 * @description:
 */

class ArchiveUtils {
    static final Logger log = Logger.getLogger(this.class.name)

    static ArchiveInputStream getArchiveInputStream(File archiveFile) {
        ArchiveInputStream ais = null
        InputStream inputStream = null
        String name = archiveFile.name
        String fileType = Files.probeContentType(archiveFile.toPath())
        String ext = FilenameUtils.getExtension(name)
        log.info "\t\tFile type: ($fileType) and extension: $ext"
        if (name.endsWith('tar.gz') || name.endsWith('tgz')) {
            inputStream = new GzipCompressorInputStream(archiveFile.newInputStream());
            ais = new TarArchiveInputStream(inputStream)
        } else if (fileType.equalsIgnoreCase('application/zip') || name.endsWith('zip') ) {
            ais = new ZipArchiveInputStream(archiveFile.newInputStream());
        } else if (name.endsWith('bz2') ) {
                inputStream = new BZip2CompressorInputStream(archiveFile.newInputStream());
                ais = new TarArchiveInputStream(inputStream)
        } else {
            log.warn "Unknown archive type: $archiveFile"
        }
        return ais
    }

    static gatherArchiveEntries(ArchiveInputStream ais) {
        List<ArchiveEntry> entries = []
        ArchiveEntry entry = null
        while ((entry = ais.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                log.info "Dir: ${entry.name} (${entry.size})"
                entries << entry
            } else {
                log.info "archive entry file (${entry.name}) -- type:(${entry.class.name})"
//                Long rsize = entry.getRealSize()
                Long size = entry.size
                log.debug "\t\tFile: ${entry.name} (${entry.size})"
                entries << entry

//            } else {
//                println "LBL_UNKNOWN entry (NOT FILE no Directory): ${entry.name}"
            }
        }
        return entries
    }

    static boolean isArchiveFileByExtension(File file){
        String name = file.name.toLowerCase()
        boolean archive = false
        if(name.endsWith('tar.gz') || name.endsWith('tgz') || name.endsWith('tar.z') || name.endsWith('tar.bz2')){
            archive = true
        }

        return archive
    }
}
