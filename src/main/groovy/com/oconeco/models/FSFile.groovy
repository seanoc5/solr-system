package com.oconeco.models

import com.oconeco.analysis.BaseAnalyzer
import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSaver
import groovy.io.FileType
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
class FSFile extends SavableObject {
    Logger log = Logger.getLogger(this.class.name)
    public static final String TYPE = 'File'
    String extension
    String mimeType
//    List<String> permissions

    FSFile(String id) {
        super(id)
    }
//    FSFile(File f, int depth = 1) {
//        log.debug "File(f:${f.absolutePath}) constructor"
//        id = f.absolutePath
//        this.thing = f
//        name = f.name
//        size = f.size()
//        this.depth = depth
//        lastModifiedDate = new Date(f.lastModified())
//        type = TYPE
//        // todo -- more here -- also check FSFolder object, and analyze() method,
//        extension = FilenameUtils.getExtension(f.name)
//    }


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

    SolrInputDocument toSolrInputDocument(String crawlName=null) {
        File f = thing
        SolrInputDocument sid = super.toSolrInputDocument()
//        sid.addField(SolrSaver.FLD_SIZE, size)
        if(crawlName){
            sid.setField(Constants.FLD_CRAWL_NAME, crawlName)
        }

        if (extension) {
            sid.addField(SolrSaver.FLD_EXTENSION_SS, extension)
        }
        sid.addField(SolrSaver.FLD_NAME_SIZE_S, "${f.name}:${f.size()}")
//        sid.addField(SolrSaver.FLD_DEPTH, depth)
        return sid
    }

    boolean isArchive(FSFile ffs) {
        int fileSignature = 0;
        File f = ffs.thing
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            fileSignature = raf.readInt();
        } catch (IOException e) {
            log.warn "File ($ffs) io exception checking if we have an archive: $e"
        }
        boolean isArchive = fileSignature == 0x504B0304 || fileSignature == 0x504B0506 || fileSignature == 0x504B0708 || fileSignature== 529205248 || fileSignature== 529205268
        if(ffs.assignedTypes.contains(Constants.LBL_ARCHIVE)) {
            if(!isArchive) {
                log.info "Should be archive: $ffs -> ($fileSignature) -- $isArchive"
            } else {
                log.debug "is archive: $ffs"
            }
        }
        return isArchive

    }

    public static List<FSFile> getFileFSList(File folder, boolean recurse = false){
        List<FSFile> fileFSList = []
        if(folder && folder.isDirectory()){
            if(recurse) {
                int parentPathCount = folder.path.split(File.separator)
                folder.eachFileRecurse(FileType.FILES){
                    int childPathCount = it.path.split(File.separator)
                    int depth = childPathCount = parentPathCount
                    FSFile childFS= new FSFile(it, depth)
                    fileFSList << childFS
                }
            } else {
                folder.eachFile(FileType.FILES){
                    int childPathCount = it.path.split(File.separator)
                    FSFile childFS= new FSFile(it)
                    fileFSList << childFS
                }
            }
        }
        return fileFSList
    }

}
