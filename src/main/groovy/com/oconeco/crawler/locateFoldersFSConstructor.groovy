package com.oconeco.crawler

import com.oconeco.analysis.FolderAnalyzer
import com.oconeco.helpers.SolrCrawlArgParser
import com.oconeco.models.FolderFS
import com.oconeco.persistence.SolrSaver
import org.apache.log4j.Logger
import org.apache.solr.common.SolrInputDocument

import java.util.regex.Pattern

/**
 * Testing constructor based (recursive) crawl -- load the parent folder, and it will self-crawl subdirs and files
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

long start = System.currentTimeMillis()


// ------------------ Folder stuff -------------------
FolderAnalyzer analyzer = new FolderAnalyzer(config)
Pattern ignoreFolders = config.folders.namePatterns.ignore ?: analyzer.DEFAULT_FOLDERNAME_PATTERNS.ignore
log.info "Ignore FOLDERS: $ignoreFolders"
Pattern ignoreFiles = config.files.namePatterns.ignore ?: analyzer.DEFAULT_FOLDERNAME_PATTERNS.ignore
log.info "Ignore files: $ignoreFiles"
Map startFolders = dataSources.localFiles

Long fileCount = 0
startFolders.each { String crawlName, String sf ->
    File startFolder = new File(sf)
    log.info "Crawling parent folder: $startFolder"

    File f = new File(sf)
    FolderFS folderFS = new FolderFS(f, 1, ignoreFiles, ignoreFolders)

    // ------------------ Solr Stuff -------------------
    SolrSaver solrSaver = new SolrSaver(solrUrl, crawlName)

    boolean wipeContent = options.wipeContent
    if (wipeContent) {
        log.warn "Wiping previous crawl data (BEWARE!!!)..."
        def foo = solrSaver.clearCollection()
        log.info "Clear results: $foo"
    }


    def sortedbyName = groupedFolders.sort { it.value.size() * -1 }
    def sortedbyType = fsFoldersToCrawl.groupBy { it.assignedTypes[0] }.sort {
        it.value.size() * -1
    }
    log.info "Sorted by name: ${sortedbyName.collect { String key, List<FolderFS> fsFolders -> "\n\t\t$key: ${fsFolders.size()}" }}"
    log.info "Sorted by type: ${sortedbyType.collect { String key, List<FolderFS> fsFolders -> "\n\t\t$key: ${fsFolders.size()}" }}"
    log.debug "done with folder: $sf"

    fsFoldersToCrawl.each { FolderFS fsFolder ->
        List<SolrInputDocument> sidList = fsFolder.toSolrInputDocumentList()
        def result = solrSaver.saveDocs(sidList)
        log.info "Saved folder (${fsFolder}) with ${fsFolder.countTotal} items"
    }

}

long end = System.currentTimeMillis()
long elapsed = end - start
log.info "Elapsed time: ${elapsed} ms   (${elapsed / 1000} sec)"


log.info "done!?"
