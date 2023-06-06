package misc


import com.oconeco.persistence.SolrSystemClient
import org.apache.log4j.Logger

Logger log = Logger.getLogger(this.class.name);
SolrSystemClient solrSaver  = new SolrSystemClient("http://oldie:8983/solr")
def clusterStatus= solrSaver.getClusterStatus()

log.info "Cluster status: $clusterStatus"

def result = solrSaver.createCollection('solr_system', 1,1, '_default')
log.info "Result: $result"

solrSaver.closeClient()
log.info "Done..."
