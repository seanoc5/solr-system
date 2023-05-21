package com.oconeco.analysis


import com.oconeco.models.SavableObject
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

    abstract List<String> analyze(SavableObject object)

    String toString(){
        String s = this.class.simpleName
    }
}
