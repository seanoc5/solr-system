package misc

import com.oconeco.analysis.BaseAnalyzer
import com.oconeco.analysis.FileSystemAnalyzer
import com.oconeco.crawler.LocalFileSystemCrawler
import com.oconeco.difference.SolrDifferenceChecker
import com.oconeco.helpers.SolrCrawlArgParser
import com.oconeco.models.FSFolder

import com.oconeco.persistence.SolrSystemClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger

Logger log = LogManager.getLogger(this.class.name);
log.info "Start ${this.class.name}, with args: $args"



// call the arg parser to read the config and return a merged config
ConfigObject config = SolrCrawlArgParser.parse(this.class.simpleName, args)

Map<String, String> crawlMap = config.dataSources.localFolders
log.info "\t\tCrawl Map (config.dataSources.localFolders): $crawlMap"
String solrUrl = config.solrUrl
String locationName = config.locationName

SolrSystemClient solrClient = new SolrSystemClient(solrUrl)
log.info "\t\tSolr Saver created: $solrClient"

long numFoundPreLocation = solrClient.getDocumentCount()

//boolean compareExistingSolrFolderDocs = config.compareExistingSolrFolderDocs
def folderNameMap = config.namePatterns.folders
def fileNameMap = config.namePatterns.files
def foldPathMap = config.pathPatterns.folders
SolrDifferenceChecker differenceChecker = new SolrDifferenceChecker(config.checkGroupSizes)
BaseAnalyzer analyzer = new FileSystemAnalyzer(folderNameMap, fileNameMap, foldPathMap, null,)
long start = System.currentTimeMillis()

crawlMap.each { String crawlName, String startPath ->
    log.info "Crawl name: $crawlName -- start path: $startPath -- location: $locationName "

    LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, solrClient, differenceChecker)
    def numFoundPreCrawl = crawler.getSolrDocCount(crawlName)
    log.debug "\t\t====Solr Doc Count before crawl(loc=$locationName: cs=$crawlName): $numFoundPreCrawl"

    // !!!!!!!!! CLEAR specific crawl if arg/config indicates !!!!!!!!!!!
    if (config.wipeContent == true) {
        log.warn "\t\tConfig/args indicates we whould WIPE CONTENT for crawl:($crawlName) and crawler:($crawler)"
        Map<String, Object> deleteResults = solrClient.deleteCrawledDocuments(locationName, crawlName)
        log.info "\t\tDelete results map: $deleteResults"
    }

    File startDir = new File(startPath)
    def existingFolderSolrDocs = crawler.getSavedGroupDocs(crawlName, startPath)
    log.info "\t\tcrawl map item with label:'$crawlName' - startpath:'$startPath' -- numFound preCrawl: $numFoundPreCrawl -- existingSolrFolderDocs: ${existingFolderSolrDocs.size()}"
    Map<String, List<FSFolder>> crawlFolders = crawler.crawlFolders(crawlName, startDir, existingFolderSolrDocs, analyzer)
    if (crawlFolders.updated) {
        log.info "\t\tCrawled Folders, updated:${crawlFolders?.updated?.size()} -- skipped: ${crawlFolders?.skipped?.size()}"
        LinkedHashMap<String, Long> solrResults = crawler.getSolrDocCount(crawlName)
        log.info "\t\t====crawl($crawlName): Solr Doc Count preCrawl:($numFoundPreCrawl) -> after:($solrResults)"
    } else {
        log.info "\t\t----no folders updated $locationName:$crawlName, skipped count:${crawlFolders?.skipped?.size()} --  Solr Doc Count preCrawl ($numFoundPreCrawl) assume the same after no updates..."
    }
}


long end = System.currentTimeMillis()
long elapsed = end - start
log.info "${this.class.simpleName} Elapsed time: ${elapsed}ms (${elapsed / 1000} sec) (${elapsed / (1000 * 60)} min)"

log.info "Force commit updates to get proper count a"
solrClient.commitUpdates(true, true)

long numFoundPostLocation = solrClient.getDocumentCount()
log.info "Before crawl ($numFoundPreLocation) -- After crawl NumFound ($numFoundPostLocation) -- difference:${numFoundPostLocation - numFoundPreLocation}"

solrClient.closeClient(this.class.name)
