package com.oconeco.models

import com.oconeco.analysis.BaseAnalyzer
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
    List<FolderFS> ignoredFolders = []

    /** in a given crawl, count 'duplicate' names as a metric for the anaylzer */
    int sameNameCount = 0
    public static final Pattern IGNORE_ALL = ~/.*/

//    List<String> systemFileType = []
//    List<String> folderPermissions = []
//    List filePermissions = []
//    List contentFiles = []
//    List supportFiles = []
//    List hiddenFiles = []
//    List tempFiles = []
//    List symLinks = []
//    List hardLinks = []
    FolderFS(String srcPath, int depth = 1, Pattern ignoreFilesPattern = null, Pattern ignoreSubdirectories = null) {
        this(new File(srcPath), depth, ignoreFilesPattern, ignoreSubdirectories)
        log.debug "convenience constructor taking a string path($srcPath) and calling proper constructor with file..."
    }


    FolderFS(File srcFolder, int depth = 1, Pattern ignoreFilesPattern = null, Pattern ignoreSubdirectories = null) {
        log.info "$depth) ${srcFolder.absolutePath}) FolderFS constructor"
        if (srcFolder?.exists()) {
            id = srcFolder.absolutePath
            me = srcFolder
            name = srcFolder.name
            type = TYPE
            long diskSpace = 0
            log.debug "\t\tFiltering folder with ignore pattern: $ignoreFilesPattern "
            srcFolder.eachFile { File f ->
                if (f.isFile()) {
//                    FileFS ffs = processFile(ignoreFilesPattern, f, depth)
                    FileFS ffs = new FileFS(f)
                    if(f.name ==~ ignoreFilesPattern) {
                        ignoredFiles << ffs
                    } else {
                        filesFS << ffs
                        diskSpace += f.size()
                    }
                    log.debug "\t\tProcessed file: $ffs"
                } else if (f.isDirectory()) {
                    processDirectory(f, ignoreSubdirectories, depth, ignoreFilesPattern, srcFolder)
                }
            }

            countFiles = filesFS.size()
            countSubdirs = subDirectories.size()
            countTotal = countFiles + countSubdirs
            this.size = diskSpace

        } else {
            throw new IllegalArgumentException("Sourc folder (${srcFolder.absolutePath}) does not exist!")
        }
    }


    public void processDirectory(File f, Pattern ignoreSubdirectories, int depth, Pattern ignoreFilesPattern, File srcFolder) {
        if (f.name ==~ ignoreSubdirectories) {
            log.info "\t\t\tIGNORING ++++FOLDER:$f because name matches ignore subdir pattern: $ignoreSubdirectories"
            FolderFS ffs = new FolderFS(f, depth + 1, ignoreFilesPattern, IGNORE_ALL)
            ignoredFolders << ffs
        } else {
            log.debug "FolderFS ($srcFolder) processing subdir:$f "
            subDirectories << f
//            FolderFS ffs = new FolderFS(f, depth + 1, ignoreFilesPattern, ignoreSubdirectories)
//            childFolders << ffs
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

    def analyze(BaseAnalyzer baseAnalyzer) {
        log.debug "Analyze Folder: ${((String) this).padRight(30)}  --->  ${this.me.absolutePath}"
        if (!baseAnalyzer instanceof FolderAnalyzer) {
            throw new IllegalArgumentException("Analyzer not instance of Folder Analyzer, throwing error...")
        }
        FolderAnalyzer analyzer = baseAnalyzer
        childAssignedTypes = analyzer.assignFileTypes(filesFS)
//        def archiveResults = analyzer.analyzeArchives(filesFS)

        List<String> folderLabels = analyzer.assignFolderType(this)

        return childAssignedTypes + folderLabels      // todo-- improve return value
    }

    String toString() {
        return "($type):$name"
    }

    List<SolrInputDocument> toSolrInputDocumentList() {
        List<SolrInputDocument> sidList = []

        SolrInputDocument sid = super.toSolrInputDocument()
        sid.setField(SolrSaver.FLD_SUBDIR_COUNT, countSubdirs)
        sid.setField(SolrSaver.FLD_FILE_COUNT, countFiles)

        if (ignoredFiles) {
            sid.setField(SolrSaver.FLD_IGNORED_FILES + '_ss', ignoredFiles.collect{ it.name})
            sid.setField(SolrSaver.FLD_IGNORED_FILES + '_txt', ignoredFiles.collect{ it.name})
            sid.setField(SolrSaver.FLD_IGNORED_FILES_COUNT, ignoredFiles.size())
        }
        if (ignoredFolders) {
            sid.setField(SolrSaver.FLD_IGNORED_FOLDERS + '_ss', ignoredFolders.collect{ it.name})
            sid.setField(SolrSaver.FLD_IGNORED_FOLDERS + '_txt', ignoredFolders.collect{ it.name})
            sid.setField(SolrSaver.FLD_IGNORED_FOLDERS_COUNT, ignoredFolders.size())
        }
        if (sameNameCount > 1){
            sid.setField(SolrSaver.FLD_SAME_NAME_COUNT, sameNameCount)
        }
        if(childAssignedTypes){
            sid.setField(SolrSaver.FLD_CHILDASSIGNED_TYPES, childAssignedTypes)
        }

        sidList << sid

        List<String> assignments = []
        filesFS.each { FileFS ffs ->
            SolrInputDocument sidFfs = ffs.toSolrInputDocument()
            sidList << sidFfs
        }

        return sidList
    }
}
