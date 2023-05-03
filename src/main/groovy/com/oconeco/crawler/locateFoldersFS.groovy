package com.oconeco.crawler

import com.oconeco.analysis.FolderAnalyzer
import com.oconeco.helpers.SolrCrawlArgParser
import com.oconeco.models.FolderFS
import com.oconeco.persistence.SolrSaver
import org.apache.log4j.Logger
import org.apache.solr.common.SolrInputDocument

import java.util.regex.Pattern

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
String crawlName = options.name
String solrUrl = options.solrUrl


File cfgFile = new File(options.config)
if (!cfgFile.exists()) {
    throw new IllegalArgumentException("Config file: $cfgFile does not exist, bailing...")
} else {
    log.info "cfg file: ${cfgFile.absolutePath}"
}
ConfigObject config = new ConfigSlurper().parse(cfgFile.toURI().toURL())

// todo -- learn how to get/use multi-valued args like a list of start folders -- just getting the first one so far...
def startFolders = options.fs
log.info "Crawl ($crawlName) -- Start folders: $startFolders"

long start = System.currentTimeMillis()

// ------------------ Solr Stuff -------------------
SolrSaver solrSaver = new SolrSaver(solrUrl, crawlName)

boolean wipeContent = options.wipeContent
if (wipeContent) {
    log.warn "Wiping previous crawl data (BEWARE!!!)..."
    def foo = solrSaver.clearCollection()
    log.info "Clear results: $foo"
}


// ------------------ Folder stuff -------------------
FolderAnalyzer analyzer = new FolderAnalyzer(config)
Pattern ignoreFolders = config.folders.namePatterns.ignore ?: analyzer.DEFAULT_FOLDERNAME_PATTERNS.ignore
Pattern ignoreFiles = config.files.namePatterns.ignore ?: analyzer.DEFAULT_FOLDERNAME_PATTERNS.ignore
//Pattern ignoreFolders = analyzer.folderNamePatterns.ignore ?: analyzer.DEFAULT_FOLDERNAME_PATTERNS.ignore
//Pattern ignoreFiles = analyzer.fileNamePatterns.ignore      //analyzer.DEFAULT_FOLDERNAME_PATTERNS.ignore

Long fileCount = 0
startFolders.each { String sf ->
    File startFolder = new File(sf)
    log.info "Crawling parent folder: $startFolder"

    // get list of non-noisy folders to crawl (or compare against stored info (check if we need to crawl...
    List<FolderFS> fsFoldersToCrawl = LocalFileCrawler.gatherFolderFSToCrawl(startFolder, ignoreFiles, ignoreFolders)
//    List<File> foldersToCrawl = LocalFileCrawler.getFoldersToCrawl(startFolder, analyzer.DEFAULT_FOLDERNAME_PATTERNS)
    Map groupedFolders = fsFoldersToCrawl.groupBy { it.name }
    groupedFolders.each { String name, List<FolderFS> fsFolderList ->
        fsFolderList.each { FolderFS ffs ->
            ffs.sameNameCount = fsFolderList.size()
            def foo = ffs.analyze(analyzer)
        }
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
