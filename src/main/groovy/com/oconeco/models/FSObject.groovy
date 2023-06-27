package com.oconeco.models

import org.apache.log4j.Logger

import java.nio.file.Files
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
 * todo -- consider interface??
 * structure for handling information about a filesystem object,
 */
class FSObject extends SavableObject {
    Logger log = Logger.getLogger(this.class.name)
    public static final String TYPE = 'FSObject'

    String owner
    String osName


    FSObject(File f, SavableObject parent, String locationName, String crawlName) {
        super(f, parent, locationName, crawlName)
        name = f.name
        path = f.absolutePath
        // todo -- revisit if this replacing backslashes with forward slashes helps, I had trouble querying for id with backslashes (SoC 20230603)
        osName = System.getProperty("os.name")
        if (osName.contains('Windows')) {
            // todo -- can windows have backslashes in names?? hopefully not
            log.warn "\t\tReplace backslashes in windows path with forward slashes"     //todo change from warn to debug after testing in Widoze
            path = path.replaceAll('\\\\', '/')
        }
        id = SavableObject.buildId(locationName, path)
        type = TYPE

        hidden = f.isHidden()
        if (hidden) {
            log.info "\t\t~~~~processing hidden file: $f"
        }

        if(osName==null){
            log.warn "OSName is null??!! $this"
            osName = System.getProperty("os.name")
        } else {
            log.info "OSName: $osName"
        }

        lastModifiedDate = new Date(f.lastModified())

        if (f.exists()) {
//            Path p = f.toPath()
//            BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class)
//            FileTime lastAccessTime = attr.lastAccessTime()
//            lastAccessDate = new Date(lastAccessTime.toMillis())
//            FileTime lastModifyTime = attr.lastModifiedTime()
//            lastModifiedDate = new Date(lastModifyTime.toMillis())
//            createdDate = new Date(attr.creationTime().toMillis())
//            FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(p, FileOwnerAttributeView.class)
//            owner = ownerAttributeView.getOwner()
        } else {
            log.warn "File: $f does not exist!! broken symlink??"
        }
        log.debug "File(${this.toString()})"
    }


//    List<SolrInputDocument> gatherSolrInputDocs(FSObject fsfile, String locationName = Constants.LBL_UNKNOWN, String crawlName = Constants.LBL_UNKNOWN, Integer depth = null) {
//        ArchiveInputStream aiszip = ArchiveUtils.getArchiveInputStream(fsfile)
//        List<SolrInputDocument> sidList = gatherSolrInputDocs(aiszip, locationName, crawlName, depth)
//        return sidList
//    }


    def addFolderDetails() {
        Map<String, Object> details = [:]
        File srcFolder = (File) this.thing

        if (!path) {
            log.warn "No path set for this FSObject ($this)??!! it should have been set already"
            path = srcFolder.absolutePath
            details.path = path
        }

        BasicFileAttributes attr = Files.readAttributes(srcFolder.toPath(), BasicFileAttributes.class);
        FileTime lastAccessTime = attr.lastAccessTime()
        lastAccessDate = new Date(lastAccessTime.toMillis())
        details.lastAccessDate = lastAccessDate

        if(!lastModifiedDate){
            log.warn "No lastModifiedDate set yet?? this:$this"
            FileTime lastModifyTime = attr.lastModifiedTime()
            lastModifiedDate = new Date(lastModifyTime.toMillis())
            details.lastModifiedDate = lastModifiedDate
        }


        createdDate = new Date(attr.creationTime().toMillis())
        details.createdDate = createdDate
        FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(((File) this.thing).toPath(), FileOwnerAttributeView.class);

        owner = ownerAttributeView.getOwner();
        details.owner = owner

        return details
    }


}
