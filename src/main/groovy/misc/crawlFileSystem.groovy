package misc

import com.oconeco.crawler.DifferenceChecker
import com.oconeco.crawler.LocalFileSystemCrawler
import com.oconeco.helpers.SolrCrawlArgParser
import com.oconeco.models.FSFolder
import com.oconeco.models.SavableObject
import com.oconeco.persistence.SolrSystemClient
import org.apache.log4j.Logger
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrInputDocument

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
log.info "config.ignoreArchivesPattern == " + config.ignoreArchivesPattern

SolrSystemClient solrClient = new SolrSystemClient(solrUrl)
log.info "\t\tSolr Saver created: $solrClient"
QueryResponse resp = solrClient.query('*:*')

long numFound = resp.results.getNumFound()
log.info "Before crawl NumFound: $numFound"

//FolderAnalyzer folderAnalyzer = new FolderAnalyzer(config)
//FileAnalyzer fileAnalyzer = new FileAnalyzer(config)

long start = System.currentTimeMillis()

crawlMap.each { String crawlName, String startPath ->
    log.info "Crawl name: $crawlName -- start path: $startPath -- location: $locationName "
    DifferenceChecker differenceChecker = new DifferenceChecker()
    LocalFileSystemCrawler crawler = new LocalFileSystemCrawler(locationName, crawlName, 3, differenceChecker)
    Map<String, SolrDocument> existingSolrFolderDocs
    if(config.wipeContent==true) {
        log.warn "\t\tConfig/args indicates we whould wipe content for crawl: $crawler"
        Map<String, Object> deleteResults = solrClient.deleteCrawledDocuments(crawler)
        log.info "\t\tDelete results map: $deleteResults"
    } else {
        existingSolrFolderDocs = solrClient.getSolrFolderDocs(crawler)
        if(existingSolrFolderDocs) {
            log.debug "\t\texisting Solr Folder docs (to check incremental) size: ${existingSolrFolderDocs.size()}"
        } else {
            log.warn "No existing solr folder docs found for $crawlName : $startPath -- first crawl??"
        }
    }
    log.debug "\t\tcrawl map item with label:'$crawlName' --- and --- startpath:'$startPath'..."
    def crawlFolders = crawler.buildCrawlFolders(crawlName, startPath, folderIgnorePattern, existingSolrFolderDocs)
    crawlFolders.each { FSFolder fsfolder ->
        def details = fsfolder.addFolderDetails()
        log.debug "\t\t$fsfolder: added folder details: $details"

        List<SavableObject> children = fsfolder.buildChildrenList(fileIgnorePattern)
        // todo -- add analysis call here, to update folder with (basic) info of children
        def sidList = fsfolder.toSolrInputDocumentList()
        UpdateResponse response
        try {
            response = solrClient.saveDocs(sidList)
            log.info "\t\tCrawled folder: $fsfolder with ${fsfolder.children.size()} children -- Solr update response (status:${response.getStatus()} -- ${response.getElapsedTime()}ms): $response)"
        } catch (BaseHttpSolrClient.RemoteSolrException rse){
            log.error "Error with solrSaver.saveDocs: $rse"
        }

        def archiveFSFiles = fsfolder.gatherArchiveFiles(config.ignoreArchivesPattern)
        archiveFSFiles.each {
            List<SavableObject> archObjects = it.gatherArchiveEntries()
            if(archObjects) {
                List<SolrInputDocument> solrObjects = archObjects.collect { it.toSolrInputDocument() }
                log.info "\t\t++++::: Process arch file($it) with (${archObjects.size()}) archive child objects"
                try {
                    response = solrClient.saveDocs(solrObjects)
                    log.info "\t\tCrawled Archive objects from arch-file($it) with ${archObjects.size()} children -- Solr update response (status:${response.getStatus()} -- ${response.getElapsedTime()}ms): $response)"
                } catch (RemoteSolrException rse) {
                    log.error "Error with solrSaver.saveDocs (Archive entries): $rse"
                }
            } else {
                log.info "No valid archive objects found for source file: $it"
            }
        }
    }
}

solrClient.commitUpdates(true, true)
long end = System.currentTimeMillis()
long elapsed = end - start
log.info "${this.class.name} Elapsed time: ${elapsed}ms (${elapsed / 1000} sec)"


long numFound2 = resp.results.getNumFound()
log.info "Before crawl ($numFound) -- After crawl NumFound: $numFound2 -- difference:${numFound2-numFound}"

solrClient.closeClient(this.class.name)
