package com.oconeco.crawler

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
String solrUrl = options.solrUrl

def startFolders = options.fs
log.info "Start folders: $startFolders"
String configLocation = options.config
File cfgFile = new File(configLocation)
if (!cfgFile.exists()) {
    cfgFile = new File(getClass().getResource(configLocation).toURI())
    if (cfgFile.exists()) {
        log.info "Found config file (${configLocation}) in resources: ${cfgFile.absolutePath}"
    } else {
        throw new IllegalArgumentException("Config file: $cfgFile does not exist, bailing...")
    }
} else {
    log.info "cfg file: ${cfgFile.absolutePath}"
}
ConfigObject config = new ConfigSlurper().parse(cfgFile.toURI().toURL())

Date start = new Date()
SolrSaver solrSaver = new SolrSaver(solrUrl, 'Documents Locate')
boolean wipeContent = options.wipeContent
if (wipeContent){
    log.warn "Wiping previous crawl data (BEWARE!!!)..."
    def foo = solrSaver.clearCollection()
    log.info "Clear results: $foo"
}

def fileNamePatterns = config.files.namePatterns
def folderNamePatterns = config.folders.namePatterns
Long fileCount = 0
startFolders.each { String sf ->
    File startFolder = new File(sf)
    log.info "Crawling parent folder: $startFolder"

    // get list of non-noisy folders to crawl (or compare against stored info (check if we need to crawl...
    Set<File> foldersToCrawl = LocalFileCrawler.getFoldersToCrawl(startFolder, fileNamePatterns)

    def uresplist = solrSaver.saveFolderList(foldersToCrawl)
    log.debug "Starting Folder ($sf) Update responses: $uresplist"

    log.info "Folders size: ${foldersToCrawl.size()}"


    foldersToCrawl.each { File crawlableFolder ->
        log.info "$fileCount) folder: ${crawlableFolder.absolutePath}"
        Map<File, Map> filesToCrawlMap = LocalFileCrawler.getFilesToCrawlList(crawlableFolder, LocalFileCrawler.FILE_REGEX_TO_SKIP, LocalFileCrawler.FILE_EXTENSIONS_TO_INDEX, LocalFileCrawler.FILE_EXTENSIONS_TO_ANALYZE)
        fileCount += filesToCrawlMap.size()
        List<SolrInputDocument> sidList = solrSaver.buildFilesToCrawlInputList(filesToCrawlMap)
        if (sidList) {
            UpdateResponse response = solrSaver.saveFilesCollection(sidList)
            log.debug "Update response: $response"
        } else {
            log.info "\t\tSkipping empty crawl list: $sidList (folder: $crawlableFolder)"
        }
    }

    log.debug "\t\t next folder..."

}
Date end = new Date()
use(groovy.time.TimeCategory) {
    def duration = end - start
    log.info "Crawl duration: minutes: ${duration.minutes} -- seconds: ${duration.seconds} -- Hours: ${duration.hours} -- Days: ${duration.days}, "
}

def commitResponse = solrSaver.commitUpdates()
log.info "Commited: $commitResponse"

log.info "done!?"
