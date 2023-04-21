package misc

import com.oconeco.persistence.SolrSaver
import org.apache.log4j.Logger
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.response.QueryResponse

/**
 * simple script to perform delete query
 */
Logger log = Logger.getLogger(this.class.name);
log.info "${this.class.name} -- Delete documents...."
String solrUrl = "http://localhost:8983/solr/solr_system"
SolrSaver solrSaver = new SolrSaver(solrUrl)

String q = "*:*"

def foo = solrSaver.deleteDocuments(q)
log.info "Clear results: $foo"

SolrQuery sq = new SolrQuery(q)
QueryResponse resp = solrSaver.query('*:*')
long numFound = resp.results.getNumFound()
log.info "NumFound: $numFound"
assert numFound==0


log.info "done!?"
