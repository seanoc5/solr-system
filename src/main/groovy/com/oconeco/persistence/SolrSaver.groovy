package com.oconeco.persistence


import org.apache.log4j.Logger
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument
import org.apache.tika.config.TikaConfig
import org.apache.tika.detect.Detector
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.Parser
import org.apache.tika.sax.BodyContentHandler
/**
 * Looking at helper class to save solr_system (file crawl to start with content to solr
 */
class SolrSaver {
    public static final Logger log = Logger.getLogger(this.class.name)

    // todo -- move this to (BASE) Object hierarchy, leave solrsaver to do the actual saving, not building?
    public static final String FOLDER = 'Folder'
    public static final String FILE = 'File'

    public static final String TRACK = 'track'
    public static final String ANALYZE = 'analyze'

    public static final String FLD_ID = 'id'
    public static final String FLD_CRAWL_NAME = "crawlName_s"
    public static final String FLD_LOCATION_NAME = "locationName_s"

    public static final String FLD_DEPTH = 'depth_i'
    public static final String FLD_TYPE = 'type_s'
    public static final String FLD_PATH_S = 'path_s'
    public static final String FLD_PATH_T = 'path_t'
    public static final String FLD_NAME_S = 'name_s'
    public static final String FLD_NAME_T = 'name_t'
    public static final String FLD_CONTENT_BODY = 'content_txt_en'
    public static final String FLD_PARENT_PATH = 'parentPath_s'
    public static final String FLD_LASTMODIFIED = 'lastModified_dt'
    public static final String FLD_FILENAME_SS = "fileName_ss"
    public static final String FLD_DIRNAME_SS = "dirName_ss"
    public static final String FLD_HIDDENNAME_SS = "hiddenName_ss"
    public static final String FLD_EXTENSION_SS = "extension_ss"

    public static final String FLD_SUBDIR_COUNT = 'subdirCount_i'
    public static final String FLD_FILE_COUNT = "fileCount_i"
    public static final String FLD_DIR_COUNT = "dirCount_i"
    public static final String FLD_SIZE = "size_i"
    public static final String FLD_TAG_SS = "tag_ss"
    public static final String FLD_ASSIGNED_TYPES = "assignedTypes_ss"
    public static final String FLD_CHILDASSIGNED_TYPES = "childAssignedTypes_ss"
    public static final String FLD_SAME_NAME_COUNT = "sameNameCount_i"
    public static final String FLD_NAME_SIZE_S = "nameSize_s"
    public static final String FLD_HOSTNAME = "hostName_s"
    public static final String FLD_INDEX_DATETIME = "indexedTime_dt"            // pdate dynamic field

    public static String FLD_IGNORED_FILES_COUNT = 'ignoredFilesCount_i'
    public static String FLD_IGNORED_FILES = 'ignoredFileNames_ss'
    public static String FLD_IGNORED_FOLDERS_COUNT = 'ignoredFoldersCount_i'
    public static String FLD_IGNORED_FOLDERS = 'ignoredFolders_ss'
    public static String FLD_UNIQUE = 'unique_s'

    final Integer SOLR_BATCH_SIZE = 5000
    final Integer MIN_FILE_SIZE = 10
    Long MAX_CONTENT_SIZE = 1024 * 1000 * 10 // (10 MB of text?)
    Http2SolrClient solrClient

    Detector detector = null
    Parser parser = null
    BodyContentHandler handler = null
    TikaConfig tikaConfig
//    String dataSourceName = 'undefined'
    String hostName = 'unknown host'

    /**
     * Create helper with a (non-thread safe???) solrClient that is configured for the solr server AND collection
     * todo -- revisit for better approach...
     * @param baseSolrUrl
     */
    SolrSaver(String baseSolrUrl) {
        log.info "Constructor baseSolrUrl:$baseSolrUrl"
        buildSolrClient(baseSolrUrl)
    }

    SolrSaver(String baseSolrUrl, String hostName) {
//        this(baseSolrUrl)
        log.info "\t\tConstructor baseSolrUrl:$baseSolrUrl --- Host/machine name:$hostName --- WITHOUT tika"
        this.hostName = hostName
        buildSolrClient(baseSolrUrl)
    }

    SolrSaver(String baseSolrUrl, String hostName, TikaConfig tikaConfig) {
//        this(baseSolrUrl, dataSourceName)
        log.info "\t\tConstructor baseSolrUrl:$baseSolrUrl --- CrawlName:$hostName --- with TIKA config: $tikaConfig"
        this.hostName = hostName
        buildSolrClient(baseSolrUrl)
        this.tikaConfig = tikaConfig
        detector = tikaConfig.getDetector()
        parser = new AutoDetectParser()
        handler = new BodyContentHandler()
    }

    public void buildSolrClient(String baseSolrUrl) {
        log.info "\t\tBuild solr client with baseSolrUrl: $baseSolrUrl"
//        solrClient = new HttpSolrClient.Builder(baseSolrUrl).build()
        solrClient = new Http2SolrClient.Builder(baseSolrUrl).build()
        log.info "\t\tBuilt Solr Client: $solrClient"
    }

    public void closeClient(String msg = 'general close call'){
        log.info "Closing solr client with message '$msg' ---- ($solrClient)"
        solrClient.close()
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
    UpdateResponse deleteDocuments(String deleteQuery, int commitWithinMS = 10) {
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
     * Create Solr Input doc
     *
     */
//    SolrInputDocument createSolrInputFolderDocument(File folder) {
//        FSFolder folderFS = new FSFolder(folder)
//        return folderFS.toSolrInputDocument()
//    }


    /**
     * Create Solr Input doc from archive entry
     *
     */
//    SolrInputDocument createSolrInputFolderDocument(ArchiveEntry) {
//        SolrInputDocument sid = new SolrInputDocument()
//
//        return sid
//    }


    /**
     * Take a list of file folders and save them to solr
     * todo -- revisit for better approach...
     * todo -- catch errors, general improvement
     * @param folders  list of file folders to turn into solr input docs
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


/*
    static void addSolrAnalyzeFields(File file, SolrInputDocument sid) {
        sid.setField(FLD_TAG_SS, ANALYZE)
        log.debug "\t\tsetting file: $file to tag: $ANALYZE"
    }
*/


    /**
     * quick little wrapper in case we get fancy later
     * @param fileDocs
     * @return
     */
/*
    UpdateResponse saveFilesCollection(Collection<SolrInputDocument> fileDocs, int msToCommit = 1000) {
        UpdateResponse ursp = null
        if (fileDocs) {
            try {
                ursp = solrClient.add(fileDocs, msToCommit)
                log.debug "Update response: $ursp"
            } catch (Exception e) {
                log.error "Exception: $e"
            }
        } else {
            log.warn "No docs to commit... $fileDocs (don't call thing if you don't need thing...)"
        }
        return ursp
    }
*/


    def commitUpdates() {
        solrClient.commit()
    }


    /**
     * wrapper to send collection of solrInputDocs to solr for processing
     * @param solrInputDocuments
     * @return UpdateResponse
     */
    def saveDocs(ArrayList<SolrInputDocument> solrInputDocuments) {
        log.info "Adding solrInputDocuments, size: ${solrInputDocuments.size()}"
        UpdateResponse resp = solrClient.add(solrInputDocuments, 1000)
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

    QueryResponse query(String query = '*:*') {
        SolrQuery sq = new SolrQuery(query)
//        sq.setFields('')
//        sq.setRows(0)
        QueryResponse resp = solrClient.query(sq)
        return resp
    }


    @Override
    public String toString() {
        return "SolrSaver{" +
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
