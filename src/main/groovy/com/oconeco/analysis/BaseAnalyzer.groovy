package com.oconeco.analysis


import org.apache.log4j.Logger
/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

abstract class BaseAnalyzer {
    static final Logger log = Logger.getLogger(this.class.name);

//    BaseAnalyzer() {
//        log.debug "BaseAnalyzer() constructor..."
//    }

//    abstract analyze(BaseObject object)

    String toString(){
        String s = this.class.simpleName
    }
}
