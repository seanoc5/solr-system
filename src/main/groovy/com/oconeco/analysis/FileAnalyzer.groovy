package com.oconeco.analysis

import com.oconeco.models.BaseObject
import com.oconeco.models.FileFS
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
    Map filenamePatterns = null

    FileAnalyzer() {
        log.info "Blank constructor, using default filename patterns ($DEFAULT_FILENAME_PATTERNS)...?"
        filenamePatterns = DEFAULT_FILENAME_PATTERNS
    }

    FileAnalyzer(Map<String, Pattern> fnPatterns) {
        filenamePatterns = fnPatterns
        log.info "Using map constructorm filenamePatterns ($fnPatterns)..."
    }


    FileAnalyzer(ConfigObject config) {
        log.info "Config constructor..."
        if(config?.namePatterns?.files){
            filenamePatterns = config?.files?.namePatterns
            log.info "using file name patterns (($filenamePatterns)) from config object."
        } else {
            log.warn "No valid config files.namePatterns!! falling back to default: $DEFAULT_FILENAME_PATTERNS"
            filenamePatterns = DEFAULT_FILENAME_PATTERNS
        }
    }


    List<String> analyze(List<FileFS> fileFsList) {
        List<String> labels = []
        fileFsList.each { FileFS filefs ->
            log.info "Analyze fileFS: $filefs"
            String label = analyze(filefs)
        }
    }

    List<String> analyze(BaseObject object) {
        log.warn "more code here!!! FileAnalyzer.analyze(object)"
        object.assignedTypes << UNKNOWN_LABEL
        return object.assignedTypes
    }


}
