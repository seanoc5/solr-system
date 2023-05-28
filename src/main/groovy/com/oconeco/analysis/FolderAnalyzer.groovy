package com.oconeco.analysis

import com.oconeco.helpers.Constants
import com.oconeco.models.SavableObject
import com.oconeco.models.FSFile
import com.oconeco.models.FSFolder
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

    /**
     * @deprecated ?? is there a real reason to track this, or just accept it as a constructor arg....
     */
    ConfigObject config

//    List<String> extensions = []
//    Map<String, Pattern> fileNamePatterns = Constants.DEFAULT_FILENAME_PATTERNS
    Map<String, Pattern> namePatternsMap = Constants.DEFAULT_FOLDERNAME_PATTERNS

    ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();

    FolderAnalyzer() {
        this(Constants.DEFAULT_FILENAME_PATTERNS, Constants.DEFAULT_FOLDERNAME_PATTERNS)
        log.info "Blank constructor, using defaults..."
    }

    FolderAnalyzer(Map<String, Pattern> fileNamePatterns, Map<String, Pattern> namePatternsMap) {
        this.fileNamePatterns = fileNamePatterns
        this.namePatternsMap = namePatternsMap
    }

    FolderAnalyzer(ConfigObject config) {
        this.config = config
        if (config.namePatterns?.folders) {
            namePatternsMap = config.namePatterns.folders
        }
    }


//    FolderAnalyzer(Map fnPatterns) {
//        folderNamePatterns = fnPatterns
//    }

    List<String> analyze(SavableObject object) {
        List<String> labels = []
        if(object) {
            if(!object.labels) object.labels = []
//            if(!object.labels) object.labels = []
            FSFolder ffs = (FSFolder) object
            namePatternsMap.each { String label, Pattern pattern ->
                if (ffs.name ==~ pattern) {
                    log.info "\t\tASSIGNING label: $label -- $ffs"
                    ffs.labels << label
                } else {
                    log.debug "\\t\t ($label) no match on pattern: $pattern -- $ffs"
                }
            }
            labels = object.labels
            if (labels) {
                if (labels.size() > 1) {
                    log.debug " \t\t........ More that one label?? $labels -- $ffs"
                }
            } else {
                log.debug "\t\tNO LABEL folderType assigned?? $ffs  --> ${((FSFolder) ffs).thing.absolutePath}"
                ffs.labels << Constants.LBL_UNKNOWN
            }
        } else {
            log.warn "Not a valid object: $object -- nothing to analyze"
        }
        return labels
    }


    /**
     * iterate through files, and apply file type labels (from map of patterns...?)
     * @param files
     * @return list of types found, plus we updated the sourcec list (non-FP friendly...)
     */
    List<String> assignFileTypes(List<FSFile> files) {
        List<String> allLabels = []
        files.each { FSFile fileFS ->
            log.debug "\t\tAssign file type: $fileFS"
            String fname = ((File) fileFS.thing).name
//            List<String> labels = []
            fileNamePatterns.each { String label, Pattern pattern ->
                Matcher m = (fname =~ pattern)
                if (m.matches()) {
                    log.debug "\t\tASSIGNING label: $label -- $fileFS :: matcher: ${m[0]}"
                    allLabels << label
                    fileFS.labels << label
                } else {
                    log.debug "\\t\t ($label) no match on pattern: $pattern -- $fileFS"
                }
            }
            if (fileFS.labels) {
                if (fileFS.labels.size() > 1) {
                    log.debug "More that one label?? ${fileFS.labels} -- $fileFS"
                }
            } else {
                log.debug "\t\tNO LABEL assigned?? $fileFS  --> ${((File) fileFS.thing).absolutePath}"
                fileFS.labels << Constants.LBL_UNKNOWN
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
    def analyzeArchives(List<FSFile> filesFs) {

        filesFs.each { FSFile ffs ->
            if (ffs.isArchive(ffs)) {
                File f = ffs.thing
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
//    List<String> analyze(SavableObject object) {
//        return null
//    }
}
