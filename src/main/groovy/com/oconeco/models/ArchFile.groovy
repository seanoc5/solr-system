package com.oconeco.models

import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSystemClient
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.io.FilenameUtils
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.LogManager

import org.apache.solr.common.SolrInputDocument

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

/**
 * structure for handling information about an entry from an archive file (tar.gz, zip, etc),
 * handles both basic information from the filesystem, and analysis results from some process
 */
class ArchFile extends SavableObject {
    Logger log = LogManager.getLogger(this.class.name)
    public static final String TYPE = 'ArchFile'
    String extension

    ArchFile(ArchiveEntry ae, SavableObject parent, String locationName, String crawlName ) {
        super(ae, parent, locationName, crawlName)
        String parentId = parent.id
        groupObject = false

        if (ae.name) {
            path = FilenameUtils.getFullPathNoEndSeparator(ae.name)
            String fname = FilenameUtils.getName(ae.name)
            if(fname) {
                name = fname
            } else {
                log.warn "could not get folder name for arch entry: $ae"
            }

            try {
                String ext = FilenameUtils.getExtension(name)
                if (ext) {
                    this.extension = ext
                } else {
                    log.debug "\t\tno extension found for name: ${ae.name} (ext:$ext)"
                }
            } catch (IllegalArgumentException illegalArgumentException){
                log.warn "Problem getting extension for name:($name) -- does it have an illegal character?? perhaps a colon?? Exception:$illegalArgumentException"
            }

            if (parentId) {
                this.id = parentId + "::" + ae.name
            } else {
                id = locationName + ':' + ae.name
                log.warn "Missing parent id ($parentId) or ae.name (${ae.name}), using fallback id ($id)"
            }
        } else {
            log.warn "Missing archive entry name?? $ae"
            name = ae.toString()
            path = ae.toString()
        }

        Long aesize = ae.getSize()
        if (aesize >= 0) {
            size = aesize
            String d = setDedupString()
            log.debug "\t\tbuilt dedup String: $d in archive constructor: $this"
        } else if (ae instanceof ZipArchiveEntry) {
            log.info "\t\t no size for archive file: ${this}"
        } else {
            log.warn "\t\t no size for archive file: ${this}"
        }

        def foo = setLastModifiedDate(ae)

        type = TYPE
    }



    def setLastModifiedDate(ArchiveEntry ae) {
        if (ae instanceof ZipArchiveEntry) {
            long tstamp = ae.getTime()
            if (tstamp) {
                lastModifiedDate = new Date(tstamp)
                log.debug "Zip entry modified time field(s): $lastModifiedDate "
            } else {
                log.info "No Modified date/time in zip entry: $zae"
            }
        } else if (ae instanceof TarArchiveEntry) {
            lastModifiedDate = ((TarArchiveEntry) ae).lastModifiedDate
        } else {
            log.warn "unhandled archive entry (type: ${ae.class.simpleName}) ${ae.name}"
        }
        lastModifiedDate
    }


    SolrInputDocument toPersistenceDocument() {
        SolrInputDocument sid = super.toPersistenceDocument()

        if (extension) {
            sid.addField(SolrSystemClient.FLD_EXTENSION_SS, extension)
        } else {
            log.debug "\t\tno extention for archive file: $this"
        }
        return sid
    }

    boolean isArchive() {
        int fileSignature = 0;
        File f = this.thing
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            fileSignature = raf.readInt();
        } catch (IOException e) {
            log.warn "File ($ffs) io exception checking if we have an archive: $e"
        }
        boolean isArchive = fileSignature == 0x504B0304 || fileSignature == 0x504B0506 || fileSignature == 0x504B0708 || fileSignature == 529205248 || fileSignature == 529205268
        if (labels?.contains(Constants.LBL_ARCHIVE)) {
            if (!isArchive) {
                log.info "Should be archive: $f -> ($fileSignature) -- $isArchive"
            } else {
                log.debug "is archive: $f"
            }
        }
        return isArchive

    }


    /**
     * for the sake of deduplication, we treat an archive file the same as an FSFile -- note this.type will be true ArchFile.TYPE
     * @return string to help compare likely duplicates based on file-type, name, and size (no quarantee, but good enough for focus attention
     */
    @Override
    String buildDedupString() {
        dedup = FSFile.TYPE + ':' + name + '::' + size
        return dedup
    }


}
