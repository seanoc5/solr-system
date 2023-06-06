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
    UpdateResponse deleteDocuments(String deleteQuery, int commitWithinMS = 0) {
        log.warn "Clearing collection: ${this.solrClient.baseURL} -- deleteQuery: $deleteQuery (commit within: $commitWithinMS)"
        UpdateResponse ursp = solrClient.deleteByQuery(deleteQuery, commitWithinMS)
        int status = ursp.getStatus()
        if (status == 0) {
            log.info "\t\tSuccess clearing collection with query: $deleteQuery"
        } else {
            log.warn "FAILED to delete docs with delete query: ($deleteQuery) -- solr problem/error?? $ursp"
        }
        return ursp
    }


    /**
     * Take a list of file folders and save them to solr
     * todo -- revisit for better approach...
     * todo -- catch errors, general improvement
     * @param folders list of file folders to turn into solr input docs
     * @param dataSourceLabel name of group of files processed by this larger process
     * @param source name of machine, email account, browser account.... processed by this larger process
     * @return list of UpdateResponse objects from batched calls to solr
     */
/*
    List<UpdateResponse> saveFolderList(List<File> folders, String dataSourceLabel = 'unlabeled', String source='') {
        log.info "$dataSourceLabel) Save folders list (${folders.size()})..."
        List<UpdateResponse> updates = []
        List<SolrInputDocument> sidList = new ArrayList<>(SOLR_BATCH_SIZE)
        int i = 0
        folders.each { File folder ->
            i++
//            SolrInputDocument sid = folder.
            SolrInputDocument sid = createSolrInputFolderDocument(folder)
            sid.setField(Constants.FLD_CRAWL_NAME, dataSourceLabel)

            sidList << sid
            if (sidList.size() >= SOLR_BATCH_SIZE) {
                log.info "\t\t$i) Adding folder list batch (size: ${sidList.size()})..."
                UpdateResponse uresp = solrClient.add(sidList, 1000)
                log.debug "\t\t save folders Update resp: $uresp"
                updates << uresp
                sidList = new ArrayList<SolrInputDocument>(SOLR_BATCH_SIZE)
            }
        }

        if (sidList.size() > 0) {
            log.info "\t\t$i) ------ FINAL folder list batch added (size: ${sidList.size()}) ------"
            UpdateResponse uresp = solrClient.add(sidList, 1000)
            log.info "\t\t$i) update response: $uresp"
            updates << uresp
        }
        return updates
    }
*/


/*
    List<SolrInputDocument> buildFilesToCrawlInputList(Map<File, Map> filesToCrawl, String dsLabel = 'n.a.', String source = 'n.a.') {
        List<SolrInputDocument> inputDocuments = []
        if (filesToCrawl?.size() > 0) {
            filesToCrawl.each { File file, Map details ->
                String status = details?.status
                log.debug "File: $file -- Status: $status -- details: $details"
                SolrInputDocument sid = buildBasicTrackSolrFields(file)
                sid.setField(Constants.FLD_CRAWL_NAME, dsLabel)
                if (tikaConfig) {
                    if (details.status == Constants.STATUS_INDEX_CONTENT) {
                        log.debug "content tagged as worthy of indexing, send to proc to extra and add content: $file"
                        addSolrIndexFields(file, sid)

                    } else if (details.status == Constants.STATUS_ANALYZE) {
                        addSolrIndexFields(file, sid)
                        addSolrAnalyzeFields(file, sid)

                    } else {
                        log.debug "falling through to default 'track'"
                    }
                } else {
                    log.debug "No tika config, so skipping indexing the content"
                }
                inputDocuments << sid
            }
            if (inputDocuments.size() == 0) {
                log.debug "no docs??"
            }
        } else {
            log.debug "\t\tno files to crawl provided?? $filesToCrawl"
        }
        return inputDocuments
    }
*/


    /**
     * build solr docs with basic file info, light and fast tracking of files (like slocate)
     * @param filesToTrack
     * @return
     */
/*
    static SolrInputDocument buildBasicTrackSolrFields(File file, String dsLabel = '') {
        SolrInputDocument sid = new SolrInputDocument()
        String id = "${file.canonicalPath}--${file.lastModified()}"
        sid.setField(FLD_ID, id)
        sid.setField(FLD_TYPE, FILE)
        sid.setField(FLD_TAG_SS, TRACK)
        sid.setField(FLD_PATH_T, file.canonicalPath)
        sid.setField(FLD_PATH_S, file.canonicalPath)
        sid.setField(FLD_PARENT_PATH, file.parentFile.canonicalPath)
        sid.setField(FLD_NAME_S, file.name)
        sid.setField(FLD_NAME_T, file.name)
        if (dsLabel) {
            sid.setField(Constants.FLD_CRAWL_NAME, dsLabel)
        }

        if (file.isDirectory()) {
//            log.warn "Should not be a directory: $file"
            // todo -- make this whole class static
            sid.setField(FLD_DIRNAME_SS, file.name)
        } else if (file.isFile()) {
            sid.setField(FLD_FILENAME_SS, file.name)
        }
        String ext = FilenameUtils.getExtension(file.name)
        sid.setField(FLD_EXTENSION_SS, ext?.toLowerCase())
        sid.setField(FLD_SIZE, file.size())

        Date lmDate = new Date(file.lastModified())
        sid.setField(FLD_LASTMODIFIED, lmDate)

//        sid.addField(FLD_CRAWL_NAME, this.dataSourceName)

        return sid
    }
*/


    /**
     * add the 'index' fields to the input doc
     * @param filesToIndex
     * @return
     */
/*
    def addSolrIndexFields(File file, SolrInputDocument sid) {
        if (file.exists()) {
            if (file.size() > MIN_FILE_SIZE) {

                log.debug "\t\t\t add solr INDEX fields for file: ${file.absolutePath}"
                sid.addField(FLD_TAG_SS, 'index')
                // sid.setField('type_s', FILE)

                BodyContentHandler handler = new BodyContentHandler(-1)
                AutoDetectParser parser = new AutoDetectParser()
                Metadata metadata = new Metadata()

                metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, file.name)
//        metadata.set(Metadata.CONTENT_TYPE, attributes.getValue(ATTRIBUTE_CONTENT_TYPE));

                ParseContext context = new ParseContext()

//        TikaConfig config = TikaConfig.getDefaultConfig();
//        Detector detector = tikaConfig.getDetector()
                TikaInputStream stream = TikaInputStream.get(file.toPath())
//        MediaType mediaType = detector.detect(stream, metadata)

                try {
                    parser.parse(stream, handler, metadata, context)
                    String content = handler.toString()
                    if (content) {
                        log.debug "got some content, size: ${content.size()}"
                        if (content.size() > MAX_CONTENT_SIZE) {
                            log.warn "Trunacating content size (${content.size()}) to MAX_CONTENT_SIZE: $MAX_CONTENT_SIZE"
                        }
                        sid.addField(FLD_CONTENT_BODY, content)

                    } else {
                        log.info "\t\t${file.absolutePath}: no content...?"
                    }
                } catch (TikaException te) {
                    log.warn "Parsing error ($file): $te"
                }
            } else {
                log.warn "($file) File size (${file.size()}) less than MIN_FILE_SIZE, no parsing..."
            }
        } else {
            log.warn "($file) File does not exist! ${file.absolutePath}"
        }

    }
*/


    def commitUpdates(boolean waitFlush = false, boolean  waitSearcher = false) {
        log.info "Explicit call to solr 'commit' (consider allowing autocommit settings to do this for you...)"
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

    SolrDocumentList getSolrFolderDocs(LocalFileSystemCrawler crawler, String q = '*:*', String fq = "type_s:${FSFolder.TYPE}", String fl = SolrSystemClient.FIELDS_TO_CHECK.join(' ')) {
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
            log.info "Get existing solr folder docs map, size: ${docs.size()} -- Filters: ${sq.getFilterQueries()}"
        }
        Map<String, SolrDocument> docsMap = docs.groupBy {it.getFirstValue('id')}
        return docsMap
    }
}
