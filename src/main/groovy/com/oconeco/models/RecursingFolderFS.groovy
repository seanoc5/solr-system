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
class RecursingFolderFS extends FolderFS {
    public static final String TYPE = 'Folder'
    Logger log = Logger.getLogger(this.class.name);


    RecursingFolderFS(File srcFolder, int depth = 1, Pattern ignoreFilesPattern = null, Pattern ignoreSubdirectories = null) {
        super(srcFolder, depth, ignoreFilesPattern, ignoreSubdirectories)

        log.info "FolderFS [$depth] ::::  (srcFolder:${srcFolder.absolutePath}) constructor"
        if (srcFolder?.exists()) {
            id = srcFolder.absolutePath
            me = srcFolder
            name = srcFolder.name
            type = TYPE
            long diskSpace = 0
            log.debug "\t\tFiltering folder with ignore pattern: $ignoreFilesPattern "
            srcFolder.eachFile { File f ->
                if (f.isFile()) {
                    if (ignoreFilesPattern && f.name ==~ ignoreFilesPattern) {
                        FileFS ffs = new FileFS(f, depth)
                        ignoredFiles << ffs
                        log.info "\t\t\tIGNORING ____file: $f because it matches ignoreFiles pattern: $ignoreFilesPattern"
                    } else {
                        processFile(f, depth)
                        diskSpace += f.size()

                    }
                } else if (f.isDirectory()) {
                    if (f.name ==~ ignoreSubdirectories) {
                        log.info "\t\t\tIGNORING ++++FOLDER:$f because name matches ignore subdir pattern: $ignoreSubdirectories"
                        RecursingFolderFS ffs = new RecursingFolderFS(f, depth + 1, ignoreFilesPattern, IGNORE_ALL)
                        ignoredFolders << ffs
                    } else {
                        log.debug "FolderFS ($srcFolder) processing subdir:$f "
                        RecursingFolderFS ffs = new RecursingFolderFS(f, depth + 1, ignoreFilesPattern, ignoreSubdirectories)
                        childFolders << ffs
                    }
                }
            }

            countFiles = filesFS.size()
            countSubdirs = childFolders.size()
            countTotal = countFiles + countSubdirs
            this.size = diskSpace

        } else {
            throw new IllegalArgumentException("Sourc folder (${srcFolder.absolutePath}) does not exist!")
        }
    }


    /**
     * extracted method to do the actual processing
     *
     * @param ignoreFiles
     * @param f
     */
    public void processFile(File f, int depth = 1) {

        FileFS fileFS = new FileFS(f)
        filesFS << fileFS

        fileDates << f.lastModified()
        filesSizes << f.size()

        // todo detect/process links (hard or symbolic), also sockets, and other outliers
    }

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
        analyzer.assignFolderType(this)
        def foo = analyzer.assignFileTypes(filesFS)

        return foo      // todo-- improve return value
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
            sid.setField(SolrSaver.FLD_IGNORED_FILES + '_ss', ignoredFiles.collect { it.name })
            sid.setField(SolrSaver.FLD_IGNORED_FILES + '_txt', ignoredFiles.collect { it.name })
            sid.setField(SolrSaver.FLD_IGNORED_FILES_COUNT, ignoredFiles.size())
        }
        if (ignoredFolders) {
            sid.setField(SolrSaver.FLD_IGNORED_FOLDERS + '_ss', ignoredFolders.collect { it.name })
            sid.setField(SolrSaver.FLD_IGNORED_FOLDERS + '_txt', ignoredFolders.collect { it.name })
            sid.setField(SolrSaver.FLD_IGNORED_FOLDERS_COUNT, ignoredFolders.size())
        }
        if (sameNameCount > 1) {
            sid.setField(SolrSaver.FLD_SAME_NAME_COUNT, sameNameCount)
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
