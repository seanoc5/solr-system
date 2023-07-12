package com.oconeco.persistence

import com.oconeco.models.SavableObject
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrRequest
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.request.CollectionAdminRequest
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.common.util.NamedList
import org.apache.tika.detect.Detector
/**
 * Looking at helper class to save solr_system (file crawl to start with content to solr
 */
class SolrSystemClient extends BaseClient {
    public final Logger log = LogManager.getLogger(this.class.name)

    /** upper limit of 'big' queries, e.g. folder lists for a gven crawl */
    public static int MAX_ROWS_RETURNED = 100000


    // todo -- move this to (BASE) Object hierarchy, leave solrsaver to do the actual saving, not building?
    public static String FLD_PARENT_ID = 'parentId_s'

    public static String FLD_DEPTH = 'depth_i'
    public static String FLD_PATH_T = 'path_txt_en'
    public static String FLD_NAME_S = 'name_s'
    public static String FLD_NAME_T = 'name_txt_en'

    public static String FLD_CREATED_DATE = "createdDate_dt"          // pdate dynamic field
    // todo - improve owner name extraction/ETL
    public static String FLD_OWNER = "owner_s"                        // varies by operating system...


    public static String FLD_HIDDEN_B = "hidden_b"
    public static String FLD_EXTENSION_SS = "extension_ss"

    public static String FLD_TAG_SS = "tag_ss"
    public static String FLD_LABELS = "label_ss"
    public static String FLD_OS_NAME = "operatingSystem_s"
//    public static String FLD_ = ""
    public static String FLD_MATCHED_LABELS = "matchedLabels_ss"

    public static String FLD_CONTENT_BODY = 'content_txt_en'
    public static String FLD_CONTENT_BODY_SIZE = 'contentSize_l'
    public static String FLD_METADATA = 'metadata_txt_en'

//    public static String FLD_ = ""
//    public static String FLD_ = ""


    Integer SOLR_BATCH_SIZE = 5000
    Integer MIN_FILE_SIZE = 10
    Integer MAX_CONTENT_SIZE = 1024 * 1000 * 10 // (10 MB of text?)
    Http2SolrClient solrClient

    Detector detector = null
//    Parser parser = null
//    BodyContentHandler handler = null
//    TikaConfig tikaConfig

    /**
     * Create helper with a (non-thread safe???) solrClient that is configured for the solr server AND collection
     * todo -- revisit for better approach...
     * @param baseSolrUrl
     */
    SolrSystemClient(String baseSolrUrl) {
        log.debug "Constructor baseSolrUrl:$baseSolrUrl"
        buildSolrClient(baseSolrUrl)
//        parser = new AutoDetectParser()
//        handler = new BodyContentHandler(MAX_CONTENT_SIZE)
    }

/*    SolrSystemClient(String baseSolrUrl, String hostName) {
//        this(baseSolrUrl)
        log.debug "\t\tConstructor baseSolrUrl:$baseSolrUrl --- Host/machine name:$hostName --- WITHOUT tika"
        buildSolrClient(baseSolrUrl)
    }*/

/*
    SolrSystemClient(String baseSolrUrl, String hostName, TikaConfig tikaConfig) {
//        this(baseSolrUrl, dataSourceName)
        log.info "\t\tConstructor baseSolrUrl:$baseSolrUrl --- CrawlName:$hostName --- with TIKA config: $tikaConfig"
        buildSolrClient(baseSolrUrl)
//        this.tikaConfig = tikaConfig
//        detector = tikaConfig.getDetector()
//        parser = new AutoDetectParser()
//        handler = new BodyContentHandler()
    }
*/

    public void buildSolrClient(String baseSolrUrl) {
        log.info "\t\tBuild solr client with baseSolrUrl: $baseSolrUrl"
        solrClient = new Http2SolrClient.Builder(baseSolrUrl).build()
        log.debug "\t\tBuilt Solr Client: ${solrClient.baseURL}"
    }

    public void closeClient(String msg = 'general close call') {
        log.info "Closing solr client with message '$msg' ---- ($solrClient)"
        solrClient.close()
    }

    def getClusterStatus() {
        final SolrRequest request = new CollectionAdminRequest.ClusterStatus();

        final NamedList<Object> response = solrClient.request(request);
        final NamedList<Object> cluster = (NamedList<Object>) response.get("cluster");
        final List<String> liveNodes = (List<String>) cluster.get("live_nodes");

        log.info("Found " + liveNodes.size() + " live nodes");
        return response
    }

    /**
     * helper/wrapper function to create a new solr collection (i.e. solr_system)
     * @param collectionName
     * @param numShards
     * @param replactionFactor
     * @param configName - defaults to _default
     * @return named list of results
     */
    NamedList createCollection(String collectionName, Integer numShards, Integer replactionFactor, String configName = '_default') {
        //http://localhost:8983/solr/admin/collections?action=CREATE&name=newCollection&numShards=2&replicationFactor=1
        NamedList result = null
        try {
            CollectionAdminRequest.Create create = CollectionAdminRequest.createCollection(collectionName, numShards, replactionFactor);
//        def request = new CollectionAdminRequest.create(collectionName, configName, numShards, replactionFactor);
            result = solrClient.request(create)
        } catch (RemoteSolrException rse) {
            log.error "Remote error: $rse"
        }
        return result
    }


    /**
     * Clear the collection of current data -- BEWARE
     * todo -- add partitioning so we clear the same 'datasource' as we are about to crawl,
     * i.e. get a job name/id based on the source paths
     */


    /**
     * Clear the solr collection, but using a path of folders (and children) to clear
     * @param pathToClear
     * @return solr update response
     *
     * todo -- add logic to handle "complex" clear paths -- sym links etc....
     */
    UpdateResponse deleteDocuments(String deleteQuery, int commitWithinMS = 500, boolean checkBeforeAfter = true) {
        Long beforeCount
        if (checkBeforeAfter) {
            beforeCount = getDocumentCount(deleteQuery)
            log.info "\t\t$beforeCount count before query: $deleteQuery"
        }
        log.warn "DELETing solr docs: ${this.solrClient.baseURL} -- deleteQuery: $deleteQuery (commit within: $commitWithinMS)"
        UpdateResponse ursp = solrClient.deleteByQuery(deleteQuery, commitWithinMS)
        UpdateResponse ursp2 = solrClient.commit(true, true)
        int status = ursp.getStatus()
        if (status == 0) {
            log.debug "\t\tSuccess deleting documents with query: $deleteQuery"
        } else {
            log.warn "FAILED to delete docs with delete query: ($deleteQuery) -- solr problem/error?? $ursp"
        }
        if (checkBeforeAfter) {
            Long afterCount = getDocumentCount(deleteQuery)
            log.debug "\t\t$afterCount count AFTER query: $deleteQuery (diff: ${beforeCount - afterCount})"
            // todo -- is this bad code? defaults somehow to not wait for commit? later call catches proper counter (0)???
        }

        return ursp
    }


    /**
     * get solr delete query for 'current' crawler info: locationName and crawlName
     * @param Specific crawler with locationName and crawlName
     * @return difference between before delete and after delete
     */
    Map<String, Object> deleteCrawledDocuments(String locationName, String crawlName) {
        Map<String, Object> results = [:]
        String deleteQuery = SolrSystemClient.FLD_LOCATION_NAME + ':"' + locationName + '" AND ' + SolrSystemClient.FLD_CRAWL_NAME + ':"' + crawlName + '"'
        long preDeleteCount = getDocumentCount(deleteQuery)
        log.debug "\t\t------ DELETE query: $deleteQuery (pre-delete count: $preDeleteCount)"
        UpdateResponse deleteResponse = deleteDocuments(deleteQuery, 0)
        UpdateResponse commitResponse = commitUpdates(true, true)
        long postDeleteCount = getDocumentCount(deleteQuery)
        log.info "------PRE-delete count:($preDeleteCount) --> POST-delete count:($postDeleteCount) -- diff:(${preDeleteCount - postDeleteCount})"
        results = [deleteQuery: deleteQuery, preDeleteCount: preDeleteCount, postDeleteCount: postDeleteCount, deleteResponse: deleteResponse, commitResponse: commitResponse]
        return results
    }


    /**
     * force a commit.
     * NOTE: read up on explicit/forced commits, often better to leave solr to handle with proper soft/auto commits
     * @param waitFlush
     * @param waitSearcher
     * @return
     */
    def commitUpdates(boolean waitFlush = false, boolean waitSearcher = false) {
        log.debug "\t\t____explicit call to solr 'commit' (consider allowing autocommit settings to do this for you...)"
        solrClient.commit(waitFlush, waitSearcher)
    }


    @Override
    def saveObjects(List<SavableObject> objects, int commitWithinMS = 1000) {
        UpdateResponse resp
        if(objects) {
            String firstId = objects[0].id
            log.debug "\t\t++++Adding solrInputDocuments, size: ${objects.size()} -- first ID:($firstId)"
            List<SolrInputDocument> solrInputDocuments = objects.collect { it.toSolrInputDocument() }
            try {
                resp = solrClient.add(solrInputDocuments, commitWithinMS)
                if (resp.status == 0) {
                    log.debug "saveObjects add response: $resp"
                } else {
                    log.warn "Bad response from solr save/commit?? $resp"
                }
            } catch (SolrServerException sse) {
                log.error "Solr server exception: $sse"
            }
        } else {
            log.info "No objects passed to saveObjects($objects), skipping..."
        }
        return resp
    }


    /**
     * convenience method to get a count of documents in index
     * @param queryToCount (defaults to all docs)
     * @return
     */
    def long getDocumentCount(String queryToCount = '*:*', String filterQuery = null) {
        SolrQuery sq = new SolrQuery(queryToCount)
        sq.setFields('')
        sq.setRows(0)
        if (filterQuery) {
            sq.addFilterQuery(filterQuery)
        }
        QueryResponse resp = solrClient.query(sq)
        long docCount = resp.getResults().numFound
        return docCount
    }


    QueryResponse query(SolrQuery sq) {
        // todo -- add retry if timeout
        QueryResponse resp = solrClient.query(sq)
        return resp
    }

    QueryResponse query(String query) {
        // todo -- add retry if timeout
        SolrQuery sq = new SolrQuery(query)
        QueryResponse resp = this.query(sq)
        return resp
    }


    @Override
    public String toString() {
        return "SolrSystemClient{" +
                "SOLR_BATCH_SIZE=" + SOLR_BATCH_SIZE +
                ", MIN_FILE_SIZE=" + MIN_FILE_SIZE +
                ", MAX_CONTENT_SIZE=" + MAX_CONTENT_SIZE +
                ", solrClient=" + solrClient +
                ", detector=" + detector +
                ", parser=" + parser +
                ", handler=" + handler +
                ", tikaConfig=" + tikaConfig +
                '}';
    }


}
