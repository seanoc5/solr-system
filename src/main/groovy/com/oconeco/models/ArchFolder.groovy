package com.oconeco.models

import com.oconeco.helpers.Constants
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
class ArchFolder extends SavableObject {
    Logger log = LogManager.getLogger(this.class.name)
    public static final String TYPE = 'ArchFolder'
    String owner

    ArchFolder(ArchiveEntry ae, SavableObject parent, String locationName , String crawlName) {
        super(ae, parent, locationName, crawlName)
        groupObject = true
        type = TYPE
        archive = true
        compressed=true     // todo -- add more logic here to check for archive without compression
        String pid = parent.id
        if (ae.name) {
            path = FilenameUtils.getFullPathNoEndSeparator(ae.name)
            String fname = FilenameUtils.getName(path)      // todo -- revisit getBaseName() ... was that a typo, or is there a reason to do so...?  //            String fname = FilenameUtils.getBaseName(path)
            if(fname) {
                name = fname
            } else {
                log.warn "could not get folder name for arch entry: $ae"
            }

            if (pid) {
                this.parentId = pid
                this.id = ArchFolder.buildId(parentId, ae.name)
            } else {
                id = SavableObject.buildId(locationName, path)
                log.warn "Missing parent id ($parentId) or ae.name (${ae.name}), using fallback id ($id)"
            }

        } else {
            log.warn "Missing archive entry name?? $ae"
            name = ae.toString()
            path = ae.toString()
        }

        if (!this.thing) {
            this.thing = ae
        }
        if(ae.size != null) {
            size = ae.size
        } else {
            log.info "No size for archive entry folder: $ae"
        }
        if (parent?.depth) {
            this.depth = parent.depth + 1
        } else {
            log.warn "No depth for archive entry (folder: $this"
        }

        if (crawlName == Constants.LBL_UNKNOWN || crawlName == null) {
            log.debug "\t\t you likely want to add crawl name...."
        } else {
            this.crawlName = crawlName
        }

        setLastModifiedDate(ae)
        log.debug "Archive Filder: (${this.toString()})"
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
            // todo -- check last modified date processing
            lastModifiedDate = ((TarArchiveEntry) ae).lastModifiedDate
        } else {
            log.warn "unhandled archive entry (type: ${ae.class.simpleName}) ${ae.name}"
        }
        lastModifiedDate
    }


    /**
     * minor helper to add any specific Solr Fields to this type of object
     * @return
     */
    SolrInputDocument toPersistenceDocument() {
        SolrInputDocument sid = super.toPersistenceDocument()
        return sid
    }

}
