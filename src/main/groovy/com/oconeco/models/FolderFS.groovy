package com.oconeco.models

import com.oconeco.analysis.BaseAnalyzer
import com.oconeco.analysis.FileAnalyzer
import com.oconeco.analysis.FolderAnalyzer
import com.oconeco.persistence.SolrSaver
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
class FolderFS extends BaseObject {
    public static final String TYPE = 'Folder'
    Logger log = Logger.getLogger(this.class.name);

    /** basic list of files in this dir */
    List<FileFS> filesFS = []
    /** basic list of sub directories in this dir */
//    List<File> subDirectories = []

    // numerics to help with (later) analysis....
    Long countFiles
    Long countSubdirs
    Long countTotal

    // summary/searchable metadata (might maintain useful order, but no promises)
    List<Date> fileDates = []
    List<Long> filesSizes = []
    // more useful metadata
    FolderFS parentFolder = null
    List<File> subDirectories = []
    List<FolderFS> childFolders = []
    List<String> childAssignedTypes = []
    List<FileFS> ignoredFiles = []

    // todo -- do we need/want both Files and FolderFSs??
    List<FolderFS> ignoredFolders = []
    List<File> ignoredDirectories = []

    /** in a given crawl, count 'duplicate' names as a metric for the anaylzer */
    int sameNameCount = 0
    public static final Pattern IGNORE_ALL = ~/.*/
    public static final String

    /**
     * over-engineered constructor-based crawling, not my favorite idea....
     * @param srcPath
     * @param depth
     * @param ignoreFilesPattern
     * @param ignoreSubdirectories
     * @param recurse
     * @deprecated I don't think the constructor approach is right, deprecating in favor of more explicit (and less surprising) method based approach (the original approach)
     */
    FolderFS(String srcPath, int depth = 1, Pattern ignoreFilesPattern = null, Pattern ignoreSubdirectories = null, Boolean recurse = false) {
        this(new File(srcPath), depth, ignoreFilesPattern, ignoreSubdirectories, recurse)
//        log.debug "convenience constructor taking a string path($srcPath) and calling proper constructor with file..."
    }


    /**
     * This constructor has the ability to recurse through the filesystem and build tree
     * @param srcFolder
     * @param depth
     * @param ignoreFilesPattern
     * @param ignoreSubdirectories
     * @deprecated I don't think the constructor approach is right, deprecating in favor of more explicit (and less surprising) method based approach (the original approach)
     */
    FolderFS(File srcFolder, int depth = 1, Pattern ignoreFilesPattern = null, Pattern ignoreSubdirectories = null, boolean recurse = false, FolderFS parentFolderFS = null) {
        log.debug '\t' * depth + "$depth) ${srcFolder.absolutePath}) FolderFS constructor"
        if (srcFolder?.exists()) {
            id = srcFolder.absolutePath
            me = srcFolder
            name = srcFolder.name
            type = TYPE
            long diskSpace = 0
            lastModifiedDate = new Date(srcFolder.lastModified())
            this.depth = depth
            log.debug "\t\tFiltering folder with ignore pattern: $ignoreFilesPattern "

            countFiles = filesFS.size()
            countSubdirs = subDirectories.size()
            countTotal = countFiles + countSubdirs
            this.size = diskSpace

        } else {
            throw new IllegalArgumentException("Sourc folder (${srcFolder.absolutePath}) does not exist!")
        }
    }


    public void processDirectory(File f, int depth, Pattern ignoreSubdirectories, Pattern ignoreFilesPattern, File srcFolder, boolean recurse = false, FolderFS parentFS = null) {
        if (f.name ==~ ignoreSubdirectories) {
            log.info "\t\t\tIGNORING ++++FOLDER:$f because name matches ignore subdir pattern: $ignoreSubdirectories"
//            FolderFS ffs = new FolderFS(f, depth + 1, ignoreFilesPattern, IGNORE_ALL)
//            ignoredFolders << ffs
            ignoredDirectories << f
        } else {
            log.debug "FolderFS ($srcFolder) processing subdir:$f "
            subDirectories << f
            if (recurse) {
                int nextDepth = depth + 1
                log.debug "recurse into subfolder: $f -- depth:($nextDepth}"
                this.parentFolder = parentFS
                FolderFS ffs = new FolderFS(f, nextDepth, ignoreFilesPattern, ignoreSubdirectories, recurse, this)
                childFolders << ffs
            } else {
                log.debug "param 'recurse' ($recurse) not true, skipping recursive tree crawling"
            }
        }
    }


    /**
     * extracted method to do the actual processing
     *
     * @param ignoreFiles
     * @param f
     */
//    public void processFile(File f, int depth = 1) {
//
//        FileFS fileFS = new FileFS(f)
//        filesFS << fileFS
//
//        fileDates << f.lastModified()
//        filesSizes << f.size()
//
//        // todo detect/process links (hard or symbolic), also sockets, and other outliers
//    }

/*    def linkFolders(FolderFS parentFolder) {
        log.info "linkFolders(parentFolder:${parentFolder.me.name})"
        if (parentFolder) {
            log.info "\t\tLink parent and child/this folder... "
            parentFolder.childFolders << this
            if (!parentFolder.depth > 0) {
                log.warn "Parent (${parentFolder.me} depth was 0, set to 1 (is this the start of the crawl?)"
                parentFolder.depth = 1
            }
            this.parentFolder = parentFolder
            this.depth = parentFolder.depth + 1
        }
    }*/

    List<String> analyze(BaseAnalyzer analyzer) {
        log.debug "Analyze Folder: ${((String) this).padRight(30)}  --->  ${this.me.absolutePath}"
        if (!folderAnalyzer instanceof FolderAnalyzer) {
            throw new IllegalArgumentException("Analyzer not instance of Folder Analyzer, throwing error...")
        }
        FolderAnalyzer folderAnalyzer = (FolderAnalyzer) analyzer

        List<String> labels = folderAnalyzer.assignFolderLabels(this)
        return labels
    }

    List<String> analyze(FolderAnalyzer folderAnalyzer, FileAnalyzer fileAnalyzer = null) {
        log.debug "Analyze Folder: ${((String) this).padRight(30)}  --->  ${this.me.absolutePath}"
        List<String> folderLabels = []  // analyze(folderAnalyzer)

        if (fileAnalyzer) {
            List<String> labels = fileAnalyzer.analyze(filesFS)
            folderLabels.addAll(labels)
        }
//        return childAssignedTypes + folderLabels      // todo-- improve return value
        return folderLabels
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
            sidFolder.setField(SolrSaver.FLD_CRAWL_NAME, crawlName)
        }

        if (ignoredFiles) {
            sidFolder.setField(SolrSaver.FLD_IGNORED_FILES + '_ss', ignoredFiles.collect { it.name })
            sidFolder.setField(SolrSaver.FLD_IGNORED_FILES + '_txt', ignoredFiles.collect { it.name })
            sidFolder.setField(SolrSaver.FLD_IGNORED_FILES_COUNT, ignoredFiles.size())
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
        filesFS.each { FileFS ffs ->
            SolrInputDocument sidFfs = ffs.toSolrInputDocument(crawlName)
            sidList << sidFfs
        }

        return sidList
    }


    /**
     * recursing method to gather up all (current & descendant) folders in a flat list (i.e. to save to solr)
     * @return flattened list of Folder objects
     */
    List<FolderFS> getAllSubFolders() {
        log.debug '\t' * depth + "${name} ($depth) get subfolders: ${childFolders}"
        List<FolderFS> allFolders = []
        this.childFolders.each { FolderFS subFolder ->
            allFolders << subFolder
            List<FolderFS> foldersFS = subFolder.getAllSubFolders()
            allFolders.addAll(foldersFS)
        }
//        allFolders << this
        return allFolders
    }
}
