package com.oconeco.models

import com.oconeco.analysis.BaseAnalyzer
import com.oconeco.persistence.SolrSaver
import org.apache.commons.io.FilenameUtils
import org.apache.log4j.Logger
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
 */
class FileFS extends BaseObject {
    Logger log = Logger.getLogger(this.class.name)
    public static final String TYPE = 'File'
    String extension
//    List<String> permissions


    FileFS(File f, int depth = 1) {
        log.debug "File(f:${f.absolutePath}) constructor"
        id = f.absolutePath
        this.me = f
        name = f.name
        size = f.size()
        this.depth = depth
        type = TYPE
        // todo -- more here -- also check FolderFS object, and analyze() method,
        extension = FilenameUtils.getExtension(f.name)
    }

//    linkFolders(FileFS parentFolder) {
//        log.info "linkFolders(parentFolder:${parentFolder.directory.name})"
//        if (parentFolder) {
//            log.info "\t\tLink parent and child/this folder... "
//            parentFolder.subdirFolders << this
//            if (!parentFolder.depth > 0) {
//                log.warn "Parent (${parentFolder.directory} depth was 0, set to 1 (is this the start of the crawl?)"
//                parentFolder.depth = 1
//            }
//            this.parentFolder = parentFolder
//            this.depth = parentFolder.depth + 1
//        }
//    }

    // todo -- should this be object.analze, or analyzer.analyze(object)???
    def analyze(BaseAnalyzer analyzer) {
        log.info "Analyze Folder: $this with analyzer:$analyzer"
    }

    String toString() {
        String s = null
        if(assignedTypes) {
            s = "${type}: ${name} :: (${assignedTypes[0]})"
        } else {
            s = "${type}: ${name}"
        }
        return s
    }

    SolrInputDocument toSolrInputDocument(){
        File f = me
        SolrInputDocument sid = super.toSolrInputDocument()
//        sid.addField(SolrSaver.FLD_SIZE, size)
        if(extension) {
            sid.addField(SolrSaver.FLD_EXTENSION_SS, extension)
        }
//        sid.addField(SolrSaver.FLD_DEPTH, depth)
        return sid
    }
}
