package com.oconeco.analysis

import com.oconeco.models.BaseObject
import org.apache.log4j.Logger
/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

abstract class BaseAnalyzer {
    static final Logger log = Logger.getLogger(this.class.name);
    String UNKNOWN_LABEL = 'unknown'

//    BaseAnalyzer() {
//        log.debug "BaseAnalyzer() constructor..."
//    }

    abstract List<String> analyze(BaseObject object)

    String toString(){
        String s = this.class.simpleName
    }
}
