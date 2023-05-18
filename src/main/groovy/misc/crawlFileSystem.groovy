package misc

import com.oconeco.analysis.FileAnalyzer
import com.oconeco.analysis.FolderAnalyzer
import com.oconeco.crawler.LocalFileSystemCrawlerSimple
import com.oconeco.helpers.SolrCrawlArgParser
import com.oconeco.persistence.SolrSaver
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

String source = config.sourceName ?: 'undefined'
Map folderNamePatterns = config.namePatterns.folders
Pattern ignorePattern = config.namePatterns.folders.ignore

SolrSaver solrSaver = new SolrSaver(solrUrl, source)
log.info "\t\tSolr Saver created: $solrSaver"

FolderAnalyzer folderAnalyzer = new FolderAnalyzer(config)
FileAnalyzer fileAnalyzer = new FileAnalyzer(config)

def linkOption = LinkOption.NOFOLLOW_LINKS

long start = System.currentTimeMillis()

crawlMap.each { String label, String startPath ->
    log.info "crawl map item with label:'$label' --- and --- startpath:'$startPath'..."
    Map<String, List<Map<String,Object>>> crawlFolders = LocalFileSystemCrawlerSimple.processStartFolder(label, startPath, folderNamePatterns)
    crawlFolders.each {String type, List<Map<String, Object>> foldersMap->
        log.info "\t\tCrawl type [$type] --- folder count: ${foldersMap.size()}"
    }
//    log.info "\t\tcrawlFolders:'$crawlFolders'..."
}

solrSaver.closeClient()

long end = System.currentTimeMillis()
long elapsed = end - start
log.info "${this.class.name} Elapsed time: ${elapsed}ms (${elapsed/1000} sec)"

//crawlMap.each { String label, String startPath ->
//    Path path = Paths.get(startPath)
//    Map crawlFolders = null
//    if(Files.exists(path, linkOption )) {
//        crawlFolders = LocalFileSystemCrawlerSimple.processStartFolder(path, ignorePattern)
//    } else {
//        log.warn "File '$startPath' either does not exist, or is a symbolic link"
//    }
//    return crawlFolders
//}