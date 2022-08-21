package com.oconeco.models

import com.oconeco.analysis.BaseAnalyzer
import com.oconeco.analysis.FolderAnalyzer
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


    // todo -- should this be object.analze, or analyzer.analyze(object)???
    def analyze(BaseAnalyzer analyzer) {
        log.info "MORE CODE here:: Analyze Folder: $this with analyzer:$analyzer"
    }

    String toString() {
        String s = null
        if (assignedTypes) {
            s = "${type}: ${name} :: (${assignedTypes[0]})"
        } else {
            s = "${type}: ${name}"
        }
        return s
    }

    SolrInputDocument toSolrInputDocument() {
        File f = me
        SolrInputDocument sid = super.toSolrInputDocument()
//        sid.addField(SolrSaver.FLD_SIZE, size)
        if (extension) {
            sid.addField(SolrSaver.FLD_EXTENSION_SS, extension)
            sid.addField(SolrSaver.FLD_NAME_SIZE_S, "${f.name}:${f.size()}")
        }
//        sid.addField(SolrSaver.FLD_DEPTH, depth)
        return sid
    }

    boolean isArchive(FileFS ffs) {
        int fileSignature = 0;
        File f = ffs.me
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            fileSignature = raf.readInt();
        } catch (IOException e) {
            log.warn "File ($ffs) io exception checking if we have an archive: $e"
        }
        boolean isArchive = fileSignature == 0x504B0304 || fileSignature == 0x504B0506 || fileSignature == 0x504B0708 || fileSignature== 529205248 || fileSignature== 529205268
        if(ffs.assignedTypes.contains(FolderAnalyzer.ARCHIVE)) {
            if(!isArchive) {
                log.info "Should be archive: $ffs -> ($fileSignature) -- $isArchive"
            } else {
                log.debug "is archive: $ffs"
            }
        }
        return isArchive

    }
}
