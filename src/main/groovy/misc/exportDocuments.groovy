package misc


import com.oconeco.persistence.SolrSystemClient
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.LogManager

Logger log = LogManager.getLogger(this.class.name);
SolrSystemClient client  = new SolrSystemClient("http://localhost:8983/solr")

def clusterStatus= client.getClusterStatus()
log.info "Cluster status: $clusterStatus"

def lukeresults = client.getSchemaInformation('solr_system')
log.info "Luke response: $lukeresults"

client.closeClient()
log.info "Done..."
