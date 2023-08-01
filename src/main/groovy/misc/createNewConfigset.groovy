package misc

import com.oconeco.persistence.SolrSystemClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.solr.client.solrj.response.ConfigSetAdminResponse

Logger log = LogManager.getLogger(this.class.name);

log.info "Starting ${this.class.name}...."
// todo -- figure out error message about authentication...
// currently this script is broken on auth error
SolrSystemClient client = new SolrSystemClient()

def clusterResponse = client.getClusterStatus()
log.info "Cluster status: $clusterResponse"

ConfigSetAdminResponse response = client.createConfigSet('foo')

log.info "createConfigSet response: $response"
