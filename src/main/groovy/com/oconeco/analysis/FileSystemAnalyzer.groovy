package com.oconeco.analysis

import com.oconeco.models.SavableObject
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

    /**
     * Constructor with folder and filename maps for processing basics (track, ignore,...)
     * @param folderNameMap - map of label names, with submap of `pattern`:regex and `analysis`:[List<String> names of analysis methods to call]
     * @param fileNameMap - same for file names
     * @param folderPathMap - same for folder paths
     * @param filePathMap - same for file paths
     * @param parser - tika parser
     * @param handler - tika body handler
     */
    FileSystemAnalyzer(Map<String, Map<String, Object>> folderNameMap, Map<String, Map<String, Object>> fileNameMap, Map<String, Map<String, Object>> folderPathMap = null, Map<String, Map<String, Object>> filePathMap = null, Parser parser = null, BodyContentHandler handler = null) {
        super(folderNameMap, fileNameMap, folderPathMap, filePathMap, parser, handler)
    }

    @Override
    Map<String, Map<String, Object>> analyze(SavableObject object) {
        def analysis = super.analyze(object)

        if (object.childItems) {
            object.childItems.each {
                def addedLabelsMap = it.addAncestorLabels()
            }
            log.debug "\t\t${object.path} --> added ancestor labels map (${object.labels}) for ${object.childItems.size()} child items"
        }

        if (object.childGroups) {
            object.childGroups.each {
                def addedLabelsMap = it.addAncestorLabels()
            }
            log.debug "\t\t${object.path} --> added ancestor labels map (${object.labels}) for ${object.childGroups.size()} child groups"
        }

        return analysis
    }
}
