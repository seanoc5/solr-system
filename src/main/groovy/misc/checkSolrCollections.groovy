package misc

import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSystemClient
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.LogManager

Logger log = LogManager.getLogger(this.class.name);

SolrSystemClient solrSystemClient  = new SolrSystemClient("http://localhost:8983/solr")
String primaryCollection = Constants.DEFAULT_COLL_NAME
String analysisCollection = Constants.ANALYSIS_COLL_NAME

def clusterStatus= solrSystemClient.getClusterStatus()
log.info "Cluster status: $clusterStatus"

def collMap  = solrSystemClient.getCollections()
log.info "Collections info map: $collMap "
List<String> collections = collMap.collections

// ----------------------- Solr_system -----------------------
if (collections.contains(primaryCollection)){
    log.info "We seem to have default collection named: ${primaryCollection}, no need to create"
} else {
    def result = solrSystemClient.createCollection(primaryCollection, 1, 1, '_default')
    log.info "Create default collection ($primaryCollection) Result: $result"
}
def lukeresultsPrimary = solrSystemClient.getSchemaInformation(primaryCollection)
log.info "Primary collection ($primaryCollection) schema/luke info: $lukeresultsPrimary"


// ----------------------- system_analysis -----------------------
if (collections.contains(Constants.ANALYSIS_COLL_NAME)){
    log.info "We seem to have ANALYSIS collection named: ${Constants.ANALYSIS_COLL_NAME}, no need to create"
} else {
    def result = solrSystemClient.createCollection(Constants.ANALYSIS_COLL_NAME, 1, 1, '_default')
    log.warn "Create default collection Result: $result"
}
def lukeresultsAnalysis = solrSystemClient.getSchemaInformation(Constants.DEFAULT_COLL_NAME)
log.info "Analysis collection ($analysisCollection) schema/luke info: $lukeresultsAnalysis"


solrSystemClient.closeClient()
log.info "Done..."
