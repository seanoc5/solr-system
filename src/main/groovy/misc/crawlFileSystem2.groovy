package misc

import com.oconeco.crawler.LocalFileSystemCrawler
import com.oconeco.helpers.SolrCrawlArgParser
import com.oconeco.models.FSFolder
import com.oconeco.persistence.SolrSystemClient
import org.apache.log4j.Logger

import java.util.regex.Pattern

Logger log = Logger.getLogger(this.class.name);
log.info "Start ${this.class.name}, with args: $args"

// call the arg parser to read the config and return a merged config
ConfigObject config = SolrCrawlArgParser.parse(this.class.simpleName, args)

Map<String, String> crawlMap = config.dataSources.localFolders
log.info "\t\tCrawl Map (config.dataSources.localFolders): $crawlMap"
String solrUrl = config.solrUrl
String locationName = config.sourceName
Pattern fileIgnorePattern = config.namePatterns.files.ignore
Pattern folderIgnorePattern = config.namePatterns.folders.ignore
log.info "config.ignoreArchivesPattern == " + config.ignoreArchivesPattern + " -- special pattern to block out things like docx which is really a conceptual doc, but zip of files"

SolrSystemClient solrClient = new SolrSystemClient(solrUrl)
log.info "\t\tSolr Saver created: $solrClient"

//FolderAnalyzer folderAnalyzer = new FolderAnalyzer(config)
//FileAnalyzer fileAnalyzer = new FileAnalyzer(config)

long start = System.currentTimeMillis()

crawlMap.each { String crawlName, String startPath ->
    log.info "Crawl name: $crawlName -- start path: $startPath -- location: $locationName "
//    DifferenceChecker differenceChecker = new DifferenceChecker()
    LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, crawlName, solrClient)
    long numFound = crawler.getSolrDocCount(crawlName)
    log.debug "\t\tSolr Doc Count before crawl($crawlName): $numFound"


    if(config.wipeContent==true) {
        log.warn "\t\tConfig/args indicates we whould wipe content for crawl: $crawler"
        Map<String, Object> deleteResults = solrClient.deleteCrawledDocuments(crawler)
        log.info "\t\tDelete results map: $deleteResults"
    }

    log.debug "\t\tcrawl map item with label:'$crawlName' --- and --- startpath:'$startPath'..."
    File startFolder = new File(startPath)
    List<FSFolder> crawlFolders = crawler.crawlFolders(crawlName, startFolder, folderIgnorePattern, fileIgnorePattern)
    log.info "\t\tCrawled Folders: ${crawlFolders.size()}"
}

solrClient.commitUpdates(true, true)
long end = System.currentTimeMillis()
long elapsed = end - start
log.info "${this.class.name} Elapsed time: ${elapsed}ms (${elapsed / 1000} sec)"


long numFound2 = resp.results.getNumFound()
log.info "Before crawl ($numFound) -- After crawl NumFound: $numFound2 -- difference:${numFound2-numFound}"

solrClient.closeClient(this.class.name)
