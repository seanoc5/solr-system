package com.oconeco.models

import com.oconeco.analysis.BaseAnalyzer
import com.oconeco.analysis.FileAnalyzer
import com.oconeco.analysis.FolderAnalyzer
import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSaver
import org.apache.log4j.Logger
import org.apache.solr.common.SolrInputDocument

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
    /** the actual filesystem object for reference (user/permission info as available) */
//    File thisFile

    /** basic list of files in this dir */
    List<FSFile> fsFiles = []

    // numerics to help with (later) analysis....
    Long countFiles
    Long countSubdirs
    Long countTotal

//    // summary/searchable metadata (might maintain useful order, but no promises)
    List<Date> fileDates = []
    List<Long> filesSizes = []
//
//    // more useful metadata
    FSFolder parentFSFolder = null
    List<FSFolder> childFsFolders = []
    List<String> childAssignedTypes = []
    List<String> ignoredFileNames = []
//
//    // todo -- do we need/want both Files and FolderFSs??
    List<FSFolder> ignoredFsFolders = []
//    List<File> ignoredDirectories = []
//
//    /** in a given crawl, count 'duplicate' names as a metric for the anaylzer */
//    int sameNameCount = 0
//    public static final String

    /**
     * TJust build a filesystem folder (wi
     * @param id
     * @param depth
     * @param ignoreFilesPattern
     * @param ignoreSubdirectories
     */
    FSFolder(File folder) {
        super(folder)
        name = folder.name
//        parent.sourceName = 'unknown'
//        addFolderDetails(sourceName, folder)
    }

    /**
     * This constructor has the ability to recurse through the filesystem and build tree
     * @param id
     * @param sourceName
     */
//    FSFolder(File thing, String sourceName) {
//        super(thing, sourceName)
//        addFolderDetails(sourceName, folder)
//    }

    /**
     * This constructor has the ability to recurse through the filesystem and build tree
     * @param id
     * @param sourceName
     * @param depth
     */
//    FSFolder(File thing, String sourceName, Integer depth) {
//        super(thing, sourceName, depth)
//        addFolderDetails(sourceName, folder)
//    }


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
            sidFolder.setField(SolrSaver.FLD_IGNORED_FOLDERS + '_ss', ignoredDirectories.collect { it.name })
            sidFolder.setField(SolrSaver.FLD_IGNORED_FOLDERS + '_txt', ignoredDirectories.collect { it.name })
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
        fsFiles.each { FSFile ffs ->
            SolrInputDocument sidFfs = ffs.toSolrInputDocument(crawlName)
            sidList << sidFfs
        }

        return sidList
    }


    def addFolderDetails(File folder) {
        id = srcFolder.absolutePath
        me = srcFolder
        name = srcFolder.name
        type = TYPE
        long diskSpace = 0
        lastModifiedDate = new Date(srcFolder.lastModified())
        this.depth = depth
        log.debug "\t\tFiltering folder with ignore pattern: $ignoreFilesPattern "

        countFiles = fsFiles.size()
        countSubdirs = childFsFolders.size()
        countTotal = countFiles + countSubdirs
        this.size = diskSpace
    }

}
