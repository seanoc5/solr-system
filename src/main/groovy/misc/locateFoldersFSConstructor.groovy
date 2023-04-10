package misc

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
Map startFolders = config.dataSources.localFolders

Long fileCount = 0
startFolders.each { String crawlName, String sourceFolder ->
    File startFolder = new File(sourceFolder)
    log.info "Crawling parent folder: $startFolder"

    File f = new File(sourceFolder)
    FolderFS startFolderFS = new FolderFS(f, 1, ignoreFiles, ignoreFolders, true)

    // ------------------ Solr Stuff -------------------
    String solrUrl = options.solrUrl
    if (solrUrl && solrUrl != 'false') {
        log.info "Found solrUrl ($solrUrl) in command line, using that (and ignoring anything from the config file...)"
    } else {
        solrUrl = config.solrUrl
        log.info "Found solrUrl ($solrUrl) in config file (nothing from command line)"
    }
    SolrSaver solrSaver = new SolrSaver(solrUrl, crawlName)

    boolean wipeContent = options.wipeContent
    String q = "${SolrSaver.FLD_DATA_SOURCE}:\"${crawlName}\""
    long existingDocCount = solrSaver.getDocumentCount(q)
    log.info "Starting crawl named:($crawlName) with sourceFolder:($sourceFolder) -- ($existingDocCount) existing docs"
    if (wipeContent) {
        log.warn "Wiping previous crawl data (BEWARE!!!), delete query: ($q) -- ($existingDocCount) existing docs"
        def foo = solrSaver.deleteDocuments(q)
        log.info "Clear results: $foo"
    }


    def allFolders = startFolderFS.getAllSubFolders() + startFolderFS
    Map groupedFolders = allFolders.groupBy { it.name }

    def sortedbyDuplicates = groupedFolders.sort { it.value.size() * -1 }
    def sortedbyType = allFolders.groupBy { it.assignedTypes[0] }.sort {
        it.value.size() * -1
    }
    log.info "Sorted by duplicates: ${sortedbyDuplicates.collect { String key, List<FolderFS> fsFolders -> "\n\t\t$key: ${fsFolders.size()}" }}"
    log.info "Sorted by type: ${sortedbyType.collect { String key, List<FolderFS> fsFolders -> "\n\t\t$key: ${fsFolders.size()}" }}"
    log.debug "done with folder: $sourceFolder"

    allFolders.each { FolderFS fsFolder ->
        List<SolrInputDocument> sidList = fsFolder.toSolrInputDocumentList()
        def result = solrSaver.saveDocs(sidList)
        log.info "\t\tSaved folder (${fsFolder}) with ${fsFolder.countTotal} items"
    }

}

long end = System.currentTimeMillis()
long elapsed = end - start
log.info "Elapsed time: ${elapsed} ms   (${elapsed / 1000} sec)"


log.info "done!?"
