package com.oconeco.analysis

import com.oconeco.helpers.Constants
import com.oconeco.models.FSFolder
import com.oconeco.models.SavableObject
import com.oconeco.models.FSFile
import org.apache.log4j.Logger

import java.util.regex.Pattern

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

class FileAnalyzer extends BaseAnalyzer {
    Logger log = Logger.getLogger(this.class.name)
    Map DEFAULT_FILENAME_PATTERNS = [ignore: ~/([.~]*lock.*|_.*|.*\.te?mp$|.*\.class$|robots.txt)/,
                                     index : ~/(bash.*|csv|groovy|ics|ipynb|java|lst|md|php|py|rdf|rss|scala|sh|tab|te?xt|tsv)/,
    ]
    Map namePatternsMap = null

    FileAnalyzer() {
        namePatternsMap = Constants.DEFAULT_FILENAME_PATTERNS
        log.info "Blank constructor, using default filename patterns ($namePatternsMap)...?"
    }

    FileAnalyzer(Map<String, Pattern> fnPatterns) {
        namePatternsMap = fnPatterns
        log.info "Using map constructorm filenamePatterns ($fnPatterns)..."
    }


    FileAnalyzer(ConfigObject config) {
        log.info "Config constructor..."
        if (config?.namePatterns?.files) {
            namePatternsMap = config?.files?.namePatterns
            log.info "using file name patterns (($namePatternsMap)) from config object."
        } else {
            namePatternsMap = Constants.DEFAULT_FILENAME_PATTERNS
            log.warn "No valid config files.namePatterns!! falling back to default: $namePatternsMap"
        }
    }


    List<String> analyze(List<FSFile> fileFsList) {
        List<String> labels = []
        fileFsList.each { FSFile filefs ->
            log.info "Analyze fileFS: $filefs"
            String label = analyze(filefs)
        }
    }

    List<String> analyze(SavableObject object) {
        List<String> labels = []
        if (object) {
            if (!object.labels) {
                object.labels = []
            }

            FSFile fSFile = object
            namePatternsMap.each { String label, Pattern pattern ->
                if (fSFile.name ==~ pattern) {
                    log.info "\t\tASSIGNING label: $label -- $fSFile"
                    fSFile.labels << label
                } else {
                    log.debug "\\t\t ($label) no match on pattern: $pattern -- $fSFile"
                }
            }
            labels = object.labels
            if (labels) {
                if (labels.size() > 1) {
                    log.debug " \t\t........ More that one label?? $labels -- $fSFile"
                }
            } else {
                log.debug "\t\tNO LABEL folderType assigned?? $fSFile  --> ${((FSFolder) fSFile).thing.absolutePath}"
                fSFile.labels << Constants.LBL_UNKNOWN
            }
        } else {
            log.warn "Not a valid object: $object -- nothing to analyze"
        }
        return labels
    }

//    List<String> analyze(SavableObject object) {
//        log.warn "more code here!!! FileAnalyzer.analyze(object)"
//        object.labels << UNKNOWN_LABEL
//        return object.labels
//    }


}
