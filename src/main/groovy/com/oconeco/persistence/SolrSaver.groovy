package com.oconeco.persistence

import com.oconeco.crawler.LocalFileCrawler
import org.apache.commons.io.FilenameUtils
import org.apache.log4j.Logger
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument
import org.apache.tika.config.TikaConfig
import org.apache.tika.detect.Detector
import org.apache.tika.exception.TikaException
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import org.apache.tika.metadata.TikaCoreProperties
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
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
    public static final String FLD_DEPTH = 'depth_i'
    public static final String FLD_TYPE = 'type_s'
    public static final String FLD_PATH_S = 'path_s'
    public static final String FLD_PATH_T = 'path_t'
    public static final String FLD_NAME_S = 'name_s'
    public static final String FLD_NAME_T = 'name_t'
    public static final String FLD_CONTENT_BODY = 'content_txt_en'
    public static final String FLD_PARENT_PATH = 'parent_path_s'
    public static final String FLD_LASTMODIFIED = 'lastModified'
    public static final String FLD_FILENAME_SS = "filename_ss"
    public static final String FLD_DIRNAME_SS = "dirname_ss"
    public static final String FLD_HIDDENNAME_SS = "hiddenname_ss"
    public static final String FLD_EXTENSION_SS = "extension_ss"
    public static final String FLD_SUBDIR_COUNT = 'subdir_count_i'
    public static final String FLD_FILE_COUNT = "fileCount_i"
    public static final String FLD_DIR_COUNT = "dirCount_i"
    public static final String FLD_DATA_SOURCE = "data_source_s"
    public static final String FLD_TAG_SS = "tag_ss"
    public static final String FLD_ASSIGNED_TYPES = "assignedTypes_ss"
    public static final String FLD_CHILDASSIGNED_TYPES = "childAssignedTypes_ss"
    public static final String FLD_SIZE = "size_i"
    public static final String FLD_SAME_NAME_COUNT = "sameNameCount_i"
    public static final String FLD_NAME_SIZE_S = "nameSize_s"

    public static String FLD_IGNORED_FILES_COUNT = 'ignoredFilesCount_i'
    public static String FLD_IGNORED_FILES = 'ignoredFiles'
    public static String FLD_IGNORED_FOLDERS_COUNT = 'ignoredFoldersCount_i'
    public static String FLD_IGNORED_FOLDERS = 'ignoredFolders'

    final Integer SOLR_BATCH_SIZE = 5000
    final Integer MIN_FILE_SIZE = 10
    Long MAX_CONTENT_SIZE = 1024 * 1000 * 10 // (10 MB of text?)
    HttpSolrClient solrClient

    Detector detector = null
    Parser parser = null
    BodyContentHandler handler = null
    TikaConfig tikaConfig
    String dataSourceName = 'undefined'

    /**
     * Create helper with a (non-thread safe???) solrClient that is configured for the solr server AND collection
     * todo -- revisit for better approach...
     * @param baseSolrUrl
     */
    SolrSaver(String baseSolrUrl, String dataSourceName) {
        log.info "Constructor baseSolrUrl:$baseSolrUrl, CrawlName:$dataSourceName, WITHOUT tika"
        solrClient = new HttpSolrClient.Builder(baseSolrUrl).build()
        this.dataSourceName = dataSourceName
    }

    SolrSaver(String baseSolrUrl, String dataSourceName, TikaConfig tikaConfig) {
        log.info "Constructor baseSolrUrl:$baseSolrUrl, CrawlName:$dataSourceName, with TIKA config: $tikaConfig"
        solrClient = new HttpSolrClient.Builder(baseSolrUrl).build()
        this.tikaConfig = tikaConfig
        detector = tikaConfig.getDetector()
        parser = new AutoDetectParser()
        handler = new BodyContentHandler()
        this.dataSourceName = dataSourceName
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
    UpdateResponse clearCollection(def pathToClear) {
        log.warn "Clearing collection: ${this.solrClient.baseURL} -- pathToClear: $pathToClear"
        UpdateResponse ursp = solrClient.deleteByQuery("type_s:($FILE $FOLDER)", 100)
        int status = ursp.getStatus()
        if(status == 0){
            log.debug "\t\tSuccess clearing collection with path to clear: $pathToClear"
        } else {
            log.warn "FAILED to clear path: $pathToClear -- solr problem/error?? $ursp"
        }
//        def orsp = solrClient.optimize()
//        log.info "Optimized as well (overkill?), response:$orsp"
        return ursp
    }


    /**
     * Create Solr Input doc
     *
     */
    SolrInputDocument createSolrInputFolderDocument(File folder) {
    }


    /**
     * Take a list of file folders and save them to solr
     * todo -- revisit for better approach...
     * todo -- catch errors, general improvement
     * @param folders
     * @return
     */
    List<UpdateResponse> saveFolderList(Set<File> folders) {
        log.info "Save folders list (${folders.size()})..."
        List<UpdateResponse> updates = []
        List<SolrInputDocument> sidList = new ArrayList<>(SOLR_BATCH_SIZE)
        int i = 0
        folders.each { File folder ->
            i++
            SolrInputDocument sid = createSolrInputFolderDocument(folder)

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
            log.info "\t\t$i) Adding final folder list batch (size: ${sidList.size()})..."
            UpdateResponse uresp = solrClient.add(sidList, 1000)
            updates << uresp
        }
        return updates
    }


    List<SolrInputDocument> buildFilesToCrawlInputList(Map<File, Map> filesToCrawl) {
        List<SolrInputDocument> inputDocuments = []
        if(filesToCrawl?.size()>0) {
            filesToCrawl.each { File file, Map details ->
                String status = details?.status
                log.debug "File: $file -- Status: $status -- details: $details"
                SolrInputDocument sid = buildBasicTrackSolrFields(file)
                if(tikaConfig) {
                    if (details.status == LocalFileCrawler.STATUS_INDEX_CONTENT) {
                        log.debug "test 2..."
                        addSolrIndexFields(file, sid)

                    } else if (details.status == LocalFileCrawler.STATUS_ANALYZE) {
                        addSolrIndexFields(file, sid)
                        addSolrAnalyzeFields(file, sid)

                    } else {
                        log.debug "falling through to default 'track'"
                    }
                } else{
                    log.debug "No tika config, so skipping indexing the content"
                }
                inputDocuments << sid
            }
            if (inputDocuments.size() == 0) {
                log.debug "no docs??"
            }
        } else {
            log.warn "\t\tno files to crawl provided?? $filesToCrawl"
        }
        return inputDocuments
    }


    /**
     * build solr docs with basic file info, light and fast tracking of files (like slocate)
     * @param filesToTrack
     * @return
     */
    static SolrInputDocument buildBasicTrackSolrFields(File file) {
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

        if (file.isDirectory()) {
//            log.warn "Should not be a directory: $file"
            // todo -- make this whole class static
            sid.setField(FLD_DIRNAME_SS, file.name)
        } else if (file.isFile()) {
            sid.setField(FLD_FILENAME_SS, file.name)
        }
        String ext = FilenameUtils.getExtension(file.name)
        sid.setField(FLD_EXTENSION_SS, ext)
        sid.setField(FLD_SIZE, file.size())

        Date lmDate = new Date(file.lastModified())
        sid.setField(FLD_LASTMODIFIED, lmDate)
        return sid
    }


    /**
     * add the 'index' fields to the input doc
     * @param filesToIndex
     * @return
     */
    def addSolrIndexFields(File file, SolrInputDocument sid) {
        if (file.exists()) {
            if (file.size() > MIN_FILE_SIZE) {

                log.debug "\t\t\t add solr INDEX fields for file: ${file.absolutePath}"
                sid.addField(FLD_TAG_SS, 'index')
//        sid.setField('type_s', FILE)
                sid.addField(FLD_DATA_SOURCE, this.dataSourceName)
                // note: this is a Lucidworks Fusion default "system" field, using it here for my convenience

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
                } catch (TikaException te){
                    log.warn "Parsing error ($file): $te"
                }
            } else {
                log.warn "($file) File size (${file.size()}) less than MIN_FILE_SIZE, no parsing..."
            }
        } else {
            log.warn "($file) File does not exist! ${file.absolutePath}"
        }

    }


    static void addSolrAnalyzeFields(File file, SolrInputDocument sid) {
        sid.setField(FLD_TAG_SS, ANALYZE)
        log.debug "\t\tsetting file: $file to tag: $ANALYZE"
    }


    /**
     * quick little wrapper in case we get fancy later
     * @param fileDocs
     * @return
     */
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
            log.warn "No docs to commit... $fileDocs (don't call me if you don't need me...)"
        }
        return ursp
    }


    def commitUpdates() {
        solrClient.commit()
    }

    def saveDocs(ArrayList<SolrInputDocument> solrInputDocuments) {
        solrInputDocuments.each {SolrInputDocument sid ->
            sid.setField(FLD_DATA_SOURCE, this.dataSourceName)
        }
        UpdateResponse resp = solrClient.add(solrInputDocuments, 1000)
        return resp
    }
}
