package com.oconeco.models


import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger

import org.apache.solr.common.SolrInputDocument
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
//            Path nioPath = srcFolder.toPath()

            if (srcFolder.canExecute() && srcFolder.canRead()) {
                size = FileUtils.sizeOf(srcFolder)
                log.debug "\t\t$srcFolder) directory size: $size"
                dedup = buildDedupString()

            } else {
                log.warn "\t\tCannot execute/read folder: $srcFolder"
                size = srcFolder.size()
            }



        } else {
            log.warn "Src folder ($srcFolder) is not accessible??"
        }
    }


    SolrInputDocument toSolrInputDocument() {
        SolrInputDocument sid = super.toSolrInputDocument()
        log.debug "\t\tFSFolder toSolrInputDocument(), from super: $sid"
        // todo -- is there anything else FSFolder specific to add??

        return sid
    }

    List<SavableObject> gatherSavableObjects(){
        List<SavableObject> list = this.children
        list.addAll(this)
        return list
    }

    List<FSFile> gatherArchiveFiles(){
        List<FSFile> archiveFiles = []
        if(children){
            children.findAll {it.archive}
        }
        return archiveFiles
    }
}
