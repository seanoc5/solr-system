package com.oconeco.models

import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSaver
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.io.FilenameUtils
import org.apache.log4j.Logger
import org.apache.solr.common.SolrInputDocument

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileOwnerAttributeView
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
class ArchiveFolder extends SavableObject {
    Logger log = Logger.getLogger(this.class.name)
    public static final String TYPE = 'ArchiveFolder'
    String owner
//    List<String> permissions
    Date lastAccessDate
    Date lastModifyDate
    Boolean archive = true
    Boolean compressed = true

    ArchiveFolder(ArchiveEntry ae, String locationName = Constants.LBL_UNKNOWN, String crawlName = Constants.LBL_UNKNOWN, Integer depth = null) {
        super(ae,locationName, depth)
        id = locationName + ':' + f.absolutePath
        type = TYPE

        if(!this.thing) {
            this.thing = f
        }
        name = f.name
        size = f.size()

        if(depth) {
            this.depth = depth
        }

        if(crawlName==Constants.LBL_UNKNOWN || crawlName == null) {
            log.debug "\t\t you likely want to add crawl name...."
        } else {
            this.crawlName = crawlName
        }

        lastModifiedDate = new Date(f.lastModified())

        // todo -- more here -- also check FSFolder object, and analyze() method,
        extension = FilenameUtils.getExtension(f.name)
        Path path = f.toPath()

        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
        FileTime lastAccessTime = attr.lastAccessTime()
        lastAccessDate = new Date(lastAccessTime.toMillis())
        FileTime lastModifyTime = attr.lastModifiedTime()
        lastModifyDate = new Date(lastModifyTime.toMillis())
        createdDate = new Date(attr.creationTime().toMillis())
        FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
        owner = ownerAttributeView.getOwner();

        log.debug "File(${this.toString()})"
    }

/*
    ArchiveFile(org.apache.commons.compress.archivers.ArchiveEntry archiveEntry, String locationName = Constants.LBL_UNKNOWN, String crawlName = Constants.LBL_UNKNOWN, Integer depth = null) {
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
        if (assignedTypes) {
            s = "${type}: ${name} :: (${assignedTypes[0]})"
        } else {
            s = "${type}: ${name}"
        }
        return s
    }

    SolrInputDocument toSolrInputDocument() {
//        File f = thing
        SolrInputDocument sid = super.toSolrInputDocument()
//        sid.addField(SolrSaver.FLD_SIZE, size)
        if(crawlName){
            sid.setField(SolrSaver.FLD_CRAWL_NAME, crawlName)
        }

        if (extension) {
            sid.addField(SolrSaver.FLD_EXTENSION_SS, extension)
        }
        sid.addField(SolrSaver.FLD_NAME_SIZE_S, "${this.name}:${this.size}")
//        sid.addField(SolrSaver.FLD_DEPTH, depth)
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
        boolean isArchive = fileSignature == 0x504B0304 || fileSignature == 0x504B0506 || fileSignature == 0x504B0708 || fileSignature== 529205248 || fileSignature== 529205268
        if(assignedTypes?.contains(Constants.LBL_ARCHIVE)) {
            if(!isArchive) {
                log.info "Should be archive: $f -> ($fileSignature) -- $isArchive"
            } else {
                log.debug "is archive: $f"
            }
        }
        return isArchive

    }



}