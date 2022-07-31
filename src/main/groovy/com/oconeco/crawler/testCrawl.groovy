package com.oconeco.crawler

import com.oconeco.persistence.SolrSaver
import groovy.cli.picocli.CliBuilder
import groovy.cli.picocli.OptionAccessor
import org.apache.log4j.Logger
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument

Logger log = Logger.getLogger(this.class.name);
log.info "Start ${this.class.name}..."

String toolName = this.class.name

CliBuilder cli = new CliBuilder(usage: "${toolName}.groovy -fhttp://myFusion5addr:6764 -uadmin -psecret123 -s~/data/MyApp.objects.json -m ~/Fusion/migration/F4/mappingFolder", width: 160)
cli.with {
    h longOpt: 'help', 'Show usage information'
    s longOpt: 'solrUrl', args: 1, required: true, 'Solr url with protocol, host, and port (if any)'
    f longOpt: 'folders', args:'+'; 'Parent folder(s) to crawl'
}

OptionAccessor options = cli.parse(args)
if (!options) {
    cli.usage()
    System.exit(-1)
}
if (options.help) {
    cli.usage()
    System.exit(0)
}

//String solrUrl = "http://localhost:8983/solr/lucy"

List<String> startFolders = [
//        '/home/sean/work/OconEco/data/munis/2012_Individual_Unit_file',
//        '/home/sean/work/OconEco/data/munis'
//        '/home/sean/work/OconEco/OneDrive - OconEco'
        '/home/sean/work/'
]
//List<String> startFolders = ['/home/sean/']

// NOTE - don't do this, it will traverse ALL subfolders, including noisy ones
//def foo = LocalFileCrawler.getFoldersToCrawlRecurse(startFolders[0])

Date start = new Date()
SolrSaver solrSaver = new SolrSaver(solrUrl)
boolean clear = false
if (clear){
    log.warn "Clearing collection (clear==true)..."
    def foo = solrSaver.clearCollection()
    log.info "Clear results: $foo"
}
Long fileCount = 0
startFolders.each { String sf ->
    File startFolder = new File(sf)
    log.info "Crawling parent folder: $startFolder"

    // get list of non-noisy folders to crawl (or compare against stored info (check if we need to crawl...
    Set<File> foldersToCrawl = LocalFileCrawler.getFoldersToCrawl(startFolder)
    // see if there are common folders -- dev-time informational/insight only
    //def foldersGroups = foldersToCrawl.groupBy { it.name }

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
