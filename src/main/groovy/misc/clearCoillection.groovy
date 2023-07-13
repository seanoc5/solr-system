package misc

import com.oconeco.persistence.SolrSystemClient
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.LogManager

import org.apache.solr.client.solrj.response.QueryResponse
/**
 * simple script to perform delete query
 */
Logger log = LogManager.getLogger(this.class.name);
log.info "${this.class.name} -- Delete documents...."
String solrUrl = "http://localhost:8983/solr/solr_system"
SolrSystemClient solrSaver = new SolrSystemClient(solrUrl)

String q = "*:*"
//SolrQuery sq = new SolrQuery(q)
QueryResponse resp = solrSaver.query(q)
long numFound = resp.results.getNumFound()
log.info "NumFound before clear: $numFound"

def foo = solrSaver.deleteDocuments(q)
log.info "Clear results: $foo"
QueryResponse resp2 = solrSaver.query(q)

long numFound2 =solrSaver.getDocumentCount(q)
log.info "NumFound after clear: $numFound2"

solrSaver.closeClient(this.class.name)
assert numFound2==0

log.info "done!?"
