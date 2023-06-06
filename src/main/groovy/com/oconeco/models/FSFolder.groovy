package com.oconeco.models

import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSystemClient
import org.apache.commons.io.FileUtils
import org.apache.log4j.Logger
import org.apache.solr.common.SolrInputDocument

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileOwnerAttributeView
import java.nio.file.attribute.FileTime
import java.util.regex.Pattern

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

/**
 * structure for handling information about a folder from the FS (filesystem),
 * handles both basic information from the filesystem, and analysis results from some process
 * todo -- switch to NIO Files and Paths
 */
class FSFolder extends SavableObject {
    public static final String TYPE = 'Folder'
    String osName
    Logger log = Logger.getLogger(this.class.name);


    FSFolder(File srcFolder, SavableObject parent, String locationName, String crawlName = Constants.LBL_UNKNOWN) {
        this(srcFolder, locationName, crawlName, parent.depth + 1)
        this.parent = parent
    }


    /**
     * This constructor has the ability to recurse through the filesystem and build tree
     * @param id
     * @param locationName
     */
    FSFolder(File srcFolder, String locationName, String crawlName = Constants.LBL_UNKNOWN, Integer depth = null) {
        super(srcFolder, locationName, crawlName, depth)
        osName = System.getProperty("os.name")
        if (srcFolder.isHidden()) {
            log.info "\t\tprocessing hidden folder: $srcFolder"
            hidden = true
        }
        if (srcFolder.exists() && srcFolder.canRead() && srcFolder.canExecute()) {
            type = TYPE
            name = srcFolder.getName()
            size = srcFolder.size()
            path = srcFolder.absolutePath

            // todo -- revisit if this replacing backslashes with forward slashes helps, I had trouble querying for id with backslashes (SoC 20230603)
            id = (locationName + ':' + path).replaceAll('\\\\', '/')

            dedup = buildDedupString()

            size = FileUtils.sizeOfDirectory(srcFolder)

            Path nioPath = srcFolder.toPath()
            BasicFileAttributes attr = Files.readAttributes(nioPath, BasicFileAttributes.class);
            FileTime lastAccessTime = attr.lastAccessTime()
            lastAccessDate = new Date(lastAccessTime.toMillis())
            FileTime lastModifyTime = attr.lastModifiedTime()
            lastModifiedDate = new Date(lastModifyTime.toMillis())
            createdDate = new Date(attr.creationTime().toMillis())
            FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(nioPath, FileOwnerAttributeView.class);
            owner = ownerAttributeView.getOwner();


        } else {
            log.warn "Src folder ($srcFolder) is not accessible??"
        }
    }

    boolean exists() {
        ((File) this.thing).exists()
    }

    boolean canRead() {
        ((File) this.thing).canRead()
    }

    String toString() {
        if (ignore) {
            return "${depth}] ($type):$name [IGNORE]"
        } else {
            return "${depth}] ($type):$name"
        }

    }

    SolrInputDocument toSolrInputDocument() {
        SolrInputDocument sid = super.toSolrInputDocument()
        log.debug "FSFolder toSolrInputDocument(), from super: $sid"
        // todo -- is there anything else FSFolder specific to add??

        return sid
    }


    /**
     * populate the children of this folder
     * Assumes there could be a first pass crawl of folders only, so we make this an explicit call when/where desired
     * @param sid
     * @return
     */
    public int populateChildren(SolrInputDocument sid) {
        int ignoreFiles = 0
        int indexFiles = 0
        int ignoreFolders = 0
        int indexFolders = 0
        children.each {
            def childTypes = children.groupBy { it.type }
            def childIngores = children.groupBy { it.ignore }
            if (it.ignore) {
                if (it.type.containsIgnoreCase('file')) {
                    ignoreFiles++
                    sid.addField(SolrSystemClient.FLD_IGNORED_FILES, it.name)
                } else if (it.type.containsIgnoreCase('folder')) {
                    ignoreFolders++
                    sid.addField(SolrSystemClient.FLD_IGNORED_FOLDERS, it.name)
                } else {
                    log.warn "\t\tunknown savable object type: ${it.type}, processing it like a file"
                    ignoreFiles++
                    sid.addField(SolrSystemClient.FLD_IGNORED_FILES, it.name)
                }
            } else {
                if (it.type.containsIgnoreCase('file')) {
                    indexFiles++
                    sid.addField(SolrSystemClient.FLD_CHILD_FILENAMES, it.name)
                    sid.addField(SolrSystemClient.FLD_EXTENSION_SS, ((FSFile) it).extension)
                } else if (it.type.containsIgnoreCase('folder')) {
                    indexFiles++
                    sid.addField(SolrSystemClient.FLD_CHILD_DIRNAMES, it.name)
                } else {
                    log.warn "\t\tunknown savable object type: ${it.type}, processing it like a file"
                    ignoreFiles++
                    sid.addField(SolrSystemClient.FLD_IGNORED_FILES, it.name)
                }

            }
        }
        sid.addField(SolrSystemClient.FLD_IGNORED_FILES_COUNT, ignoreFiles)
        sid.addField(SolrSystemClient.FLD_IGNORED_FOLDERS_COUNT, ignoreFolders)
        sid.addField(SolrSystemClient.FLD_FILE_COUNT, indexFiles)
        sid.addField(SolrSystemClient.FLD_DIR_COUNT, indexFolders)
        return children.size()
    }


    /**
     * Get solr input docs for this object, and all its children -- convenience wrapper
     * @return list of solr input docs ready to be indexed in solr
     */
    List<SolrInputDocument> toSolrInputDocumentList() {
        List<SolrInputDocument> sidList = []
        // todo - add more FSFolder specific fields here
        SolrInputDocument sidFolder = super.toSolrInputDocument()
//        sidFolder.addField(SolrSystemClient.FLD_NAME_SIZE_S, "${name}:${size}")
        if (dedup) {
            log.debug "ensure dedupe is saved, and replaces name-size field"
        } else {
            log.warn "ensure dedupe is saved, and replaces name-size field"
        }

//        sidFolder.setField(SolrSystemClient.FLD_SUBDIR_COUNT, countSubdirs)
//        sidFolder.setField(SolrSystemClient.FLD_FILE_COUNT, countFiles)

        if (crawlName) {
            sidFolder.setField(SolrSystemClient.FLD_CRAWL_NAME, crawlName)
        }

//        if (ignoredFileNames) {
//            sidFolder.setField(SolrSystemClient.FLD_IGNORED_FILES + '_ss', ignoredFileNames.collect { it.name })
//            sidFolder.setField(SolrSystemClient.FLD_IGNORED_FILES + '_txt', ignoredFileNames.collect { it.name })
//            sidFolder.setField(SolrSystemClient.FLD_IGNORED_FILES_COUNT, ignoredFileNames.size())
//        }
//        if (ignoredDirectories) {
//            sidFolder.setField(SolrSystemClient.FLD_IGNORED_FOLDERS + '_ss', ignoredDirectories.collect { it.crawlName })
//            sidFolder.setField(SolrSystemClient.FLD_IGNORED_FOLDERS + '_txt', ignoredDirectories.collect { it.crawlName })
//            sidFolder.setField(SolrSystemClient.FLD_IGNORED_FOLDERS_COUNT, ignoredDirectories.size())
//        }
//        if (sameNameCount > 1) {
//            sidFolder.setField(SolrSystemClient.FLD_SAME_NAME_COUNT, sameNameCount)
//        }
//        if (childAssignedTypes) {
//            sidFolder.setField(SolrSystemClient.FLD_CHILDASSIGNED_TYPES, childAssignedTypes)
//        }

        sidList << sidFolder

        List<String> assignments = []
        children.each { SavableObject child ->
            SolrInputDocument sidFfs = child.toSolrInputDocument()
            sidList << sidFfs
        }

        return sidList
    }


    def addFolderDetails() {
        Map<String, Object> details = [:]
        File srcFolder = (File) this.thing

        if (!path) {
            path = srcFolder.absolutePath
            details.path = path
        }

        BasicFileAttributes attr = Files.readAttributes(srcFolder.toPath(), BasicFileAttributes.class);
        FileTime lastAccessTime = attr.lastAccessTime()
        lastAccessDate = new Date(lastAccessTime.toMillis())
        details.lastAccessDate = lastAccessDate

        FileTime lastModifyTime = attr.lastModifiedTime()
        lastModifiedDate = new Date(lastModifyTime.toMillis())
        details.lastModifiedDate = lastModifiedDate

        createdDate = new Date(attr.creationTime().toMillis())
        details.createdDate = createdDate
        FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(((File) this.thing).toPath(), FileOwnerAttributeView.class);
        owner = ownerAttributeView.getOwner();
        details.owner = owner

        return details
    }

    /**
     * helper method to load Directories and Files for this FSFolder object
     * it is expected that the constructors will flag ignorable objects appropriately
     * This is similar functionality to a crawler object building the children of the FSFolder, here partly for unit testing, partly to provide implementation flexibilty
     */
    List<SavableObject> buildChildrenList(Pattern ignoreFilesPattern = Constants.DEFAULT_FILENAME_PATTERNS[Constants.LBL_IGNORE], Pattern ignoreFoldersPattern = Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE]) {
        File folder = (File) thing
        if (children) {
            log.warn "Children property already initialized!!!  reseting children to empty list: $this"
            children = []
        } else {
            log.debug "Initialize children property... $thing"
            children = []
        }

        if (folder.exists()) {
            if (folder.canRead()) {
                int cnt = 0
                folder.eachFile { File file ->
                    if (file.exists()) {
                        if (file.canRead()) {
                            cnt++
                            if (file.isDirectory()) {
                                FSFolder fsFolder = new FSFolder(file, this, locationName, crawlName)
                                if (fsFolder.name ==~ ignoreFoldersPattern) {
                                    log.info "\t\t$cnt) ----- IGNORE FOLDER ----- ${this} has ignorable subsolder: $fsFolder"
                                    fsFolder.ignore = true
                                }
                                children << fsFolder
                            } else {
                                FSFile fsFile = new FSFile(file, this, locationName, crawlName)
                                if (file.name ==~ ignoreFilesPattern) {
                                    fsFile.ignore = true
                                    children << fsFile
                                    log.info "\t\t$cnt) ----- Ignoring ----- file: $file"
                                } else {
                                    log.debug "\t\t$cnt) adding file: $file"
                                    children << fsFile
                                }
                            }
                        } else {
                            log.warn "Cannot read file: $file"
                        }
                    } else {
                        log.warn "file: $file -- does not exist???"

                    }
                }
            } else {
                log.warn "\t\tCannot read folder: ${folder.absolutePath}"
            }
        } else {
            log.warn "\t\tFolder (${folder.absolutePath}) does not exist!!"
        }
        return children
    }
}
