package com.oconeco.models

import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSystemClient
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.io.FilenameUtils
import org.apache.log4j.Logger
import org.apache.solr.common.SolrInputDocument

import java.nio.file.attribute.FileTime

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
    Logger log = Logger.getLogger(this.class.name)
    public static final String TYPE = 'ArchFolder'
    String owner
//    List<String> permissions
//    Date lastAccessDate
//    Date lastModifyDate
//    Boolean archive = true
//    Boolean compressed = true

    ArchFolder(ArchiveEntry ae, SavableObject parent, String locationName = Constants.LBL_UNKNOWN, String crawlName = Constants.LBL_UNKNOWN) {
        super(ae, parent, locationName)
        type = TYPE
        archive = true
        compressed=true     // todo -- add more logic here to check for archive without compression
        String parentId = parent.id
        if (ae.name) {
            path = FilenameUtils.getFullPathNoEndSeparator(ae.name)
            String fname = FilenameUtils.getBaseName(path)
            if(fname) {
                name = fname
            } else {
                log.warn "could not get folder name for arch entry: $ae"
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

        if (!this.thing) {
            this.thing = f
        }
        if(ae.size) {
            size = ae.size
        } else {
            log.debug "No size for archive entry folder: $ae"
        }
        if (parent?.depth) {
            this.depth = parent.depth + 1
        }

        if (crawlName == Constants.LBL_UNKNOWN || crawlName == null) {
            log.debug "\t\t you likely want to add crawl name...."
        } else {
            this.crawlName = crawlName
        }

//        lastModifiedDate = new Date(ae.lastModifiedDate)
//        lastModifiedDate = new Date(ae.lastModifyDate)

        // todo -- more here -- also check FSFolder object, and analyze() method,
//        extension = FilenameUtils.getExtension(f.name)
//        Path path = f.toPath()

//        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
//        FileTime lastAccessTime = attr.lastAccessTime()
//        lastAccessDate = new Date(lastAccessTime.toMillis())
        FileTime ftime
        if (ae.mTime) {
            ftime = ae.mTime
            this.lastModifiedDate = new Date(ftime.toMillis())
        } else {
            log.info "What does this archive type (${ae.class.simpleName} have for modify time??"
        }
//        createdDate = new Date(attr.creationTime().toMillis())
//        FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
//        owner = ownerAttributeView.getOwner();

        log.debug "Archive Filder: (${this.toString()})"
    }

/*
    ArchFileArchiveEntry archiveEntry, String locationName = Constants.LBL_UNKNOWN, String crawlName = Constants.LBL_UNKNOWN, Integer depth = null) {
        super(archiveEntry,locationName, depth)
        id = locationName + ':' + archiveEntry.name
        type = 'ArchiveEntry'

        if(!this.thing) {
            this.thing = archiveEntry
        }
        name = archiveEntry.name
        size = archiveEntry.size

//        if(depth) {
//            this.depth = depth
//        }

        if(crawlName==Constants.LBL_UNKNOWN || crawlName == null) {
            log.debug "\t\t you likely want to add crawl name...."
        } else {
            this.crawlName = crawlName
        }

//        lastModifiedDate = new Date(f.lastModified())

        // todo -- more here -- also check FSFolder object, and analyze() method,
        extension = FilenameUtils.getExtension(archiveEntry.name)
//        owner = ownerAttributeView.getOwner();

        log.debug "Archive Entry File(${this.toString()})"
    }
*/

    String toString() {
        String s = null
        if (labels) {
            s = "${type}: ${name} :: (${labels[0]})"
        } else {
            s = "${type}: ${name}"
        }
        return s
    }

    SolrInputDocument toSolrInputDocument() {
//        File f = thing
        SolrInputDocument sid = super.toSolrInputDocument()
//        sid.addField(SolrSystemClient.FLD_SIZE, size)
        if (crawlName) {
            sid.setField(SolrSystemClient.FLD_CRAWL_NAME, crawlName)
        }

//        if (this.size) {
//            sid.addField(SolrSystemClient.FLD_NAME_SIZE_S, "${this.name}:${this.size}")
//        } else {
//            sid.addField(SolrSystemClient.FLD_NAME_SIZE_S, "${this.name}:unknown")
//        }
        if(dedup){
            log.warn "ensure dedupe is saved, and replaces name-size field"
        }

//        sid.addField(SolrSystemClient.FLD_DEPTH, depth)
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


}
