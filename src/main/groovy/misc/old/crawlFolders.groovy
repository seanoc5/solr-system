package misc.old

import com.oconeco.crawler.LocalFileSystemCrawler
import com.oconeco.helpers.Constants
import org.apache.log4j.Logger

/**
 * test script to only crawl folders (faster? first pass? exploratory....?)
 */
Logger log = Logger.getLogger(this.class.name);
log.info "Start here (crawl folders..."

List<String> startFolders = ['/home/sean/Documents']

startFolders.each { String sf ->
    File startFolder = new File(sf)
    log.info "Crawling parent folder: $startFolder"

    // get list of non-noisy folders to crawl (or compare against stored info (check if we need to crawl...
    def  resultsMap = LocalFileSystemCrawler.buildCrawlFolders(startFolder)
    log.info "Folders size: ${resultsMap.size()}"

    // see if there are common folders -- dev-time informational/insight only
    def crawlFolders = resultsMap[Constants.LBL_CRAWL]
    def ignoreFolders = resultsMap[Constants.LBL_IGNORE]
    def dups = ignoreFolders.findAll {String name, List values ->
        values.size() > 1
    }.sort { it.value.size() * -1}

    log.info "Dup folder names: ${dups.size()}"


/*

    foldersToCrawl.each { File crawlableFolder ->
        Map<File, Map> filesToCrawlMap = LocalFileCrawler.getFilesToCrawlList(crawlableFolder)
//        def extensionsGrouped = filesToCrawlMap.extensions?.countBy{it}
//        def extensionsGrouped = filesToCrawlMap.extensions?.groupBy { it }.sort {}
//        log.info "Extensions: $extensionsGrouped"
//        log.info "Files to crawl: \n\t${filesToCrawlMap.entrySet().join('\n\t')}"
        List<SolrInputDocument> sidList = solrSaver.buildFilesToCrawlInputList(filesToCrawlMap)
        if (sidList) {
            UpdateResponse response = solrSaver.saveFilesCollection(sidList)
            log.info "Update response: $response"
        } else {
            log.info "Skiipping empty crawl list: $sidList (folder: $crawlableFolder)"
        }
    }
*/

    log.debug "\t\t next folder..."

}

log.info "done!?"
