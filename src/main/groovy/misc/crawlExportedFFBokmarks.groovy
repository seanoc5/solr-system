package misc

import com.oconeco.models.FFBookmark
import com.oconeco.models.FFFolder
import com.oconeco.persistence.SolrSystemClient
import groovy.json.JsonSlurper
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.LogManager
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument

Logger log = LogManager.getLogger(this.class.name);
String collection = 'solr_system'
String locationName = 'Firefox Profile'
String crawlName = 'seanoc5@gmail.com'
File exportedBookarks = new File("C:/Users/bentc/Desktop/ff-bookmarks-2023-08-04.json")
File cacheFolder = new File(exportedBookarks.parentFile, 'cacheFolder')
if (cacheFolder.exists() && cacheFolder.isDirectory()){
    log.info "Using cache folder:(${cacheFolder.absolutePath}...)"
} else {
    boolean success = cacheFolder.mkdirs()
    log.info "MAKING cache folder:(${cacheFolder.absolutePath}) :: success:$success"
}

SolrSystemClient client = new SolrSystemClient("http://localhost:8983/solr/${collection}")
int SOLR_BATCH_SIZE = 100
def solrSaveClosure = { FFFolder ffFolder, int depth ->
    int childSize = ffFolder.childItems.size()
    log.info "\t\t$depth) solrSaveClosure for folder:($ffFolder) -- child items:(${childSize})"
    List<SolrInputDocument> toSaveList = [ffFolder.toPersistenceDocument()]
    int cnt = 0
    ffFolder.childItems.each { FFBookmark bookmark ->
        cnt++
        log.info "\t\t$depth:$cnt: of $childSize) Convert bookmark:($bookmark) to solr input doc..."
        SolrInputDocument solrInputDocument = bookmark.toPersistenceDocument(cacheFolder)
        toSaveList << solrInputDocument
        if (toSaveList.size() % SOLR_BATCH_SIZE ==0){
            UpdateResponse response = client.saveModels(toSaveList,100)
            log.info "$cnt) Saved batch ($SOLR_BATCH_SIZE) with solr response: $response"
            toSaveList =[]
        }
    }
    UpdateResponse response = client.saveModels(toSaveList,100)     // todo - revisit commitWithin, setting low for debugging
    log.info "$depth) Saved Folder:($ffFolder) with (${toSaveList.size()}) persistable objects :: solr response: $response"
//    log.debug '\t' * (depth + 1) + "$depth) Saved Folder:($ffFolder) with (${toSaveList.size()}) persistable objects :: solr response: $response"
}


if (exportedBookarks?.exists() && exportedBookarks.canRead()) {
    JsonSlurper slurper = new JsonSlurper()
    def json = slurper.parse(exportedBookarks)
    log.debug "ROOT json size (top level): ${json.size()}"

    FFFolder rootFolder = new FFFolder(json, null, locationName, crawlName)
    log.info "Root folder: $rootFolder"

    rootFolder.walktree(solrSaveClosure, 0)

} else {
    log.error "No exportedBookarks file: ${exportedBookarks.absolutePath}, nothing to do...."
}

//
//def saveChildren (FFFolder folder){
//    List<FFFolder> items = []
//    parent.children.each{ def item ->
//        if (item.typeCode==1){
//
//        }
//    }
//}

client.closeClient("basic close from: ${this.class.name}")

log.info "Done..."
