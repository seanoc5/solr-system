package com.oconeco.models

import com.oconeco.helpers.Constants
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.solr.common.SolrInputDocument

import java.util.regex.Matcher
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
        Matcher matcher = (srcFolder.name =~ ignoreName)
        this.ignore = matcher.matches()
        if (ignore) {
            String match = matcher.group(1)
            labels << Constants.IGNORE
            Map ml = [(Constants.IGNORE): [pattern: ignoreName, (Constants.LABEL_MATCH): match]]
            matchedLabels = ml
            log.info "\t\tIGNORE: processing a FSFolder:($this), this matches group name ignore (${ignoreName} "
        } else {
            log.debug "\t\tprocess: processing a FSFolder:($this), this does NOT match group name ignore (${ignoreName}"
        }

        if (ignorePath) {
            Matcher matcherPath = (srcFolder.path =~ ignorePath)
            if (matcherPath.matches()) {
                String match = matcherPath.group(1)
                labels << Constants.IGNORE
                Map ml = [(Constants.IGNORE): [pattern: ignorePath, (Constants.LABEL_MATCH): match]]
                matchedLabels = ml

                log.info "\t\tIgnore this folder -- path(${this.path}) matches ignore path pattern:($ignorePath)"
                ignore = true
            }
        }

    }


    SolrInputDocument toPersistenceDocument() {
        SolrInputDocument sid = super.toPersistenceDocument()
        log.debug "\t\tFSFolder toSolrInputDocument(), from super: $sid"
        // todo -- is there anything else FSFolder specific to add??


        return sid
    }


    /**
     * Simple placeholder, more advanced SavableOject implementations may add logic to what is analyzable
     * @return
     */
    List<SavableObject> gatherAnalyzableChildren() {
        List<SavableObject> analyzableChildren = this.childItems.findAll { !it.ignore }
        return analyzableChildren
    }

}
