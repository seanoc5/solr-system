package misc

import com.oconeco.persistence.SolrSystemClient
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.LogManager
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.response.QueryResponse
/**
 * simple script to perform delete query
 */
Logger log = LogManager.getLogger(this.class.name);
log.info "${this.class.name} -- Delete documents...."
String solrUrl = "http://localhost:8983/solr/solr_system"
SolrSystemClient solrSaver = new SolrSystemClient(solrUrl)

//String q = 'type_s:("FFPObject")'
//String q = 'locationName_s:("seanoc5@gmail.com")'
String q = 'locationName_s:("Firefox Profile" "seanoc5@gmail.com")'
//SolrQuery sq = new SolrQuery(q)  -- delete call only takes a basic string query, no signature for a solrquery (which sort of makes sense...)
//sq.addFilterQuery('locationName_s:("Firefox Profile" "seanoc5@gmail.com")')
QueryResponse resp = solrSaver.query(q)

long numFound = resp.results.getNumFound()
log.info "NumFound before clear (q:$q): $numFound"

def foo = solrSaver.deleteDocuments(q)
log.info "Clear results  (q:$q): $foo"
QueryResponse resp2 = solrSaver.query(q)

// redundant? double check??
long numFound2 =solrSaver.getDocumentCount(q)
log.info "NumFound after clear  (q:$q): $numFound2"

solrSaver.closeClient("Closing solr helper from parent script: ${this.class.name}")
assert numFound2==0

log.info "done!?"
