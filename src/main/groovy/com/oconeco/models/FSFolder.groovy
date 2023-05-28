package com.oconeco.models

import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSaver
import org.apache.commons.io.FileUtils
import org.apache.log4j.Logger
import org.apache.solr.common.SolrInputDocument

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
 * structure for handling information about a folder from the FS (filesystem),
 * handles both basic information from the filesystem, and analysis results from some process
 * todo -- switch to NIO Files and Paths
 */
class FSFolder extends SavableObject {
    public static final String TYPE = 'Folder'
    Logger log = Logger.getLogger(this.class.name);


    FSFolder(File srcFolder, String locationName, String crawlName = Constants.LBL_UNKNOWN, SavableObject parent) {
        this(srcFolder, locationName, crawlName, parent.depth)
        this.parent = parent
    }


    /**
     * This constructor has the ability to recurse through the filesystem and build tree
     * @param id
     * @param locationName
     */
    FSFolder(File srcFolder, String locationName, String crawlName = Constants.LBL_UNKNOWN, Integer depth = null) {
        super(srcFolder, locationName, crawlName, depth)
        type = TYPE
        id = this.locationName + ':' + srcFolder.absolutePath
        name = srcFolder.getName()
        size = srcFolder.size()
        path = srcFolder.absolutePath

        size = FileUtils.sizeOfDirectory(srcFolder)
        uniquifier = buildUniqueString()

    }

    boolean exists() {
        ((File)this.thing).exists()
    }

    boolean canRead() {
        ((File)this.thing).canRead()
    }

    String toString() {
        return "${depth}] ($type):$name"
    }

    List<SolrInputDocument> toSolrInputDocumentList(String crawlName = null) {
        List<SolrInputDocument> sidList = []

        SolrInputDocument sidFolder = super.toSolrInputDocument()
        sidFolder.setField(SolrSaver.FLD_SUBDIR_COUNT, countSubdirs)
        sidFolder.setField(SolrSaver.FLD_FILE_COUNT, countFiles)

//        def updateTime =

        if (crawlName) {
            sidFolder.setField(Constants.FLD_CRAWL_NAME, crawlName)
        }

        if (ignoredFileNames) {
            sidFolder.setField(SolrSaver.FLD_IGNORED_FILES + '_ss', ignoredFileNames.collect { it.name })
            sidFolder.setField(SolrSaver.FLD_IGNORED_FILES + '_txt', ignoredFileNames.collect { it.name })
            sidFolder.setField(SolrSaver.FLD_IGNORED_FILES_COUNT, ignoredFileNames.size())
        }
        if (ignoredDirectories) {
            sidFolder.setField(SolrSaver.FLD_IGNORED_FOLDERS + '_ss', ignoredDirectories.collect { it.crawlName })
            sidFolder.setField(SolrSaver.FLD_IGNORED_FOLDERS + '_txt', ignoredDirectories.collect { it.crawlName })
            sidFolder.setField(SolrSaver.FLD_IGNORED_FOLDERS_COUNT, ignoredDirectories.size())
        }
        if (sameNameCount > 1) {
            sidFolder.setField(SolrSaver.FLD_SAME_NAME_COUNT, sameNameCount)
        }
        if (childAssignedTypes) {
            sidFolder.setField(SolrSaver.FLD_CHILDASSIGNED_TYPES, childAssignedTypes)
        }
        sidFolder.addField(SolrSaver.FLD_NAME_SIZE_S, "${name}:${size}")

        sidList << sidFolder

        List<String> assignments = []
        children.each { def child ->
            SolrInputDocument sidFfs = child.toSolrInputDocument(crawlName)
            sidList << sidFfs
        }

        return sidList
    }


    def addFolderDetails() {
        File srcFolder = (File) this.thing

        if(!path) {
            path = srcFolder.absolutePath
        }

        BasicFileAttributes attr = Files.readAttributes(srcFolder.toPath(), BasicFileAttributes.class);
        FileTime lastAccessTime = attr.lastAccessTime()
        lastAccessDate = new Date(lastAccessTime.toMillis())
        FileTime lastModifyTime = attr.lastModifiedTime()
        lastModifiedDate = new Date(lastModifyTime.toMillis())
        createdDate = new Date(attr.creationTime().toMillis())
        FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
        owner = ownerAttributeView.getOwner();


//        countFiles = fsFiles.size()
//        countSubdirs = childFsFolders.size()
//        countTotal = countFiles + countSubdirs
    }

    // todo -- move this functionality out to Crawler...?
/*
    List<SavableObject> buildChildrenList(Pattern ignoreFilesPattern = Constants.DEFAULT_FILENAME_PATTERNS[Constants.LBL_IGNORE]) {
        File folder = (File) thing
        children = []
//        childFsFolders = []
        if (folder.exists()) {
            if (folder.canRead()) {
//                countSubdirs = 0
//                countFiles = 0
//                countTotal = 0
                folder.eachFile { File file ->
                    countTotal++
                    if (file.isDirectory()) {
                        FSFolder fsFolder = new FSFolder(file, locationName, crawlName, this.depth + 1)
                        log.info "\t\t$countTotal) more code here to track directories, ignored or followed...."
//                        countSubdirs++
                        children << fsFolder
                    } else {
                        if (file.name ==~ ignoreFilesPattern) {
                            ignoredFileNames << file.name
//                            countIgnoredFiles++
                            log.debug "Ignoring file: $file"
                        } else {

                            FSFile fsFile = new FSFile(file, locationName, this.crawlName, this.depth)
                            fsFile.parent = this.id

                            children << fsFile

//                            fileDates << file.lastModified()
//                            filesSizes << file.size()

                        }
                    }
                }
//                this.countFiles = children.size()
//                this.countIgnoredFiles = ignoredFileNames.size()
//                this.countIgnoredSubDirs = ignoredDirectories.size()
            } else {
                log.warn "\t\tCannot read folder: ${folder.absolutePath}"
            }
        } else {
            log.warn "\t\tFolder (${folder.absolutePath}) does not exist!!"
        }
        return children
    }
*/
}
