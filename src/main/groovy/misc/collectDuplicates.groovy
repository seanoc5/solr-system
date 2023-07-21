package misc

import com.oconeco.persistence.SolrSystemClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrQuery

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/3/22, Wednesday
 * @description:
 */


//import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.response.FacetField
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.util.NamedList

import java.lang.ref.ReferenceQueue

//final Logger log = LogManager.getLogger(this.class.name);
Logger log = LogManager.getLogger(this.class.name);

log.info "Starting ${this.class.name}..."

String host = 'oldie'
int port = 8983
String collection = 'solr_system'
String baseUrl = "http://$host:$port/solr/$collection"
Http2SolrClient solrClient = new Http2SolrClient.Builder(baseUrl).build()

SolrQuery q = new SolrQuery("*:*")
q.setRows(3)
q.addFilterQuery("-label_ss:(ignore system archives logs)")
q.addFilterQuery("-crawlName_s:(opt)")
q.addFilterQuery("type_s:(File ArchFile)")
q.setFacet(true)
q.setFacetLimit(100)
q.addFacetField(SolrSystemClient.FLD_DEDUP)
q.addFacetField(SolrSystemClient.FLD_TYPE)
q.addFacetField(SolrSystemClient.FLD_CRAWL_NAME)
//q.addFacetField(SolrSystemClient.FLD_CONTENT_BODY_SIZE)
//q.addFacetField(SolrSystemClient.FLD_SIZE)
q.setFacetMinCount(1)
q.add("f.${SolrSystemClient.FLD_DEDUP}.facet.mincount", "2");
//q.add("f.${SolrSystemClient.FLD_DEDUP}.facet.limit", "500000");
q.add("f.${SolrSystemClient.FLD_DEDUP}.facet.limit", "50");
//q.addFacetField(SolrSystemClient.FLD_TYPE)
//q.addFacetField(SolrSystemClient.FLD_CRAWL_NAME)
QueryResponse resp = solrClient.query(q)
SolrDocumentList sdl = resp.results
log.info "NumFound: ${sdl.getNumFound()}"

List<FacetField> facetFields = resp.facetFields
FacetField dedupFacets = facetFields.find { it.name == SolrSystemClient.FLD_DEDUP }
log.info "Dedup Facet count: ${dedupFacets.values.size()}"
dedupFacets.each {
    log.info "dedup: \t\t${it.name}"
    it.values.each {
        log.info "\t\t$it"
    }
}

int batchSize = 10
List<String> fieldList = "dedup_s type_s name_s path_s size_l lastModified_dt indexedTime_dt label_ss crawlName_s contentSize_l id".split(' ')
SolrQuery sq2 = new SolrQuery()
sq2.setFields(fieldList.join(' '))
sq2.setRows(100000)

File outFile = new File('duplicates.list.csv')
def fieldNames = []
outFile.withWriter { BufferedWriter writer ->
    int cnt = 0
    def rc = fieldList.add('dupCount')
    String headers = fieldList.join(',')
    writer.writeLine(headers)

    dedupFacets.values.each { FacetField.Count facetCount ->
        cnt++
        def dupCount = facetCount.count
        String qs = "dedup_s:\"${facetCount.name}\""
        sq2.setQuery(qs)
        QueryResponse qr = solrClient.query(sq2)
        SolrDocumentList sdl2 = qr.results

        sdl2.each { SolrDocument solrDocument ->
            fieldList.each {
                String s
                def val = solrDocument.getFieldValue(it)
                if (it.endsWith('_ss')){
                    log.debug "multivalued: $it"
                    s = val?.join('\n')
                } else {
                    s = val
                }
                if (s != null) {
                    log.debug "\t\thave a valid valu: $s"
                } else {
                    s = ''
                }
                writer.write(s + ',')
            }
            writer.writeLine("${dupCount}")
        }

    }
}
solrClient.close()


log.info "Done...?"
