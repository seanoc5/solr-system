package com.oconeco.persistence


import com.oconeco.crawler.LocalFileSystemCrawler
import com.oconeco.models.FSFolder
import org.apache.log4j.Logger
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrRequest
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.request.CollectionAdminRequest
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.common.util.NamedList
import org.apache.tika.config.TikaConfig
import org.apache.tika.detect.Detector
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.Parser
import org.apache.tika.sax.BodyContentHandler

/**
 * Looking at helper class to save solr_system (file crawl to start with content to solr
 */
class SolrSystemClient {
    public static final Logger log = Logger.getLogger(this.class.name)

    public static int MAX_ROWS_RETURNED = 100000
    //upper limit of 'big' queries, e.g. folder lists for a gven crawl

    // todo -- move this to (BASE) Object hierarchy, leave solrsaver to do the actual saving, not building?
    public static String FLD_ID = 'id'
    public static String FLD_PARENT_ID = 'parentId_s'
    public static String FLD_CRAWL_NAME = "crawlName_s"
    public static String FLD_LOCATION_NAME = "locationName_s"

    public static String FLD_DEPTH = 'depth_i'
    public static String FLD_TYPE = 'type_s'
    public static String FLD_PATH_S = 'path_s'
    public static String FLD_PATH_T = 'path_txt_en'
    public static String FLD_NAME_S = 'name_s'
    public static String FLD_NAME_T = 'name_txt_en'
    public static String FLD_CONTENT_BODY = 'content_txt_en'
    public static String FLD_SIZE = "size_i"
    public static String FLD_DEDUP = 'dedup_s'

    public static String FLD_INDEX_DATETIME = "indexedTime_dt"        // pdate dynamic field
    public static String FLD_CREATED_DATE = "createdDate_dt"          // pdate dynamic field
    public static String FLD_LAST_MODIFIED = "lastModified_dt"        // pdate dynamic field
    // todo - improve owner name extraction/ETL
    public static String FLD_OWNER = "owner_s"                        // varies by operating system...


    public static String FLD_CHILD_FILENAMES = "childFileName_ss"
    public static String FLD_CHILD_DIRNAMES = "childDirName_ss"
    public static String FLD_HIDDENNAME_SS = "hiddenName_ss"
    public static String FLD_HIDDEN_B = "hidden_b"
    public static String FLD_EXTENSION_SS = "extension_ss"

    public static String FLD_SUBDIR_COUNT = 'subdirCount_i'
    public static String FLD_FILE_COUNT = "fileCount_i"
    public static String FLD_DIR_COUNT = "dirCount_i"

    public static String FLD_TAG_SS = "tag_ss"
    public static String FLD_LABELS = "label_ss"
//    public static  String FLD_CHILDASSIGNED_TYPES = "childAssignedTypes_ss"
//    public static  String FLD_SAME_NAME_COUNT = "sameNameCount_i"
//    public static  String FLD_NAME_SIZE_S = "nameSize_s"
//    public static  String FLD_HOSTNAME = "hostName_s"
    public static String FLD_OS_NAME = "operatingSystem_s"


    public static String FLD_IGNORED_FILES_COUNT = 'ignoredFilesCount_i'
    public static String FLD_IGNORED_FILES = 'ignoredFileNames_ss'
    public static String FLD_IGNORED_FOLDERS_COUNT = 'ignoredFoldersCount_i'
    public static String FLD_IGNORED_FOLDERS = 'ignoredFolders_ss'

    public static List<String> FIELDS_TO_CHECK = [
            FLD_ID,
            FLD_LAST_MODIFIED,
            FLD_SIZE,
            FLD_DEDUP,
            FLD_PATH_S,
            FLD_LOCATION_NAME,
            FLD_INDEX_DATETIME,
//            SolrSystemClient.FLD_,
    ]


    Integer SOLR_BATCH_SIZE = 5000
    Integer MIN_FILE_SIZE = 10
    Long MAX_CONTENT_SIZE = 1024 * 1000 * 10 // (10 MB of text?)
    Http2SolrClient solrClient

    Detector detector = null
    Parser parser = null
    BodyContentHandler handler = null
    TikaConfig tikaConfig

    /**
     * Create helper with a (non-thread safe???) solrClient that is configured for the solr server AND collection
     * todo -- revisit for better approach...
     * @param baseSolrUrl
     */
    SolrSystemClient(String baseSolrUrl) {
        log.debug "Constructor baseSolrUrl:$baseSolrUrl"
        buildSolrClient(baseSolrUrl)
    }

    SolrSystemClient(String baseSolrUrl, String hostName) {
//        this(baseSolrUrl)
        log.debug "\t\tConstructor baseSolrUrl:$baseSolrUrl --- Host/machine name:$hostName --- WITHOUT tika"
        buildSolrClient(baseSolrUrl)
    }

    SolrSystemClient(String baseSolrUrl, String hostName, TikaConfig tikaConfig) {
//        this(baseSolrUrl, dataSourceName)
        log.info "\t\tConstructor baseSolrUrl:$baseSolrUrl --- CrawlName:$hostName --- with TIKA config: $tikaConfig"
        buildSolrClient(baseSolrUrl)
        this.tikaConfig = tikaConfig
        detector = tikaConfig.getDetector()
        parser = new AutoDetectParser()
        handler = new BodyContentHandler()
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
    UpdateResponse deleteDocuments(String deleteQuery, int commitWithinMS = 0, boolean checkBeforeAfter = true) {
        Long beforeCount
        if(checkBeforeAfter){
            beforeCount = getDocumentCount(deleteQuery)
            log.info "\t\t$beforeCount count before query: $deleteQuery"
        }
        log.warn "Clearing collection: ${this.solrClient.baseURL} -- deleteQuery: $deleteQuery (commit within: $commitWithinMS)"
        UpdateResponse ursp = solrClient.deleteByQuery(deleteQuery, commitWithinMS)
        int status = ursp.getStatus()
        if (status == 0) {
            log.debug "\t\tSuccess clearing collection with query: $deleteQuery"
        } else {
            log.warn "FAILED to delete docs with delete query: ($deleteQuery) -- solr problem/error?? $ursp"
        }
        if(checkBeforeAfter){
            Long afterCount = getDocumentCount(deleteQuery)
            log.debug "\t\t$afterCount count AFTER query: $deleteQuery (diff: ${beforeCount - afterCount})"      // todo -- is this bad code? defaults somehow to not wait for commit? later call catches proper counter (0)???
        }

        return ursp
    }



    /**
     * get solr delete query for 'current' crawler info: locationName and crawlName
     * @param Specific crawler with locationName and crawlName
     * @return difference between before delete and after delete
     */
    Map<String, Object> deleteCrawledDocuments(LocalFileSystemCrawler crawler){
        Map<String, Object> results = [:]
        String deleteQuery = SolrSystemClient.FLD_LOCATION_NAME + ':"' + crawler.locationName + '" AND ' + com.oconeco.persistence.SolrSystemClient.FLD_CRAWL_NAME + ':"' + crawler.crawlName + '"'
        long preDeleteCount = getDocumentCount(deleteQuery)
        log.debug "\t\t------ DELETE query: $deleteQuery (pre-delete count: $preDeleteCount)"
        UpdateResponse deleteResponse = deleteDocuments(deleteQuery, 0)
        UpdateResponse commitResponse = commitUpdates(true, true)
        long postDeleteCount = getDocumentCount(deleteQuery)
        log.debug "\t\t------ POST-delete count:($postDeleteCount)"
        results = [deleteQuery:deleteQuery, preDeleteCount:preDeleteCount, postDeleteCount:postDeleteCount, deleteResponse:deleteResponse, commitResponse:commitResponse]
        return results
    }


    /**
     * force a commit.
     * NOTE: read up on explicit/forced commits, often better to leave solr to handle with proper soft/auto commits
     * @param waitFlush
     * @param waitSearcher
     * @return
     */
    def commitUpdates(boolean waitFlush = false, boolean  waitSearcher = false) {
        log.debug "\t\t____explicit call to solr 'commit' (consider allowing autocommit settings to do this for you...)"
        solrClient.commit(waitFlush,waitSearcher)
    }


    /**
     * wrapper to send collection of solrInputDocs to solr for processing
     * @param solrInputDocuments
     * @param commitWithinMS time to allow before solr autocommit (solr performance tuning/batching)
     * @return UpdateResponse
     */
    def saveDocs(ArrayList<SolrInputDocument> solrInputDocuments, int commitWithinMS = 1000) {
        log.debug "Adding solrInputDocuments, size: ${solrInputDocuments.size()}"
        UpdateResponse resp
        try {
            resp = solrClient.add(solrInputDocuments, commitWithinMS)
        } catch (SolrServerException sse) {
            log.error "Solr server exception: $sse"
        }
        return resp
    }


    /**
     * convenience method to get a count of documents in index
     * @param queryToCount (defaults to all docs)
     * @return
     */
    def long getDocumentCount(String queryToCount = '*:*') {
        SolrQuery sq = new SolrQuery(queryToCount)
        sq.setFields('')
        sq.setRows(0)
        QueryResponse resp = solrClient.query(sq)
        long docCount = resp.getResults().numFound
        return docCount
    }


    QueryResponse query(SolrQuery sq) {
        QueryResponse resp = solrClient.query(sq)
        return resp
    }

    QueryResponse query(String query) {
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


    /**
     * get the 'folder' documents for a given crawler (i.e. location name and crawl name)
     * @param crawler
     * @param q
     * @param fq
     * @param fl
     * @return
     */
    Map<String, SolrDocument> getSolrFolderDocs(LocalFileSystemCrawler crawler, String q = '*:*', String fq = "type_s:${FSFolder.TYPE}", String fl = SolrSystemClient.FIELDS_TO_CHECK.join(' ')) {
        SolrQuery sq = new SolrQuery('*:*')
        sq.setRows(MAX_ROWS_RETURNED)
        sq.setFilterQueries(fq)
        // add filter queries to further limit
        String filterLocation =SolrSystemClient.FLD_LOCATION_NAME + ':' + crawler.locationName
        sq.addFilterQuery(filterLocation)
        String filterCrawl =SolrSystemClient.FLD_CRAWL_NAME + ':' + crawler.crawlName
        sq.addFilterQuery(filterCrawl)

        sq.setFields(fl)

        QueryResponse response = query(sq)
        SolrDocumentList docs = response.results
        if (docs.size() == MAX_ROWS_RETURNED) {
            log.warn "getExistingFolders returned lots of rows (${docs.size()}) which equals our upper limit: ${MAX_ROWS_RETURNED}, this is almost certainly a problem.... ${sq}}"
        } else {
            log.debug "\t\tGet existing solr folder docs map, size: ${docs.size()} -- Filters: ${sq.getFilterQueries()}"
        }
        Map<String, SolrDocument> docsMap = docs.groupBy {it.getFirstValue('id')}
        return docsMap
    }
}
