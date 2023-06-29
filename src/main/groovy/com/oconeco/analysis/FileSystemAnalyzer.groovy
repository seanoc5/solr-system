package com.oconeco.analysis


import org.apache.log4j.Logger

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

    boolean shouldIgnore(File f) {
        boolean ignore = false
        if (ignoreGroup) {
            if (f.isDirectory()) {
                ignore = (f.name ==~ this.ignoreGroup)
                if (ignore) {
                    log.debug "\t\tIGNORE: processing a File directory ($f) -- this matches group name ignore (${ignoreGroup})"
                } else {
                    log.debug "\t\tnot ignore: processing a File directory ($f) -- no match for group name ignore (${ignoreGroup})"
                }

            } else if (ignoreItem) {
                ignore = (f.name ==~ this.ignoreItem)
                if (ignore) {
                    log.debug "\t\tIGNORE: processing a File ($f), this matches item name ignore (${ignoreItem}"
                } else {
                    log.debug "\t\tno ignore: processing a File ($f), does not match item name ignore (${ignoreItem}"
                }
            } else {
                log.warn "\t\t no ignoreItem property for this Analyzer($this), so setting object.ignore==false -- object:($object) (bug??)"
            }
        }
        return ignore
    }


//
//    List<String> analyze(List<FSFile> fileFsList) {
//        List<String> labels = []
//        fileFsList.each { FSFile filefs ->
//            log.debug "Analyze fileFS: $filefs"
//            String label = analyze(filefs)
//        }
//    }

//    List<String> analyze(SavableObject object) {
//        List<String> labels = []
//        if (object) {
//            if (!object.labels) {
//                object.labels = []
//            }
//
//            FSFile fSFile = object
//            namePatternsMap.each { String label, Pattern pattern ->
//                if (fSFile.name ==~ pattern) {
//                    log.debug "\t\tASSIGNING label: $label -- $fSFile"
//                    fSFile.labels << label
//                } else {
//                    log.debug "\\t\t ($label) no match on pattern: $pattern -- $fSFile"
//                }
//            }
//            labels = object.labels
//            if (labels) {
//                if (labels.size() > 1) {
//                    log.debug " \t\t........ More that one label?? $labels -- $fSFile"
//                }
//            } else {
//                log.debug "\t\tNO LABEL folderType assigned?? $fSFile  --> ${((FSFile) fSFile).thing.absolutePath}"
//                fSFile.labels << Constants.LBL_UNKNOWN
//            }
//        } else {
//            log.warn "Not a valid object: $object -- nothing to analyze"
//        }
//        return labels
//    }

//    List<String> analyze(SavableObject object) {
//        log.warn "more code here!!! FileAnalyzer.analyze(object)"
//        object.labels << UNKNOWN_LABEL
//        return object.labels
//    }


}
