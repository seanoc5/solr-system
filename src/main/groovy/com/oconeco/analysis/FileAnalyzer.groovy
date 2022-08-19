package com.oconeco.analysis

import com.oconeco.models.BaseObject
import org.apache.log4j.Logger

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

class FileAnalyzer extends BaseAnalyzer {
    Logger log = Logger.getLogger(this.class.name);


    def analyze(BaseObject object) {
        log.warn "more code here!!! FileAnalyzer.analyze(object)"
    }


}
