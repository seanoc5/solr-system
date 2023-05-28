package com.oconeco.helpers


import com.oconeco.models.ArchFile
import com.oconeco.models.ArchFolder
import com.oconeco.models.FSFile
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.io.FilenameUtils
import org.apache.log4j.Logger
import org.apache.solr.common.SolrInputDocument

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

    static gatherArchiveEntries(File f) {
        ArchiveInputStream aiszip = ArchiveUtils.getArchiveInputStream(f)
        List<ArchiveEntry> archiveEntries = gatherArchiveEntries(aiszip)
        return archiveEntries
    }


    /**
     * helper util to get archive entries for any of the 'handled' archive types (e.g. zip, tar,...)
     * @param ais
     * @return List of various Archive entries
     */
    static List<ArchiveEntry> gatherArchiveEntries(ArchiveInputStream ais) {
        List<ArchiveEntry> entries = []
        ArchiveEntry entry = null
        while ((entry = ais.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                entries << entry
                log.info "Dir: ${entry.name} (${entry.size})"
            } else {
                log.debug "\t\tarchive entry file (${entry.name}) -- type:(${entry.class.simpleName})"
//                Long rsize = entry.getRealSize()
                Long size = entry.size
                entries << entry
                log.debug "\t\tFile: ${entry.name} (${entry.size})"

            }
        }
        return entries
    }


    static List<SolrInputDocument> gatherSolrInputDocs(FSFile fsfile, String locationName = Constants.LBL_UNKNOWN, String crawlName = Constants.LBL_UNKNOWN, Integer depth = null) {
        ArchiveInputStream aiszip = ArchiveUtils.getArchiveInputStream(fsfile)
        List<SolrInputDocument> sidList = gatherSolrInputDocs(aiszip, locationName, crawlName, depth)
        return sidList
    }

    static List<SolrInputDocument> gatherSolrInputDocs(ArchiveInputStream ais, String locationName = Constants.LBL_UNKNOWN, String crawlName = Constants.LBL_UNKNOWN, Integer depth = null) {
        List<SolrInputDocument> sidList = []
        ArchiveEntry entry = null
        while ((entry = ais.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                ArchFolder folder = new ArchFolder(entry, depth, locationName, crawlName)
                SolrInputDocument sid = folder.toSolrInputDocument()
                sidList << sid
                log.debug "Archive folder (solr doc): ${sid})"
            } else {
                log.warn "REFACTOR: \t\tarchive entry file (${entry.name}) -- type:(${entry.class.simpleName}) --- move to constructor with FSFile object (to get parent file path)"
                ArchFile file = new ArchFile(entry, locationName, crawlName)
                SolrInputDocument sid = file.toSolrInputDocument()
                sidList << sid
//                Long size = entry.size
//                sidList << entry
                log.debug "\t\tArchive File (solr doc): ${sid})"

            }
        }
        return sidList
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
