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
        this(srcFolder, locationName, crawlName )
        this.depth = parent ? parent.depth+1 : 1
        this.parent = parent
        this.parentId = parent.id
    }


    /**
     * This constructor has the ability to recurse through the filesystem and build tree
     * @param id
     * @param locationName
     */
    FSFolder(File srcFolder, String locationName, String crawlName = Constants.LBL_UNKNOWN, Integer depth = null) {
        super(srcFolder, locationName, crawlName, depth)
        osName = System.getProperty("os.name")      // todo - time this call, is it significant overhead?
        if (srcFolder.isHidden()) {
            log.info "\t\t~~~~processing hidden folder: $srcFolder"
            hidden = true
        }
        if (srcFolder.exists() && srcFolder.canRead() && srcFolder.canExecute()) {
            type = TYPE
            name = srcFolder.getName()
            size = srcFolder.size()
            path = srcFolder.absolutePath

            // todo -- revisit replacing backslashes with forward slashes--just cosmetics? avoid double-backslashes in path fields for windows machines
            id = SavableObject.buildId(locationName, path.replaceAll('\\\\', '/'))
            if(srcFolder?.parentFile) {
                // todo -- check if such a parent is (or will be) saved? currently just saving the id of a parent without caring if it does or will exist in solr
                parentId = SavableObject.buildId(locationName, srcFolder.parent.replaceAll('\\\\', '/'))
            }

            dedup = buildDedupString()

            // todo -- revisit to cost of getting the dir size, for a large dir it is many seconds (a minute or two??)
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
/*
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
*/


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
    List<SavableObject> buildChildrenList(Pattern ignoreFilesPattern = Constants.DEFAULT_FILENAME_PATTERNS[Constants.LBL_IGNORE],
                                          Pattern ignoreFoldersPattern = Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE]) {
        File folder = (File) thing
        SavableObject object
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
                                 object = new FSFolder(file, this, locationName, crawlName)
                                if (object.name ==~ ignoreFoldersPattern) {
                                    log.info "\t\t$cnt) ----- mark 'ignore' CHILD folder ----- ${this} has ignorable subsolder: $object"
                                    object.ignore = true
                                } else {
                                    log.debug "\t\t--NOT IGNORED FOLDER: $object"
                                }
                                children << object

                            } else {
                                object = new FSFile(file, this, locationName, crawlName)
                                if (file.name ==~ ignoreFilesPattern) {
                                    object.ignore = true
                                    children << object
                                    log.info "\t\t[file:$cnt] ----- Ignoring ----- file: $file"
                                } else {
                                    log.debug "\t\t$cnt) adding file: $file"
                                    children << object
                                }
                            }
                        } else {
                            log.warn "Cannot read file: $file"
                        }
                    } else {
                        log.warn "file: $file -- does not exist???"
                    }
                }           // end for eachfile

            } else {
                log.warn "\t\tCannot read folder: ${folder.absolutePath}"
            }
        } else {
            log.warn "\t\tFolder (${folder.absolutePath}) does not exist!!"
        }
        if(!object || object.id ==null){
            log.warn "Saved object: $object has no id"
        }
        return children
    }

    List<FSFile> gatherArchiveFiles(def ignoreArchives){
        List<FSFile> archFiles = []
        if(this.children == null){
            log.warn "Children of FSFolder ($this) is null, not initialized!!?!"
        } else {
            archFiles = children.findAll {
                boolean gather = false
                if(it.type==FSFile.TYPE && ((FSFile)it).isArchive() ){
                    if(ignoreArchives){
                         def matches = it.name ==~ ignoreArchives      // we have an ignore pattern, check to see if we should ignore
                        gather = !matches
                    } else {
                        // no ignoreArchives pattern, so default true
                        gather = true
                    }
                } else {
                    log.debug "not archive file"
                    gather = false
                }
                return gather
            }
            log.debug "${toString()}: arch files size: ${archFiles.size()}"
        }
        return archFiles
    }


    String buildId() {
        buildId(locationName, path)
    }
}
