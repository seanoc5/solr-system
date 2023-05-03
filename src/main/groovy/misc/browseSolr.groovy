package misc
/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/3/22, Wednesday
 * @description:
 */


import org.apache.log4j.Logger
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocumentList

final Logger log = Logger.getLogger(this.class.name);

log.info "Starting ${this.class.name}..."

String host = 'localhost'
int port = 8983
String collection = 'solr_system'
String baseUrl = "http://$host:$port/solr/$collection"
SolrClient solrClient = new HttpSolrClient.Builder(baseUrl).build()
SolrQuery sq = new SolrQuery("*:*")
QueryResponse resp = solrClient.query(sq)
SolrDocumentList sdl = resp.getResults()
sdl.each { sdoc ->
    log.info "Doc: ${sdoc.id}"
}

log.info "Done...?"
