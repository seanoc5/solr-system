package com.oconeco.models

import com.oconeco.helpers.Constants
import groovy.io.FileType
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
    Logger log = LogManager.getLogger(this.class.name)
    public static final String TYPE = 'Folder'


    /**
     * basic constructor - assume further pattern matching happens in calling code
     * @param srcFolder
     * @param parent
     * @param locationName
     * @param crawlName
     */
    FSFolder(File srcFolder, SavableObject parent, String locationName, String crawlName) {
        super(srcFolder, parent, locationName, crawlName)
        groupObject = true

        if (srcFolder.exists()) {
            type = TYPE
        } else {
            log.warn "Src folder ($srcFolder) is not accessible??"
        }
    }


    /**
     * sligntly advanced constructor (for testing??) where basic ignore patterns are checked and properties set in constructor
     * @param srcFolder
     * @param parent
     * @param locationName
     * @param crawlName
     * @param ignoreName
     * @param ignorePath
     */
    FSFolder(File srcFolder, SavableObject parent, String locationName, String crawlName, Pattern ignoreName, Pattern ignorePath = null) {
        this(srcFolder, parent, locationName, crawlName)
        Matcher matcher = (srcFolder.name =~ ignoreName)
        this.ignore = matcher.matches()
        if (ignore) {
            String match = matcher.group(0)
            if(matcher.groupCount() > 0){
                match = matcher.group(1)
            }
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


    def addChildren(Pattern ignoreChildNames) {
        long folderSize = 0
        File f = this.thing
        if(f?.exists() && f.canRead()){
            f.eachFile(FileType.FILES){
                if(it.name ==~ ignoreChildNames){
                    log.debug "\t\tIgnore pattern ($ignoreChildNames) matches child filename (${it.name}) -- ignoring child (i.e. skipping)"
                } else {
                    FSFile fsf = new FSFile(it, this, this.locationName, this.crawlName)
                    folderSize += it.size()
                    this.childItems << fsf
                }
            }
            if(this.size) {
                if (this.size == folderSize) {
                    log.warn "\t\tAttention: replacing existing FSFolder($this) size(${this.size}) with newly calculated size: $folderSize -- same size? redundant processing????"
                } else {
                    log.warn "Attention: replacing existing FSFolder($this) size(${this.size}) with DIFFERENT calculated size: $folderSize -- why is there a difference? TODO: skip one of the (wrong) calculations???"
                }
            }
            this.size = folderSize

            String newDedup = this.buildDedupString()
            if(this.dedup){
                if(this.dedup == newDedup){
                    log.warn "Replacing existing dedup string with same string (redundant code??): $newDedup -- this($this)" //todo change this to debug once we have checked logic/code
                } else {
                    log.warn "How do we already have dedup for FSFolder ($this)? ${this.dedup} -- Attention: replacing it with new dedup: $newDedup"
                }
            }
            this.dedup = newDedup
        } else {
            log.warn "Cannot read FSFolder file (thing): ${this.thing} -- FSFolder:($this)"
        }
        return this.childItems
    }

}
