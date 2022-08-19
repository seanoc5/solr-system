package com.oconeco.analysis

import com.oconeco.models.FileFS
import com.oconeco.models.FolderFS
import org.apache.log4j.Logger

import java.util.regex.Pattern

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

class FolderAnalyzer extends BaseAnalyzer {
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

//    def analyze(BaseObject object) {
//        if(!srcFolder instanceof FolderFS) {
//            // todo -- improve with generics and better class hierarchy working...?
//            throw new IllegalArgumentException("Object param was not a FolderFS type, bailing hard...")
//        }
//        FolderFS srcFolder = srcFolder
//        srcFolder.files.each { File f ->
//            String fileName = f.name
//            String ext = FilenameUtils.getExtension(fileName)
//            extensions << ext
//        }
//
//    }

    def assignFolderType(FolderFS ffs) {
        List<String> labels = []
        folderNamePatterns.each { String label, Pattern pattern ->
            if (ffs.name ==~ pattern) {
                log.debug "\t\tASSIGNING label: $label -- $ffs"
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
            ffs.assignedTypes << 'unknown'
        }
    }


    def assignFileTypes(List<FileFS> files) {
        files.each { FileFS fileFS ->
            log.debug "\t\tAssign file type: $fileFS"
            String fname = ((File) fileFS.me).name
            List<String> labels = []
            fileNamePatterns.each { String label, Pattern pattern ->
                if (fname ==~ pattern) {
                    log.debug "\t\tASSIGNING label: $label -- $fileFS"
                    labels << label
                    fileFS.assignedTypes << label
                } else {
                    log.debug "\\t\t ($label) no match on pattern: $pattern -- $fileFS"
                }
            }
            if (labels) {
                if (labels.size() > 1) {
                    log.debug "More that one label?? $labels -- $fileFS"
                }
            } else {
                log.debug "\t\tNO LABEL assigned?? $fileFS  --> ${((File) fileFS.me).absolutePath}"
                fileFS.assignedTypes << 'unknown'
            }
        }
    }


}
