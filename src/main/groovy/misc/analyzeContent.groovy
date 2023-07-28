package misc


import com.oconeco.persistence.SolrSystemClient
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.LogManager
import org.apache.solr.client.solrj.SolrQuery

Logger log = LogManager.getLogger(this.class.name);
String collection = 'solr_system'
SolrSystemClient client  = new SolrSystemClient("http://localhost:8983/solr/${collection}")

SolrQuery query = new SolrQuery()

def clusterStatus=
log.info "Cluster status: $clusterStatus"

def lukeresults = client.getSchemaInformation('solr_system')
log.info "Luke response: $lukeresults"

client.closeClient()
log.info "Done..."
