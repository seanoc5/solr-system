package misc

import com.oconeco.models.FFFolder
import com.oconeco.models.FFObject
import com.oconeco.persistence.SolrSystemClient
import groovy.json.JsonSlurper
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.LogManager
import org.apache.solr.client.solrj.SolrQuery

Logger log = LogManager.getLogger(this.class.name);
String collection = 'solr_system'
String locationName = 'Firefox Profile'
String crawlName = 'seanoc5@gmail.com'
File exportedBookarks = new File("C:/Users/bentc/Desktop/ff-bookmarks-2023-08-04.json")
if (exportedBookarks?.exists() && exportedBookarks.canRead()) {
    JsonSlurper slurper = new JsonSlurper()
    def json = slurper.parse(exportedBookarks)
    log.info "json size (top level): ${json.size()}"

//    Long lm = json.lastModified / 1000
//    Date lastModified = new Date(lm)
//    log.info "Convert long dateModified timestamp (${json.lastModified}) to date:$lastModified"
//    String root = json.root
//    def children = json.children
//    def childObjects = crawlChildren(children)

    FFFolder rootFolder = new FFFolder(json,null,locationName, crawlName)
    log.info "Root folder: $rootFolder"
//    SolrSystemClient client = new SolrSystemClient("http://localhost:8983/solr/${collection}")
//    SolrQuery query = new SolrQuery()
//
//    client.closeClient()
} else {
    log.error "No exportedBookarks file: ${exportedBookarks.absolutePath}, nothing to do...."
}



//List <FFObject> crawlChildren (def parent){
//    List<FFObject> items = []
//    parent.children.each{ def item ->
//        if (item.typeCode==1){
//
//        }
//    }
//}
log.info "Done..."
