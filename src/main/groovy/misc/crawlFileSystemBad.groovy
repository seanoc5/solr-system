package misc

import com.oconeco.analysis.FileAnalyzer
import com.oconeco.analysis.FolderAnalyzer
import com.oconeco.crawler.LocalFileSystemCrawler
import com.oconeco.helpers.SolrCrawlArgParser
import com.oconeco.models.FSFolder
import com.oconeco.persistence.SolrSystemClient
import org.apache.log4j.Logger

import java.nio.file.LinkOption
import java.util.regex.Pattern

Logger log = Logger.getLogger(this.class.name);
log.info "Start ${this.class.name}, with args: $args"

ConfigObject config = SolrCrawlArgParser.parse(this.class.simpleName, args)

Map<String,String> crawlMap = config.dataSources.localFolders
log.info "\t\tCrawl Map (start folders): $crawlMap"

String solrUrl = config.solrUrl
log.info "\t\tSolr url: $solrUrl"

String locationName = config.sourceName ?: 'undefined'
String crawlName = config.ccrawlName ?: 'undefined'

//Map folderNamePatterns = config.namePatterns.folders
Pattern fileIgnorePattern = config.namePatterns.folders.ignore

//SolrSystemClient solrSaver = new SolrSystemClient(solrUrl, locationName)
SolrSystemClient solrSaver = new SolrSystemClient(solrUrl)
log.debug "\t\tSolr Saver created: $solrSaver"

FolderAnalyzer folderAnalyzer = new FolderAnalyzer(config)
FileAnalyzer fileAnalyzer = new FileAnalyzer(config)

def linkOption = LinkOption.NOFOLLOW_LINKS
LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, crawlName)
long start = System.currentTimeMillis()

crawlMap.each { String label, String startPath ->
    log.info "crawl map item with label:'$label' --- and --- startpath:'$startPath'..."
     def crawlFolders = crawler.buildCrawlFolders(startPath, fileIgnorePattern)
    crawlFolders.each { FSFolder fsfolder->
        log.info "\t\tCrawl folder: $fsfolder"
    }
}

solrSaver.closeClient()

long end = System.currentTimeMillis()
long elapsed = end - start
log.info "${this.class.name} Elapsed time: ${elapsed}ms (${elapsed/1000} sec)"

//crawlMap.each { String label, String startPath ->
//    Path path = Paths.get(startPath)
//    Map crawlFolders = null
//    if(Files.exists(path, linkOption )) {
//        crawlFolders = LocalFileSystemCrawlerSimple.processStartFolder(path, fileIgnorePattern)
//    } else {
//        log.warn "File '$startPath' either does not exist, or is a symbolic link"
//    }
//    return crawlFolders
//}
