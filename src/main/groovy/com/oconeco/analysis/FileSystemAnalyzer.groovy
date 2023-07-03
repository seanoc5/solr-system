package com.oconeco.analysis

import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.LogManager

import org.apache.tika.parser.Parser
import org.apache.tika.sax.BodyContentHandler
/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

class FileSystemAnalyzer extends BaseAnalyzer {
    Logger log = LogManager.getLogger(this.class.name)

    FileSystemAnalyzer(folderNameMap, fileNameMap, folderPathMap = null, filePathMap = null, Parser parser = null, BodyContentHandler handler = null) {
        super(folderNameMap, fileNameMap, folderPathMap, filePathMap, parser, handler)
    }
}
