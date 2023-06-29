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
        type = TYPE
        name = f.name
        path = f.absolutePath
        if (this.crawlName) {
            log.debug "Crawl name already set?? $crawlName"
        } else {
            this.crawlName = crawlName
        }
        if (this.locationName) {
            log.debug "Location name already set?? $locationName"
        } else {
            this.locationName = locationName
        }


        hidden = f.isHidden()
        if (hidden) {
            log.info "\t\t~~~~processing hidden file: $f"
        }

        // todo -- revisit if this replacing backslashes with forward slashes helps, I had trouble querying for id with backslashes (SoC 20230603)
        if (osName) {
            log.warn "Where did OSName get set already? $osName"
        } else {
            osName = System.getProperty("os.name")
        }
        if (osName.contains('Windows')) {
            // todo -- can windows have backslashes in names?? hopefully not
            log.warn "\t\tReplace backslashes in windows path with forward slashes"     //todo change from warn to debug after testing in Widoze
            path = path.replaceAll('\\\\', '/')
        }
        id = SavableObject.buildId(locationName, path)

        if (osName == null) {
            log.warn "OSName is null??!! $this"
            osName = System.getProperty("os.name")
//        } else {
//            log.debug "OSName: $osName"
        }

        lastModifiedDate = new Date(f.lastModified())

        if (f.exists()) {
            // todo -- revisit replacing backslashes with forward slashes--just cosmetics? avoid double-backslashes in path fields for windows machines
            if (parent) {
                if (parent.id) {
                    log.debug "\t\tGetting parent id from parent Object ($parent) for this:$this"
                    parentId = parent.id
                } else {
                    log.warn "FSFolder ($this) has a parent ($parent) BUT that has no id!!!? (${parent.id}) -- that makes no sense!!"
                }
            } else if (f.parentFile) {
                // todo -- check if such a parent is (or will be) saved? currently just saving the id of a parent without caring if it does or will exist in solr
                String p = f.parent
                if (osName == null) {
                    log.warn "Unknown/null osName ($osName), replacing any backslashes with forward in path($p) --this:$this"
                    p = p.replaceAll('\\\\', '/')
                } else if (osName?.contains('Windows')) {
                    log.info "\t\treplacing Windows backslashes(\\)  with forward (/) in path: $p (this)"
                    p = p.replaceAll('\\\\', '/')
                } else {
                    if (p.contains('\\')) {
                        log.warn "Path has a backslash ($p) -- is this a problem for solr searching??? ($this)"
                    }
                    log.debug "\t\tPath for os ($osName) does not need backslash replacement (??)"
                }
                parentId = SavableObject.buildId(locationName, p)
            }

            // leave this as separate calls for file and folder, as file is simple size, folder needs special summing call
//            dedup = buildDedupString()

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



    def addFileDetails() {
        Map<String, Object> details = [:]
        File f = (File) this.thing

        if (!path) {
            log.warn "No path set for this FSObject ($this)??!! it should have been set already"
            path = f.absolutePath
            details.path = path
        }

        BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
        FileTime lastAccessTime = attr.lastAccessTime()
        lastAccessDate = new Date(lastAccessTime.toMillis())
        details.lastAccessDate = lastAccessDate

        if (!lastModifiedDate) {
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
