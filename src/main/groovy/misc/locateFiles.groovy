package misc

import com.oconeco.crawler.LocalFileCrawler
import com.oconeco.helpers.SolrCrawlArgParser
import com.oconeco.persistence.SolrSaver
import org.apache.log4j.Logger
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument
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


def options = SolrCrawlArgParser.parse(this.class.simpleName, args)


String configLocation = options.config
File cfgFile = new File(configLocation)
def resourcesRoot = getClass().getResource('/')
log.info "Starting with resources root: $resourcesRoot"
if (!cfgFile.exists()) {
    URL cfgUrl = getClass().getResource(configLocation)
    log.warn "could not find config file: ${cfgFile.absolutePath}, trying resource path: $cfgUrl"
    if (cfgUrl) {
        cfgFile = new File(cfgUrl.toURI())
        if (cfgFile.exists()) {
            log.info "Found config file (${configLocation}) in resources: ${cfgFile.absolutePath}"
        } else {
            throw new IllegalArgumentException("Config file: $cfgFile does not exist, bailing...")
        }
    } else {
        log.warn "No config url found?? "
    }
} else {
    log.info "cfg file: ${cfgFile.absolutePath}"
}
ConfigObject config = new ConfigSlurper().parse(cfgFile.toURI().toURL())

Map<String,String> startFolders = config.dataSources.localFolders
String solrUrl = config.solrUrl         //options.solrUrl
log.info "Start folders: $startFolders -- solr url: $solrUrl"


SolrSaver solrSaver = new SolrSaver(solrUrl, 'Documents Locate')
log.info "Solr Saver created: $solrSaver"

boolean wipeContent = options.wipeContent
if (wipeContent) {
    log.warn "Wiping previous crawl data (BEWARE!!!)..."
    def foo = solrSaver.clearCollection()
    log.info "Clear results: $foo"
} else {
    log.debug "Not wiping content/collection, "
}

def fileNamePatterns = config.files.namePatterns
def folderNamePatterns = config.folders.namePatterns
Long fileCount = 0
Date startTimeAll = new Date()

startFolders.each { String label, String sf ->
    Date startTimeFolder = new Date()

    File startFolder = new File(sf)
    log.info "Crawling parent folder: $label :: $sf"

    Date start = new Date()
    solrSaver.setDataSourceName(label)

    // get list of non-noisy folders to crawl (or compare against stored info (check if we need to crawl...
    Map<String, List<File>> resultsMap = LocalFileCrawler.getFoldersToCrawlMap(startFolder, fileNamePatterns)
    List<File> crawlFolders = resultsMap.foldersToCrawl
//    Set<File> foldersToCrawl = LocalFileCrawler.getFoldersToCrawl(startFolder, fileNamePatterns)
    log.info "Folders to crawl(${crawlFolders.size()})"
    log.debug "\t\tFolders to crawl: ${crawlFolders}"


    def uresplist = solrSaver.saveFolderList(crawlFolders)
    log.debug "Starting Folder ($sf) Update responses: $uresplist"

    log.info "Crawl Folders size: ${crawlFolders.size()}"


    crawlFolders.each { File crawlableFolder ->
        Map<File, Map> filesToCrawlMap = LocalFileCrawler.getFilesToCrawlList(crawlableFolder, LocalFileCrawler.FILE_REGEX_TO_SKIP, LocalFileCrawler.FILE_EXTENSIONS_TO_INDEX, LocalFileCrawler.FILE_EXTENSIONS_TO_ANALYZE)
        log.info "\t\t$fileCount) folder: ${crawlableFolder.absolutePath} -- files to crawl count: ${filesToCrawlMap.size()}"
        fileCount += filesToCrawlMap.size()
        List<SolrInputDocument> sidList = solrSaver.buildFilesToCrawlInputList(filesToCrawlMap)
        if (sidList) {
//            log.info "Files to crawl list size: ${filesToCrawlMap.size()}"
            UpdateResponse response = solrSaver.saveFilesCollection(sidList)
            log.debug "Update response: $response"
        } else {
            log.info "\t\tSkipping empty crawl list: $sidList (folder: $crawlableFolder)"
        }
    }

    Date end = new Date()
    use(groovy.time.TimeCategory) {
        def duration = end - startTimeFolder
        log.info "Crawl Folder ($startFolder) duration: minutes: ${duration.minutes} -- seconds: ${duration.seconds} -- Hours: ${duration.hours}"
    }
    log.debug "\t\t next folder..."

}
Date endTimeAll = new Date()
use(groovy.time.TimeCategory) {
    def duration = endTimeAll - startTimeAll
    log.info "Crawl duration: minutes: ${duration.minutes} -- seconds: ${duration.seconds} -- Hours: ${duration.hours} -- Days: ${duration.days}, "
}

def commitResponse = solrSaver.commitUpdates()
log.info "Commited: $commitResponse"

log.info "done!?"
