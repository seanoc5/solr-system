package com.oconeco.models


import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.solr.common.SolrInputDocument

import java.util.regex.Pattern

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

/**
 * structure for handling information about a folder from the FS (filesystem),
 * handles both basic information from the filesystem, and analysis results from some process
 * todo -- switch to NIO Files and Paths
 */
class FSFolder extends FSObject {
    Logger log = LogManager.getLogger(this.class.name);
    public static final String TYPE = 'Folder'


    FSFolder(File srcFolder, SavableObject parent, String locationName, String crawlName) {
        super(srcFolder, parent, locationName, crawlName)
        groupObject = true

        if (srcFolder.exists()) {
            type = TYPE
            // todo - find best way to get file size (with ignored patterns).... currently handling in crawler??
            // see crawler to set FSFolder size after crawling folder files...

        } else {
            log.warn "Src folder ($srcFolder) is not accessible??"
        }
    }

    FSFolder(File srcFolder, SavableObject parent, String locationName, String crawlName, Pattern ignoreName, Pattern ignorePath = null) {
        this(srcFolder, parent, locationName, crawlName)
        if(srcFolder.name ==~ ignoreName){
            log.info "\t\tIgnore this folder -- name(${this.name}) matches ignore name pattern:($ignoreName)"
            ignore = true
        }
        if(ignorePath && srcFolder.path ==~ ignorePath){
            log.info "\t\tIgnore this folder -- path(${this.path}) matches ignore path pattern:($ignorePath)"
            ignore = true
        }

    }


    SolrInputDocument toPersistenceDocument() {
        SolrInputDocument sid = super.toPersistenceDocument()
        log.debug "\t\tFSFolder toSolrInputDocument(), from super: $sid"
        // todo -- is there anything else FSFolder specific to add??


        return sid
    }

    List<SavableObject> gatherAnalyzableThings(){
        List<SavableObject> savableThings = this.childItems + this
        return savableThings
    }

}
