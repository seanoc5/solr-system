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
    public static final String FOLDER = 'Folder'
    public static final String FILE = 'File'
    public static final String TRACK = 'track'
    public static final String ANALYZE = 'analyze'
    public static final Logger log = Logger.getLogger(this.class.name)
    final Integer SOLR_BATCH_SIZE = 1000
    final Integer MIN_FILE_SIZE = 10
    Long MAX_CONTENT_SIZE = 1024 * 1000 * 10 // (10 MB of text?)
    HttpSolrClient solrClient

    Detector detector = null
    Parser parser = null
    BodyContentHandler handler = null
    TikaConfig tikaConfig

    /**
     * Create helper with a (non-thread safe???) solrClient that is configured for the solr server AND collection
     * todo -- revisit for better approach...
     * @param baseSolrUrl
     */
    SolrSaver(String baseSolrUrl) {
        solrClient = new HttpSolrClient.Builder(baseSolrUrl).build()
        tikaConfig = TikaConfig.getDefaultConfig()
        detector = tikaConfig.getDetector()
        parser = new AutoDetectParser()
        handler = new BodyContentHandler()
    }


    /**
     * Clear the collection of current data -- BEWARE
     *
     */
    UpdateResponse clearCollection() {
        log.warn "Clearing collection: ${this.solrClient.baseURL}"
        UpdateResponse ursp = solrClient.deleteByQuery("type_s:($FILE $FOLDER)", 100)
//        def orsp = solrClient.optimize()
//        log.info "Optimized as well (overkill?), response:$orsp"
        return ursp
    }


    /**
     * Create Solr Input doc
     *
     */
    SolrInputDocument createSolrInputFolderDocument(File folder) {
        SolrInputDocument sid = new SolrInputDocument()
        String id = "${folder.canonicalPath}--${folder.lastModified()}"
        sid.setField('id', id)
        sid.setField('type_t', FOLDER)
        sid.setField('path_t', folder.canonicalPath)
        sid.setField('parent_path_s', folder.parentFile.canonicalPath)
        sid.setField('name_s', folder.name)
        sid.setField('name_t', folder.name)

        Date lmDate = new Date(folder.lastModified())
        sid.setField('lastModified', lmDate)

        int fileCount = 0
        int subDirCount = 0
        folder.eachFile { File file ->
            String fname = file.name
            if (file.isFile()) {
                fileCount++
                sid.addField("filename_ss", fname)
            } else if (file.isDirectory()) {
                subDirCount++
                sid.addField("dirname_ss", fname)
            }
            if (file.isHidden()  /*.name.startsWith(".")*/) {
                sid.addField("hiddenname_ss", fname)
            } else {
                def parts = file.name.split('\\.')
                if (parts.size() > 1) {
                    sid.addField("extension_ss", parts[-1])
                }
            }
        }
//        int subdirCount = getSubdirCount(folder)
        sid.addField('subdir_count_i', subDirCount)
        sid.addField("fileCount_i", fileCount)
        sid.addField("dirCount_i", subDirCount)
        sid.addField("data_source_s", this.class.simpleName)
        // note: this is a Lucidworks Fusion default "system" field, using it here for my convenience

        return sid
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
        folders.each { File folder ->
            SolrInputDocument sid = createSolrInputFolderDocument(folder)

            sidList << sid
            if (sidList.size() >= SOLR_BATCH_SIZE) {
                log.info "\t\tAdding folder list batch (size: ${sidList.size()})..."
                UpdateResponse uresp = solrClient.add(sidList, 1000)
                log.debug "\t\t save folders Update resp: $uresp"
                updates << uresp
                sidList = new ArrayList<SolrInputDocument>(SOLR_BATCH_SIZE)
            }
        }
        if (sidList.size() > 0) {
            log.info "\t\tAdding final folder list batch (size: ${sidList.size()})..."
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
                if (details.status == LocalFileCrawler.STATUS_INDEX) {
                    log.debug "test 2..."
                    addSolrIndexFields(file, sid)

                } else if (details.status == LocalFileCrawler.STATUS_ANALYZE) {
                    addSolrIndexFields(file, sid)
                    addSolrAnalyzeFields(file, sid)

                } else {
                    log.debug "falling through to default 'track'"
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
        sid.setField('id', id)
        sid.setField('type_s', FILE)
        sid.setField('tag_ss', TRACK)
        sid.setField('path_s', file.canonicalPath)
        sid.setField('path_txt_en', file.canonicalPath)
        sid.setField('parent_path_s', file.parentFile.canonicalPath)
        sid.setField('name_s', file.name)
        sid.setField('name_txt_en', file.name)

        if (file.isDirectory()) {
//            log.warn "Should not be a directory: $file"
            // todo -- make this whole class static
            sid.setField('dirname_s', file.name)
        } else if (file.isFile()) {
            sid.setField('filename_s', file.name)
        }
        String ext = FilenameUtils.getExtension(file.name)
        sid.setField('exension_s', ext)
        sid.setField('size_i', file.size())

        Date lmDate = new Date(file.lastModified())
        sid.setField('lastModified_tdt', lmDate)
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
                sid.addField('tag_ss', 'index')
//        sid.setField('type_s', FILE)
                sid.addField("data_source_s", this.class.simpleName)
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
                        sid.addField('content_txt_en', content)

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
        sid.setField('tags', ANALYZE)
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

}
