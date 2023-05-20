package com.oconeco.analysis

import com.oconeco.helpers.Constants
import com.oconeco.models.BaseObject
import com.oconeco.models.FileFS
import com.oconeco.models.FolderFS
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveException
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.log4j.Logger

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

class FolderAnalyzer extends BaseAnalyzer {
    Logger log = Logger.getLogger(this.class.name);


    ConfigObject config

    List<String> extensions = []
    Map<String, Pattern> fileNamePatterns = Constants.DEFAULT_FILENAME_PATTERNS
    Map<String, Pattern> folderNamePatterns = Constants.DEFAULT_FOLDERNAME_PATTERNS

    ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();

    FolderAnalyzer() {
        log.info "Blank constructor, using defaults..."
    }

    FolderAnalyzer(ConfigObject config) {
        this.config = config
        if (config.namePatterns?.folders) {
            folderNamePatterns = config.namePatterns.folders
//            log.info "\t\tLoading Folder analzer of foldernames from 'config.namePatterns.files': ${folderNamePatterns.collect { '\n\t\t' + it }}"
            log.info "\t\tLoading Folder analzer of foldernames from 'config.namePatterns.files': ${fileNamePatterns.keySet()}"
        }
        if (config.namePatterns?.files) {
            fileNamePatterns = config.namePatterns.files
//            log.info "\t\tLoading Folder analzer of filenames from 'config.namePatterns.files': ${fileNamePatterns.collect { '\n\t\t' + it }}"
            log.info "\t\tLoading Folder analzer of filenames from 'config.namePatterns.files': ${fileNamePatterns.keySet()}"
        }

    }


    FolderAnalyzer(Map fnPatterns) {
        folderNamePatterns = fnPatterns
    }

    List<String> analyze(BaseObject object) {
        FolderFS ffs = (FolderFS)object
        List<String> labels = []
        folderNamePatterns.each { String label, Pattern pattern ->
            if (ffs.name ==~ pattern) {
                log.info "\t\tASSIGNING label: $label -- $ffs"
                labels << label
                ffs.assignedTypes << label
            } else {
                log.debug "\\t\t ($label) no match on pattern: $pattern -- $ffs"
            }
        }
        if (labels) {
            if (labels.size() > 1) {
                log.debug " \t\t........ More that one label?? $labels -- $ffs"
            }
        } else {
            log.debug "\t\tNO LABEL folderType assigned?? $ffs  --> ${((FolderFS) ffs).me.absolutePath}"
            ffs.assignedTypes << Constants.LBL_UNKNOWN
        }
        return labels
    }


    /**
     * iterate through files, and apply file type labels (from map of patterns...?)
     * @param files
     * @return list of types found, plus we updated the sourcec list (non-FP friendly...)
     */
    List<String> assignFileTypes(List<FileFS> files) {
        List<String> allLabels = []
        files.each { FileFS fileFS ->
            log.debug "\t\tAssign file type: $fileFS"
            String fname = ((File) fileFS.me).name
//            List<String> labels = []
            fileNamePatterns.each { String label, Pattern pattern ->
                Matcher m = (fname =~ pattern)
                if (m.matches()) {
                    log.debug "\t\tASSIGNING label: $label -- $fileFS :: matcher: ${m[0]}"
                    allLabels << label
                    fileFS.assignedTypes << label
                } else {
                    log.debug "\\t\t ($label) no match on pattern: $pattern -- $fileFS"
                }
            }
            if (fileFS.assignedTypes) {
                if (fileFS.assignedTypes.size() > 1) {
                    log.debug "More that one label?? ${fileFS.assignedTypes} -- $fileFS"
                }
            } else {
                log.debug "\t\tNO LABEL assigned?? $fileFS  --> ${((File) fileFS.me).absolutePath}"
                fileFS.assignedTypes << Constants.LBL_UNKNOWN
                allLabels << Constants.LBL_UNKNOWN
            }
        }
        return allLabels
    }


    /**
     * analyze an archive (zip, tarball) file
     * todo -- more code/functionality
     * @param filesFs
     * @return
     */
    def analyzeArchives(List<FileFS> filesFs) {

        filesFs.each { FileFS ffs ->
            if (ffs.isArchive(ffs)) {
                File f = ffs.me
                log.info "\t\tFound archive: $ffs -- crawl through and add children to this folder"
                ArchiveInputStream archiveInputStream = null
                BufferedInputStream inputStream = f.newInputStream()
                try {
                    if (ffs.extension == 'gz') {
                        log.info "\t\tfound gzipped archive (i.e. tar.gz), adding GzipCompressorInputStream to stream..."
                        GzipCompressorInputStream gzi = new GzipCompressorInputStream(inputStream);
                        archiveInputStream = new TarArchiveInputStream(gzi)
                        log.info "Archive stream: $archiveInputStream"
                    } else {
                        archiveInputStream = archiveStreamFactory.createArchiveInputStream(inputStream)
                    }
                    log.info "Archive input: $archiveInputStream"
                    ArchiveEntry archiveEntry
                    while ((archiveEntry = archiveInputStream.getNextEntry()) != null) {
                        // todo -- add more code/functionality
                        log.info "entry: $archiveEntry"
                    }
                } catch (ArchiveException ae) {
                    log.warn "Problem with file: $f -- error: $ae"
                }


            } else {
                log.debug "Skip unarchiving non-archive file: $filesFs"
            }
        }
    }


//    @Override
//    List<String> analyze(BaseObject object) {
//        return null
//    }
}
