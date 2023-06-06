package misc

import com.oconeco.crawler.LocalFileSystemCrawler
import com.oconeco.helpers.SolrCrawlArgParser
import com.oconeco.models.FSFolder
import com.oconeco.models.SavableObject
import com.oconeco.persistence.SolrSystemClient
import org.apache.log4j.Logger
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrDocument

import java.util.regex.Pattern

Logger log = Logger.getLogger(this.class.name);
log.info "Start ${this.class.name}, with args: $args"

ConfigObject config = SolrCrawlArgParser.parse(this.class.simpleName, args)

Map<String, String> crawlMap = config.dataSources.localFolders
log.info "\t\tCrawl Map (config.dataSources.localFolders): $crawlMap"

String solrUrl = config.solrUrl
log.debug "\t\tSolr url: $solrUrl"

String locationName = config.sourceName ?: 'undefined'

Pattern fileIgnorePattern = config.namePatterns.files.ignore
Pattern folderIgnorePattern = config.namePatterns.folders.ignore

SolrSystemClient client = new SolrSystemClient(solrUrl)
log.info "\t\tSolr Saver created: $client"
QueryResponse resp = client.query('*:*')

long numFound = resp.results.getNumFound()
log.info "Before crawl NumFound: $numFound"

//FolderAnalyzer folderAnalyzer = new FolderAnalyzer(config)
//FileAnalyzer fileAnalyzer = new FileAnalyzer(config)

long start = System.currentTimeMillis()

crawlMap.each { String crawlName, String startPath ->
    LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, crawlName)
    Map<String, SolrDocument> existingSolrFolderDocs
    if(config.wipeContent==true) {
        log.warn "Config/args indicates we whould wipe content for crawl: $crawler"
    } else {
        existingSolrFolderDocs = client.getSolrFolderDocs(crawler)
    }
    log.info "\t\tcrawl map item with label:'$crawlName' --- and --- startpath:'$startPath'..."
    def crawlFolders = crawler.buildCrawlFolders(startPath, folderIgnorePattern)
    crawlFolders.each { FSFolder fsfolder ->
        def details = fsfolder.addFolderDetails()
        log.debug "\t\t$fsfolder: added folder details: $details"

        List<SavableObject> children = fsfolder.buildChildrenList()
        // todo -- add analysis call here, to update folder with (basic) info of children
        def sidList = fsfolder.toSolrInputDocumentList()
        UpdateResponse response
        try {
            response = client.saveDocs(sidList)
            log.info "\t\tCrawled folder: $fsfolder with ${fsfolder.children.size()} children -- Solr update response (status:${response.getStatus()} -- ${response.getElapsedTime()}ms): $response)"
        } catch (BaseHttpSolrClient.RemoteSolrException rse){
            log.error "Error with solrSaver.saveDocs: $rse"
        }
    }
}

client.commitUpdates(true, true)
long end = System.currentTimeMillis()
long elapsed = end - start
log.info "${this.class.name} Elapsed time: ${elapsed}ms (${elapsed / 1000} sec)"


long numFound2 = resp.results.getNumFound()
log.info "After crawl NumFound: $numFound2"

client.closeClient(this.class.name)
