package com.oconeco.persistence

import com.oconeco.helpers.Constants
import com.oconeco.models.SavableObject
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrRequest
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException
import org.apache.solr.client.solrj.impl.CloudSolrClient
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.request.CollectionAdminRequest
import org.apache.solr.client.solrj.request.ConfigSetAdminRequest
import org.apache.solr.client.solrj.request.LukeRequest
import org.apache.solr.client.solrj.request.SolrPing
import org.apache.solr.client.solrj.response.CollectionAdminResponse
import org.apache.solr.client.solrj.response.ConfigSetAdminResponse
import org.apache.solr.client.solrj.response.LukeResponse
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.response.SolrPingResponse
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.common.util.NamedList

/**
 * Looking at helper class to save solr_system (file crawl to start with content to solr
 */
class SolrSystemClient extends BaseClient {
    public final Logger log = LogManager.getLogger(this.class.name)

    /** upper limit of 'big' queries, e.g. folder lists for a gven crawl */
    public static int MAX_ROWS_RETURNED = 100000    // todo -- add batching if we have more rows than this to get for

    // todo -- move this to (BASE) Object hierarchy, leave solrsaver to do the actual saving, not building?
    public static String FLD_PARENT_ID = 'parentId_s'

    public static String FLD_DEPTH = 'depth_i'
    public static String FLD_PATH_T = 'path_txt_en'
    public static String FLD_NAME_S = 'name_s'
    public static String FLD_NAME_T = 'name_txt_en'

    public static String FLD_ARCHIVE = 'archive_b'
    public static String FLD_COMPRESSED = 'compressed_b'
    public static String FLD_IGNORE = 'ignore_b'
    public static String FLD_HIDDEN_B = "hidden_b"

    public static String FLD_CREATED_DATE = "createdDate_dt"          // pdate dynamic field
    public static String FLD_OWNER = "owner_s"                        // varies by operating system...

    public static String FLD_EXTENSION_SS = "extension_ss"

    /** tags are "human" labels */
    public static String FLD_TAG_SS = "tag_ss"
    /** labels are system (i.e. programatically) assigned */
    public static String FLD_LABELS = "label_ss"
    /** parent (group/folder) item labels are more relevant than ancestor/grandparent labels */
    public static String FLD_PARENT_LABELS = "parentLabel_ss"
    public static String FLD_ANCESTOR_LABELS = "ancestorLabel_ss"

//    public static String FLD_OS_NAME = "operatingSystem_s"
    public static String FLD_CHILD_ITEM_NAMES = "childNames_txt_en"
    public static String FLD_CHILD_ITEM_LABELS = "childLabels_ss"
    public static String FLD_CHILD_ITEM_COUNT = "childCount_i"
    public static String FLD_CHILD_GROUP_NAMES = "childGroupNames_txt_en"
    public static String FLD_CHILD_GROUP_COUNT = "childGroupCount_i"
//    public static String FLD_CHILD_GROUP_LABELS = "childGroupLabels_ss"

    public static String FLD_MATCHED_LABELS = "matchedLabels_ss"
    public static String FLD_MATCHED_LABELS_COUNT = "matchedLabels_i"

    public static String FLD_CONTENT_BODY = 'content_txt_en'
    public static String FLD_CONTENT_BODY_SIZE = 'contentSize_l'
    public static String FLD_METADATA = 'attr_metadata'
    public static String FLD_DIFFERENCES = 'attr_differences'

    Integer SOLR_BATCH_SIZE = 5000
    Integer MIN_FILE_SIZE = 10
    Integer MAX_CONTENT_SIZE = 1024 * 1000 * 100 // (100 MB of text?)

    Http2SolrClient solrClient

    /**
     * Create helper with a (non-thread safe???) solrClient that is configured for the solr server AND collection
     * todo -- revisit for better approach...
     * @param baseSolrUrl
     */
    SolrSystemClient(String baseSolrUrl = "http://localhost:8983/solr/${Constants.DEFAULT_APP_NAME}") {
        log.debug "Constructor baseSolrUrl:$baseSolrUrl"
        buildSolrClient(baseSolrUrl)
    }


    public void buildSolrClient(String baseSolrUrl) {
        log.info "\t\tBuild solr client with baseSolrUrl: $baseSolrUrl"
        solrClient = new Http2SolrClient.Builder(baseSolrUrl).build()
        log.debug "\t\tBuilt Solr Client: ${solrClient.baseURL}"
    }

    public void closeClient(String msg = 'general close call') {
        log.info "Closing solr client with message '$msg' ---- ($solrClient)"
        solrClient.close()
    }

    LinkedHashMap<String, Iterable<? extends Object>> getClusterStatus() {
        Map<String, Object> respMap = [:]
        final SolrRequest request = new CollectionAdminRequest.ClusterStatus();

        final NamedList<Object> response = solrClient.request(request);
        final NamedList<Object> cluster = (NamedList<Object>) response.get("cluster");
        final List<String> liveNodes = (List<String>) cluster.get("live_nodes");

        log.info("Found " + liveNodes.size() + " live nodes");
        respMap = [response: response, liveNodes: liveNodes]
        return respMap
    }

    def getSchemaInformation(String collectionName) {

//        LukeRequest lukeRequest = new LukeRequest("/$collectionName/admin/luke")
        LukeRequest lukeRequest = new LukeRequest()
        List<String> fields = 'id name_s name_txt_en path_s path_txt_en'.split(' ')
        lukeRequest.setFields(fields)
//        lukeRequest.addField('*')
        lukeRequest.includeIndexFieldFlags = true
        lukeRequest.setShowSchema(true)
        LukeResponse response = lukeRequest.process(solrClient, collectionName);
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

    def deleteCollection(String collectionName) throws SolrServerException, IOException {
        CollectionAdminRequest.Delete delete = CollectionAdminRequest.deleteCollection(collectionName)
        NamedList<Object> result = solrClient.request(delete)
        return result
    }


    def createConfigSet(String newConfigName, String sourceConfig = "_default") {
        final ConfigSetAdminRequest.Create adminRequest = new ConfigSetAdminRequest.Create();
        adminRequest.setConfigSetName(newConfigName);
        adminRequest.setBaseConfigSetName(sourceConfig);

        final CloudSolrClient cloudSolrClient = new CloudSolrClient.Builder(['http://localhost:8983/solr']).build();


        ConfigSetAdminResponse adminResponse = adminRequest.process(solrClient);

        log.info("createConfigSet adminResponse: $adminResponse")
        cloudSolrClient.close()
        return adminResponse
    }

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
        if (objects) {
            String firstId = objects[0].id
            log.debug "\t\t++++Adding solrInputDocuments, size: ${objects.size()} -- first ID:($firstId)"
            List<SolrInputDocument> solrInputDocuments = objects.collect { it.toPersistenceDocument() }
            try {
                resp = solrClient.add(solrInputDocuments, commitWithinMS)
                if (resp.status == 0) {
                    log.debug "saveObjects add response: $resp"
                } else {
                    log.warn "Bad response from solr save/commit?? $resp"
                }
            } catch (RemoteSolrException rse) {
                log.error "Remote Solr exception: $rse  (bad solr doc somewhere??"

            } catch (IOException ioe) {
                log.error "Solr server IO exception: $ioe"

            } catch (SolrServerException sse) {
                log.error "Solr server exception: $sse"
                throw sse
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

    def exportDocuments(SolrQuery solrQuery) {

    }


    @Override
    public String toString() {
        return "SolrSystemClient{" +
                "SOLR_BATCH_SIZE=" + SOLR_BATCH_SIZE +
                ", MIN_FILE_SIZE=" + MIN_FILE_SIZE +
                ", MAX_CONTENT_SIZE=" + MAX_CONTENT_SIZE +
                ", solrClient=" + solrClient +
//                ", detector=" + detector +
//                ", parser=" + parser +
//                ", handler=" + handler +
//                ", tikaConfig=" + tikaConfig +
                '}';
    }


    def getCollections() throws IOException, SolrServerException {
        CollectionAdminRequest.List req = new CollectionAdminRequest.List();
        CollectionAdminResponse response = req.process(solrClient);
        List<String> existingCollections = (List<String>) response.getResponse().get("collections");
        if (existingCollections == null) {
            existingCollections = new ArrayList<>();
        }
        Map collMap = [response:response, collections:existingCollections]
        return collMap
    }

    /**
     * convenience wrapper to check solr collection
     * @param collectionName
     * @param distributed
     * @return
     * @link https://solr.apache.org/guide/8_3/ping.html
     */
    SolrPingResponse ping(String collectionName = Constants.DEFAULT_APP_NAME, boolean distributed = false) {
        SolrPing ping = new SolrPing()
        if (distributed) {
            ping.getParams().add("distrib", "true"); //To make it a distributed request against a collection
        }
        SolrPingResponse rsp = ping.process(solrClient, collectionName);
    }
}
