package misc

import com.oconeco.persistence.SolrSaver
import org.apache.log4j.Logger
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



log.info "done!?"
