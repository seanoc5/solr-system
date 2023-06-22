package com.oconeco.analysis

import com.oconeco.helpers.Constants
import com.oconeco.models.FSFile
import com.oconeco.models.SavableObject
import org.apache.log4j.Logger

import java.util.regex.Pattern

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

class FileSystemAnalyzer extends BaseAnalyzer {
    Logger log = Logger.getLogger(this.class.name)

    FileSystemAnalyzer(folderNameMap, fileNameMap, folderPathMap= null, filePathMap=null) {
        super(folderNameMap, fileNameMap, folderPathMap, filePathMap)
    }

    FileSystemAnalyzer(ConfigObject config) {
        super(config)
    }

    List<String> analyze(List<FSFile> fileFsList) {
        List<String> labels = []
        fileFsList.each { FSFile filefs ->
            log.debug "Analyze fileFS: $filefs"
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
                    log.debug "\t\tASSIGNING label: $label -- $fSFile"
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
                log.debug "\t\tNO LABEL folderType assigned?? $fSFile  --> ${((FSFile) fSFile).thing.absolutePath}"
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
