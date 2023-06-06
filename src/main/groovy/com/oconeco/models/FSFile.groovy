package com.oconeco.models

import com.oconeco.helpers.ArchiveUtils
import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSystemClient
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
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
 * structure for handling information about a folder from the FS (filesystem),
 * handles both basic information from the filesystem, and analysis results from some process
 */
class FSFile extends SavableObject {
    Logger log = Logger.getLogger(this.class.name)
    public static final String TYPE = 'File'
    String extension
    String mimeType
    String owner
//    List<String> permissions

    String osName
//    Date lastAccessDate
//    Date lastModifyDate
//    Boolean archive
//    Boolean compressed

    FSFile(File f, SavableObject parent, String locationName = Constants.LBL_UNKNOWN, String crawlName = Constants.LBL_UNKNOWN) {
        super(f, parent, locationName)
        path = f.absolutePath
        // todo -- revisit if this replacing backslashes with forward slashes helps, I had trouble querying for id with backslashes (SoC 20230603)
        id = (locationName + ':' + path).replaceAll('\\\\', '/')
        type = TYPE
        hidden = f.isHidden()
        if (hidden)
            log.info "\t\tprocessing hidden file: $f"

        if (!this.thing) {
            this.thing = f
        }
        name = f.name
        size = f.size()

        dedup = buildDedupString()


        if (depth) {
            this.depth = depth
        }

        if (crawlName == Constants.LBL_UNKNOWN || crawlName == null) {
            log.debug "\t\t you likely want to add crawl name...."
        } else {
            this.crawlName = crawlName
        }

        lastModifiedDate = new Date(f.lastModified())

        // todo -- more here -- also check FSFolder object, and analyze() method,
        extension = FilenameUtils.getExtension(f.name)
        Path path = f.toPath()

        osName = System.getProperty("os.name")

        if (f.exists()) {
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class)
            FileTime lastAccessTime = attr.lastAccessTime()
            lastAccessDate = new Date(lastAccessTime.toMillis())
            FileTime lastModifyTime = attr.lastModifiedTime()
            lastModifiedDate = new Date(lastModifyTime.toMillis())
            createdDate = new Date(attr.creationTime().toMillis())
            FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(path, FileOwnerAttributeView.class)
            owner = ownerAttributeView.getOwner()
        } else {
            log.warn "File: $f does not exist!! broken symlink??"
        }
        log.debug "File(${this.toString()})"
    }

    // todo -- remove this, move logic to something like fsFile.expandArchive()
/*
    FSFile(ArchiveEntry archiveEntry, String locationName = Constants.LBL_UNKNOWN, String crawlName = Constants.LBL_UNKNOWN, Integer depth = null) {
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


    /**
     * create an index-ready solr input doc from this FSFile object
     * @return SolrInputDocument
     */
    @Override
    SolrInputDocument toSolrInputDocument() {
        SolrInputDocument sid = super.toSolrInputDocument()
        if (crawlName) {
            sid.setField(SolrSystemClient.FLD_CRAWL_NAME, crawlName)
        }

        if (owner) {
            sid.setField(SolrSystemClient.FLD_OWNER, owner)
        }

        if (extension) {
            sid.addField(SolrSystemClient.FLD_EXTENSION_SS, extension)
        }

        if (!sid.getFieldValue(SolrSystemClient.FLD_DEDUP)) {
            if (dedup) {
                sid.addField(SolrSystemClient.FLD_DEDUP, dedup)
            } else {
                log.warn "No dedup value available!!!"
                sid.addField(SolrSystemClient.FLD_DEDUP, "${this.name}:${this.size}")
            }
        } else {
            log.debug("have a dedup value: $dedup -- $this")
        }
        return sid
    }

    boolean isArchive() {
        int fileSignature = 0
        File f = this.thing
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            fileSignature = raf.readInt()
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

    List<SavableObject> gatherArchiveEntries() {
        ArchiveInputStream aiszip = ArchiveUtils.getArchiveInputStream(this.thing)
        List<SavableObject> archiveEntries = gatherArchiveEntries(aiszip)
        aiszip.close()
        return archiveEntries
    }


    /**
     * helper util to get archive entries for any of the 'handled' archive types (e.g. zip, tar,...)
     * @param ais
     * @return List of various Archive entries
     */
    List<SavableObject> gatherArchiveEntries(ArchiveInputStream ais) {
        if (ais) {
            if (!children) {
                log.debug "Create children list... ($this)"
                children = []
            }
//             ArchiveEntry entry = null
            while ((entry = ais.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    ArchFolder archFolder = new ArchFolder(entry, this, locationName, crawlName)
                    children << archFolder
                    log.info "Dir: ${archFolder.toString()})"
                } else {
//                log.debug "\t\tarchive entry file (${entry.name}) -- type:(${entry.class.simpleName})"
                    ArchFile archFile = new ArchFile(entry, this, locationName, crawlName)
                    children << archFile
                    log.debug "\t\tFile: ${archFile.toString()})"

                }
            }
        } else {
            log.warn "Invalid archive input stream: $ais, how did this happen??"
        }
        return children
    }


    List<SolrInputDocument> gatherSolrInputDocs(FSFile fsfile, String locationName = Constants.LBL_UNKNOWN, String crawlName = Constants.LBL_UNKNOWN, Integer depth = null) {
        ArchiveInputStream aiszip = ArchiveUtils.getArchiveInputStream(fsfile)
        List<SolrInputDocument> sidList = gatherSolrInputDocs(aiszip, locationName, crawlName, depth)
        return sidList
    }

    List<SolrInputDocument> gatherSolrInputDocs(ArchiveInputStream ais, String locationName = Constants.LBL_UNKNOWN, String crawlName = Constants.LBL_UNKNOWN, Integer depth = null) {
        List<SolrInputDocument> sidList = []
        // todo -- get parent file object, change constructor sig below
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


    /**
     * custom string return value for this
     * @return string/label
     */
    String toString() {
        String s = null
        if (labels) {
            s = "${type}: ${name} :: (${labels[0]})"
        } else {
            s = "${type}: ${name}" + (ignore ? "[ignore:$ignore]" : '')
        }
        return s
    }

}
