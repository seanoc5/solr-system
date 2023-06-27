package com.oconeco.models

import com.oconeco.helpers.Constants
import org.apache.commons.io.FileUtils
import org.apache.log4j.Logger
import org.apache.solr.common.SolrInputDocument

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
class FSFolder extends FSObject {
    Logger log = Logger.getLogger(this.class.name);
    public static final String TYPE = 'Folder'


    FSFolder(File srcFolder, SavableObject parent, String locationName, String crawlName) {
        super(srcFolder, parent, locationName, crawlName)

        if (srcFolder.exists()) {
            type = TYPE
//            Path nioPath = srcFolder.toPath()

            if (srcFolder.canExecute() && srcFolder.canRead()) {
                size = FileUtils.sizeOf(srcFolder)
                log.debug "directory size: $size"
            } else {
                log.warn "Cannot execute/read folder: $srcFolder"
                size = srcFolder.size()
            }

            dedup = buildDedupString()

            // todo -- revisit replacing backslashes with forward slashes--just cosmetics? avoid double-backslashes in path fields for windows machines
            if (parent) {
                if (parent.id) {
                    log.debug "\t\tGetting parent id from parent Object ($parent) for this:$this"
                    parentId = parent.id
                } else {
                    log.warn "FSFolder ($this) has a parent ($parent) BUT that has no id!!!? (${parent.id}) -- that makes no sense!!"
                }
            } else if (srcFolder.parentFile) {
                // todo -- check if such a parent is (or will be) saved? currently just saving the id of a parent without caring if it does or will exist in solr
                String p = srcFolder.parent
                if (osName == null) {
                    p = p.replaceAll('\\\\', '/')
                } else if (osName?.contains('Windows')) {
                    p = p.replaceAll('\\\\', '/')
                } else {
                    if(p.contains('\\')){
                        log.warn "Path has a backslash ($p) -- is this a problem for solr searching??? ($this)"
                    }
                    log.debug "\t\tPath for os ($osName) does not need backslash replacement (??)"
                }

                parentId = SavableObject.buildId(locationName, p)
            }


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
/*
    List<SolrInputDocument> toSolrInputDocumentList() {
        List<SolrInputDocument> sidList = []
        // todo - add more FSFolder specific fields here
        SolrInputDocument sidFolder = super.toSolrInputDocument()
//        sidFolder.addField(SolrSystemClient.FLD_NAME_SIZE_S, "${name}:${size}")
        if (dedup) {
            log.debug "ensure dedupe is saved, and replaces name-size field"
        } else {
            log.warn "no dedupe: $this"
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
*/


    /**
     * helper method to load Directories and Files for this FSFolder object
     * it is expected that the constructors will flag ignorable objects appropriately
     * This is similar functionality to a crawler object building the children of the FSFolder, here partly for unit testing, partly to provide implementation flexibilty
     * @deprecated use Crawler to do this
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
                log.debug "\t\tstart buildChildrenList ($folder)"
                int cnt = 0
                folder.eachFile { File file ->
                    if (file.exists()) {
                        if (file.canRead()) {
                            cnt++
                            if (file.isDirectory()) {
                                object = new FSFolder(file, this, locationName, crawlName)
                                if (object.name ==~ ignoreFoldersPattern) {
                                    log.debug "\t\t$cnt) ----- mark 'ignore' CHILD folder ----- ${this} has ignorable subsolder: $object"
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
                                    log.debug "\t\t[file:$cnt] ----- Ignoring ----- file: $file"
                                } else {
                                    log.debug "\t\t$cnt) adding file: $file"
                                    children << object
                                }
                            }
                        } else {
                            log.warn "Cannot read file: $file (file permissions??)"
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
        if (!object) {
            log.warn "Saved object: $object is missing/null"
        } else if (object.id == null) {
            log.warn "Saved object: $object has no id"
        }
        return children
    }

/*
    List<FSFile> gatherArchiveFiles(def ignoreArchives) {
        List<FSFile> archFiles = []
        if (this.children == null) {
            log.warn "Children of FSFolder ($this) is null, not initialized!!?!"
        } else {
            archFiles = children.findAll {
                boolean gather = false
                if (it.type == FSFile.TYPE && ((FSFile) it).isArchive()) {
                    if (ignoreArchives) {
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
*/


//    String buildId() {
//        buildId(locationName, path)
//    }
}
