package com.oconeco.analysis

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
    public static final String UNKNOWN = 'unknown'
    public static final String ARCHIVE = 'archive'
    Logger log = Logger.getLogger(this.class.name);
    /** label of things we can ignore (use sparingly) */
    public static final String LBL_IGNORE = 'ignore'
    Map<String, Pattern> DEFAULT_FOLDERNAME_PATTERNS = [
            ignore       : ~/.*(__snapshots__|cache|git|github|packages?|pkgs?|plugins?|skins?|svn|target|vscode)/,
            backups      : ~/.*(bkups?|old|timeshift)/,
            configuration: ~/.*(configs)/,
            documents    : ~/.*([Dd]ocuments|[Dd]esktop)/,
            downloads    : ~/.*([Dd]ownloads)/,
            pictures     : ~/.*([Pp]ictures)/,
            programming  : ~/.*(java|jupyter|src|work)/,
            system       : ~/.*(class|sbt|sbt-\d.*|runtime)/,
    ]

    Map<String, Pattern> DEFAULT_FILENAME_PATTERNS = [
            office      : ~'.*(accdb|docx?|ods|odp|odt|pptx?|rtf|txt|vsdx?|xslx?)',
            system      : ~/.*(bin|deb|gcc|lib|pkg|rpm)/,
            archive     : ~/.*(arc|gz|rar|zip|tar.gz|zip)/,
            web         : ~/.*(html?)/,
            instructions: ~'(?i).*(adoc|readme.*|md)',
            techDev     : ~/.*(c|css|go|groovy|gradle|jar|java|javascript|js|php|schema|sh)/,
            config      : ~/.*(config.*|pem|properties|xml|yaml)/,
            data        : ~/.*(csv|jsonl?d?|lst|tab)/,
            media       : ~/.*(avi|jpe?g|ogg|mp3|mpe?g|wav)/,
    ]
    ConfigObject config

    List<String> extensions = []
    Map<String, Pattern> fileNamePatterns = DEFAULT_FILENAME_PATTERNS
    Map<String, Pattern> folderNamePatterns = DEFAULT_FOLDERNAME_PATTERNS

    ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();


    FolderAnalyzer(ConfigObject config) {
        this.config = config
        if (config.folders.namePatterns) {
            folderNamePatterns = config.folders.namePatterns
            log.info "Loading Folder analzer of foldernames from 'config.files.namePatterns': ${folderNamePatterns.collect { '\n\t\t' + it }}"
        }
        if (config.files.namePatterns) {
            fileNamePatterns = config.files.namePatterns
            log.info "Loading Folder analzer of filenames from 'config.files.namePatterns': ${fileNamePatterns.collect { '\n\t\t' + it }}"
        }

    }


    def assignFolderType(FolderFS ffs) {
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
            ffs.assignedTypes << UNKNOWN
        }
        return labels
    }


    def assignFileTypes(List<FileFS> files) {
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
                fileFS.assignedTypes << UNKNOWN
                allLabels << UNKNOWN
            }
        }
        return allLabels
    }


    Object analyzeArchives(List<FileFS> filesFs) {

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
}