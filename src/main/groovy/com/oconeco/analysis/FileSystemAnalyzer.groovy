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

}
