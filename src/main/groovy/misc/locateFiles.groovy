package misc

import com.oconeco.helpers.Constants
import com.oconeco.helpers.SolrCrawlArgParser
import com.oconeco.persistence.SolrSaver
import org.apache.log4j.Logger

/**
 * Script to function much like slocate in linux.
 * Crawl all folders (with skip configs to skip things we don't care about).
 * When folders are complete:
 * <li>crawl all files (with config to skip files</li>
 * <li>also configs on what should have contents extracted
 * <li>and config on what content should be "analyzed"
 */
Logger log = Logger.getLogger(this.class.name);
log.info "Start ${this.class.name}, with args: $args"

ConfigObject config = SolrCrawlArgParser.parse(this.class.simpleName, args)

Map<String,String> startFolders = config.dataSources.localFolders
String solrUrl = config.solrUrl         //options.solrUrl
log.info "Start folders: $startFolders -- solr url: $solrUrl"
String source = config.sourceName ?: 'undefined'

// todo - replace 'Documents Locate' with param
SolrSaver solrSaver = new SolrSaver(solrUrl, source)
log.info "Solr Saver created: $solrSaver"

boolean wipeContent = config.wipeContent

def fileNamePatterns = config.namePatterns.files
def folderNamePatterns = config.namePatterns.folders
Long fileCount = 0
Date startTimeAll = new Date()

startFolders.each { String dsLabel, String sf ->
    Date startTimeFolder = new Date()

    File startFolder = new File(sf)
    log.info "Crawling parent folder: $dsLabel :: $sf"

    if (wipeContent) {
        def response = wipeSavedContent(source, dsLabel, solrSaver)
    } else {
        log.debug "Not wiping content/collection, "
    }


    // get list of non-noisy folders to crawl (or compare against stored info (check if we need to crawl...
    log.error "Need to revise this code, switch from LocalFileCrawler to LocalFileSystemCrawler"
    Map<String, List<File>> resultsMap = null       //LocalFileCrawler.getFoldersToCrawlMap(startFolder, folderNamePatterns)


    List<File> crawlFolders = resultsMap.foldersToCrawl
//    Set<File> foldersToCrawl = LocalFileCrawler.getFoldersToCrawl(startFolder, fileNamePatterns)
    log.info "Folders to crawl(${crawlFolders.size()})"
    log.debug "\t\tFolders to crawl: ${crawlFolders}"

    def uresplist = solrSaver.saveFolderList(crawlFolders, dsLabel)
    log.debug "Starting Folder ($sf) Update responses: $uresplist"

    log.info "Crawl Folders size: ${crawlFolders.size()}"

    crawlFolders.each { File crawlableFolder ->
//        change with something else
//        Map<File, Map> filesToCrawlMap = LocalFileCrawler.getFilesToCrawlList(crawlableFolder, LocalFileCrawler.FILE_REGEX_TO_SKIP_DEFAULT, LocalFileCrawler.FILE_EXTENSIONS_TO_INDEX, LocalFileCrawler.FILE_EXTENSIONS_TO_ANALYZE)
//        log.info "\t$fileCount) folder: ${crawlableFolder.absolutePath} -- files to crawl count: ${filesToCrawlMap.size()}"
//        fileCount += filesToCrawlMap.size()
//        List<SolrInputDocument> sidList = solrSaver.buildFilesToCrawlInputList(filesToCrawlMap, dsLabel)
//        if (sidList) {
////            log.info "Files to crawl list size: ${filesToCrawlMap.size()}"
//            UpdateResponse response = solrSaver.saveFilesCollection(sidList)
//            log.debug "Update response: $response"
//        } else {
//            log.info "\t\tSkipping empty crawl list: $sidList (folder: $crawlableFolder)"
//        }
    }

    Date end = new Date()
    use(groovy.time.TimeCategory) {
        def duration = end - startTimeFolder
        log.info "Crawl Folder ($startFolder) duration: minutes: ${duration.minutes} -- seconds: ${duration.seconds} -- Hours: ${duration.hours}"
    }
    log.debug "\t\t next folder..."

}

public void wipeSavedContent(String source, String dsLabel, SolrSaver solrSaver) {
    String query = "${Constants.FLD_CRAWL_NAME}:\"${dsLabel}\""
    String sourceFilter = ""
    long existingCount = solrSaver.getDocumentCount(query)
    log.info "Found existing count: $existingCount"
    if (existingCount > 0) {
        log.warn "Wiping previous crawl data (BEWARE!!!), label:'$dsLabel'!!!! - "
        def foo = solrSaver.deleteDocuments(query)
        log.info "Clear results: $foo"
    } else {
        log.info "\t\tskipping delete command for data source '$dsLabel', since we found $existingCount (0, right?) existing docs in current data source -- delete query would have been: $query"
    }
}

Date endTimeAll = new Date()
use(groovy.time.TimeCategory) {
    def duration = endTimeAll - startTimeAll
    log.info "Crawl duration: minutes: ${duration.minutes} -- seconds: ${duration.seconds} -- Hours: ${duration.hours} -- Days: ${duration.days}, "
}

def commitResponse = solrSaver.commitUpdates()
log.info "Commited: $commitResponse"

log.info "done!?"
