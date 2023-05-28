package com.oconeco.models

import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSaver
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.io.FilenameUtils
import org.apache.log4j.Logger
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
    Logger log = Logger.getLogger(this.class.name)
    public static final String TYPE = 'ArchFile'
    String extension

    ArchFile(SavableObject paren, ArchiveEntry ae, String locationName = Constants.LBL_UNKNOWN, String crawlName = Constants.LBL_UNKNOWN) {
        this(ae, parent, locationName, crawlName)
        String simpleId = this.id
        String parentId = parentFile.id
        this.id = parentId + "::" + simpleId
    }

    ArchFile(ArchiveEntry ae, String locationName = Constants.LBL_UNKNOWN, String crawlName = Constants.LBL_UNKNOWN, Integer depth = null) {
        super(ae, locationName, depth)
        path = ae.name
        name = ae.name
        id = locationName + ':' + ae.name
        type = TYPE

        if (!this.thing) {
            this.thing = ae
        }
        Long aesize = ae.getSize()
        if (aesize > 0) {
            size = aesize
        } else {
            // https://stackoverflow.com/questions/13228168/zipoutputstream-leaving-size-1
            log.warn "\t\t no size for archive file: ${this}"
        }

        if (depth) {
            this.depth = depth
        } else {
            log.debug "\t\tno depth set: $this"
        }

        setLastModifiedDate(ae)

        if (crawlName == Constants.LBL_UNKNOWN || crawlName == null) {
            log.info "\t\t you likely want to add crawl name...."
        } else {
            this.crawlName = crawlName
        }

//        lastModifiedDate = new Date(f.lastModified())

        // todo -- more here -- also check FSFolder object, and analyze() method,
        String ext = FilenameUtils.getExtension(ae.name)
        if (ext) {
            this.extension = ext
        } else {
            log.info "\t\tno extension found for name: ${ae.name} (ext:$ext)"
        }
//        Path path = f.toPath()
//
//        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
//        FileTime lastAccessTime = attr.lastAccessTime()
//        lastAccessDate = new Date(lastAccessTime.toMillis())
//        FileTime lastModifyTime = attr.lastModifiedTime()
//        lastModifyDate = new Date(lastModifyTime.toMillis())
//        createdDate = new Date(attr.creationTime().toMillis())
//        FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
//        owner = ownerAttributeView.getOwner();

        log.debug "File(${this.toString()})"
    }

    public void setLastModifiedDate(ArchiveEntry ae) {
        if (ae instanceof ZipArchiveEntry) {
            long tstamp = ae.getTime()
            if (tstamp) {
                lastModifiedDate = new Date(tstamp)
                log.debug "Zip entry modified time field(s): $lastModifiedDate "
            } else {
                log.info "No Modified date/time in zip entry: $zae"
            }
/*
            if (zae?.xdostime) {
                Date xdate = new Date(zae.xdostime)
                createdDate = xdate
                lastModifiedDate = xdate
                log.debug "Zip entry time field(s): xdostime: $xdate "
            } else {
                log.info "No xdosdate for file entry: $ae"
            }
*/
        } else if (ae instanceof TarArchiveEntry) {
            // todo -- check last modified date processing
            def lmd = ((TarArchiveEntry)ae).getLastModifiedDate()
            lastModifiedDate = ((TarArchiveEntry)ae).lastModifiedDate
//            if (tstamp) {
//                lastModifiedDate = new Date(tstamp)
//                log.debug "Zip entry modified time field(s): $lastModifiedDate "
//            } else {
//                log.info "No Modified date/time in zip entry: $zae"
//            }
        } else {
            log.warn "unhandled archive entry (type: ${ae.class.simpleName}) ${ae.name}"
        }
    }

/*
    ArchFile(org.apache.commons.compress.archivers.ArchiveEntry archiveEntry, String locationName = Constants.LBL_UNKNOWN, String crawlName = Constants.LBL_UNKNOWN, Integer depth = null) {
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
        SolrInputDocument sid = super.toSolrInputDocument()
        if (crawlName) {
            sid.setField(SolrSaver.FLD_CRAWL_NAME, crawlName)
        }
//        sid.addField(SolrSaver.FLD_SIZE, size)

        if (extension) {
            sid.addField(SolrSaver.FLD_EXTENSION_SS, extension)
        } else {
            log.info "\t\tno extention for archive file: $this"
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
