package com.oconeco.crawler

import com.oconeco.helpers.Constants
import groovy.io.FileType
import groovy.io.FileVisitResult
import org.apache.log4j.Logger

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileOwnerAttributeView
import java.nio.file.attribute.UserPrincipal
import java.util.regex.Matcher
import java.util.regex.Pattern

//import com.oconeco.persistence.JSONSerialize
//import com.oconeco.processing.ContentProcessorTextOpenNLP
//import com.oconeco.extraction.TikaHelper

/**
 * @deprecated early test for processing files, see @locateFoldersFSConstructor
 */

/**
 * Crawl file system but check stored info and only update folders/files which are newer than stored info
 * todo -- finish comparison code
 * Created by sean on 11/4/15.
 * @deprecated -- move over to LocalFileSystemCrawler
 */
class LocalFileSystemCrawlerSimple {
    static Pattern FILE_REGEX_TO_SKIP_DEFAULT = Pattern.compile('(\\~\\$.*|_.*|.*\\.te?mp$|.*\\.class$|robots.txt|.*\\.odb$|.*json|.*\\.pos)')
    // ignore temp and class files as well as typical output/data files (json, pos)
    static Pattern FILE_EXTENSIONS_TO_EXTRACT_DEFAULT = Pattern.compile("(aspx\\?|cfm|csv|docx\\?|groovy|html|htm|zhtml|ics|java|od.|pptx\\?|pdf|php|ps|pub|rdf|rss|xlsx\\?)")
    // skip txt files for extraction, but do process/analyze them
    static Pattern FOLDER_REGEX_TO_SKIP_DEFAULT = Pattern.compile('^(\\.git|\\.svn|sub|dev|cpu[0-9]*|\\.trash|te?mp|trash|[cC]ache|~.*|#.*)$')

    static Logger log = Logger.getLogger(this.class.name);
    protected boolean forceUpdate = false


    public static void main(String[] args) {
        File startFolder = new File('/home/sean/work/lucidworks/')
/*
        def input = "2011-01-01"
        def pattern = "yyyy-MM-dd"
        Date lastCrawlTime = new SimpleDateFormat(pattern).parse(input)
        Long lastCrawlTime2 = Date.parse('yyyy-MM-dd', '2015-01-03').time
*/

        boolean forceUpdate = false
        LocalFileSystemCrawlerSimple crawler = new LocalFileSystemCrawlerSimple(forceUpdate)

//        TikaHelper extractor = new TikaHelper()
//        Map knownFolders = crawler.getKnownFolderMap(startFolder, lastCrawlTime)

        Map knownFolders = [:]
        // empty list -- todo -- populate form solr or other data-store with previous crawl info
        List<String> analysisExtensions = []       // empty list -- no analysis (nlp or similar), just extraction

        List<File> dirtyFolders = crawler.getFoldersToCrawl(startFolder, knownFolders)
        dirtyFolders.each { File folder ->
            List<File> files = crawler.getFilesToCrawlInFolder(folder, FILE_REGEX_TO_SKIP_DEFAULT, analysisExtensions)
            files.each { File f ->
                crawler.log.info "Process file: $f -- ${f.class.name}"
            }
        }
        crawler.log.info("Done! Folders to crawl: ${dirtyFolders.size()}")

    }

    LocalFileSystemCrawlerSimple(boolean forceUpdate) {
        this.forceUpdate = forceUpdate
        log.error "Constructor: forceUpdate:$forceUpdate"
    }

    /**
     * Crawl the start path, and ignore certain path patterns
     * likely there will be a second pass to evealuate
     * @param startPath
     * @param ignorePattern
     * @return
     */
    static Map processStartFolder(def label, def startPath, Map folderPatternsMap) {
        log.info "\t\tProcess start folder label:'$label' -- startpath:$startPath, with namePatternsMap keys: ${folderPatternsMap.keySet()}}"
        Path nioPath = null
        if (startPath instanceof Path) {
            log.info "\t\tgiven a nio path: $nioPath -- using that!"
            nioPath = startPath
        } else {
            log.debug "\t\tconverting path ($startPath) from def type (${startPath.class.crawlName} to Path (nio)"
            nioPath = Paths.get(startPath)
        }

        def ignore = folderPatternsMap[Constants.LBL_IGNORE]
        Pattern ignorePattern = null
        if (ignore instanceof Pattern) {
            log.debug "\t\tFound 'ignore' pattern from map: \"$ignore\" -- use it as is"
            ignorePattern = ignore
        } else {
            log.info "\t\tignore value in map was not a pattern, try to cast it ($ignore) now..."
            ignorePattern = new Pattern(ignore)
        }

        Map crawlFolders = crawlStartFolder(nioPath, ignorePattern)
        crawlFolders.each { String status, List things ->
            log.debug "\t\t$status) folder names count ${things.size()}"
        }

        return crawlFolders
    }


    /**
     * Crawl the start path, and ignore certain path patterns
     * likely there will be a second pass to evealuate
     * @param startPath
     * @param ignorePattern
     * @return
     */
    static Map crawlStartFolder(Path startPath, Pattern ignorePattern) {
        long start = System.currentTimeMillis()

        List/*<Path>*/ foldersToCrawl = []
        List/*<Path>*/ ignoredFolders = []
//        Pattern ignorePattern = namePatterns.get(FolderAnalyzer.LBL_IGNORE)
        Map options = [type     : FileType.DIRECTORIES,
                       preDir   : {
                           if (it ==~ ignorePattern) {
                               ignoredFolders << it
                               log.info "\t\t\tINGOREing folder: $it -- matches ignorePattern: $ignorePattern"
                               return FileVisitResult.SKIP_SUBTREE
//                           } else if (!Files.isExecutable(it)) {
//                               log.warn "No permissions to enter/execute dir: $it, skipping!"
//                               ignoredFolders << it
//                               return FileVisitResult.SKIP_SUBTREE
//                           } else if (!Files.isReadable(it)) {
//                               log.warn "Path not readable: $it, skipping!"
//                               ignoredFolders << it
//                               return FileVisitResult.SKIP_SUBTREE
                           } else {
                               foldersToCrawl << buildMetaMapFolder(it)
                               log.debug "\t\tADDING Crawl folder: $it"
                           }
                       },
/*
                       postDir  : {
                           println("Post dir: $it")
                           totalSize = 0
                       },
*/
                       preRoot  : true,
                       postRoot : true,
                       visitRoot: true,
        ]

        log.debug "\t\tStarting crawl of folder: ($startPath)  ..."
        startPath.traverse(options) { def folder ->
            log.debug "\t\ttraverse folder $folder"
        }

        long end = System.currentTimeMillis()
        long elapsed = end - start
        log.info "\t\tElapsed time: ${elapsed}ms (${elapsed / 1000} sec)"

        Map resuts = [foldersToCrawl: foldersToCrawl, ignoredFolders: ignoredFolders]
        return resuts
    }


    /**
     * basic and naive determination of file is text only (override this with better logic)
     * @param file
     * @return
     */
    static boolean isTextFile(File file) {
        if (file) {
            if (file.name.endsWith('.txt')) {
                return true
            } else if (file.name.toLowerCase().startsWith('readme')) {
                return true
            } else {
                return false
            }
        } else {
            log.warn "File ($file) is null"
            return false
        }
    }

    /**
     * Get map of folder path and date/time (if known) of last crawl, use this to determin if we should check files in folder, or trust files are up to date
     * @param baseFolder
     * @return
     */
    static Map<String, Long> getKnownFolderMap(File baseFolder, Long lastCrawlTime) {
        Map<String, Date> knownFolders = [:]
        log.warn "Override this testCrawl method (assuming all folders older than October 2015 have already been crawled/known..."
        baseFolder.eachFileRecurse(FileType.DIRECTORIES) { File folder ->
            if (folder.lastModified() <= lastCrawlTime) {
                log.debug "\t\tFolder last mod (${folder.lastModified()}) is older than testCrawl last crawl time: (${lastCrawlTime}) assume 'NOT KNOWN': $folder"
                knownFolders.put(folder.path, folder.lastModified())
            } else {
                log.debug "Folder last mod (${folder.lastModified()}) is newer than testCrawl last crawl time: (${lastCrawlTime}) assume 'KNOWN': $folder"
            }
        }
        return knownFolders
    }

    /**
     * Get a (hash)map of file paths and modify times 'known' to the system (i.e. saved/persisted) to check if we have to recrawl or can skip
     * @param folder
     * @param lastCrawlTime -- ignore this param in this implementation
     * @return
     */
/*
    Map<String, Long> getKnownFilesMap(File folder, Long lastCrawlTime){
        Map<String, Long> knownFiles = [:]
        folder.eachFile(FileType.FILES) {File f ->
            if(f.lastModified() > lastCrawlTime){
                log.debug "File is NEWER than last crawl time, assume we don't already know it: $f"
            } else {
                knownFiles.put(f.path, f.lastModified())
                log.debug "\t\tFile is older than last crawl, assume we know it: $f"
            }
        }
        return knownFiles
    }
*/

    /**
     * wrapper function to compare current folder branch (i.e. parent and descendants) to known/saved foldersin the system
     *
     * Assume date stamps are accurate and appropriate
     * @param baseFolder
     * @param knownFolders
     * @return
     */
    List<File> getFoldersToCrawl(File baseFolder, Map<String, Long> knownFolders) {
        List<File> foldersToCrawl = [baseFolder]
        // todo -- fixme -- quick hack to include base folder -- should refactor to make it easy to call same checks on parent as in loop below
        baseFolder.eachFileRecurse(FileType.DIRECTORIES) { File d ->
            if (shouldCrawlFolder(d, knownFolders)) {
                foldersToCrawl << d
                log.info "Folder($d) is NOT current, crawl it"
            } else {
                log.info "\t\t Skip folder: $d"
            }
        }
        log.info "Dirty folder count: ${foldersToCrawl.size()}"

        return foldersToCrawl
    }

    /**
     * wrapper function to compare current files in a folder to known/saved files in the system
     *
     * Assume date stamps are accurate and appropriate
     * @param folder
     * @param knownFiles
     * @return
     */
/*    List<File> getFilesToCrawlInFolder(File folder, Map<String, Long> knownFiles){
        List<File> filesToCrawl = []
        folder.eachFile(FileType.FILES) {File f ->
            if(shouldProcessFile(f)){
                if(isFileCurrent(f, knownFiles)){
                    log.debug "\t\tSkip file (already current in system): $f"
                } else {
                    log.error "\tFile: $f added to list for crawling"
                    filesToCrawl << f
                }

            } else {
                log.debug "\t\tSkip file: $f -- failed 'shouldProcessFile()'"
            }
        }
        return filesToCrawl
    }*/

    /**
     * wrapper function to compare current files in a folder to known/saved files in the system -- check based on presence of extracted '.txt' file with similar name
     *
     * Assume date stamps are accurate and appropriate
     * @param folder
     * @param knownFiles
     * @return
     */
    List<File> getFilesToCrawlInFolder(File folder, Pattern fileRegexToSkip, List<String> analysisExtensions) {
        List<File> filesToCrawl = []
        folder.eachFile(FileType.FILES) { File f ->
            Long lastProcessedTime = 0          // todo -- get last proc time
            if (shouldProcessFile(f, fileRegexToSkip, analysisExtensions)) {
                if (isFileCurrent(f, lastProcessedTime)) {
                    log.debug "\tskip file: $f -- (already current in system)"
                } else {
                    log.info "\t\tfile $f is not 'known' in the system and is something worth crawling, add it..."
                    filesToCrawl << f
                }

            } else {
                log.debug "\t\tSkip file: $f -- failed 'shouldProcessFile()'"
            }
        }
        return filesToCrawl
    }

    /**
     * check if this is a folder we want to crawl/index
     * based on regex patterns
     *
     * If it is of interest, is it current in the system?
     * @param folder
     * @return false if we should skip this folder and all descendants
     */
    boolean shouldCrawlFolder(File folder, Map<String, Long> knownFolders) {
        boolean should = true
        String name
        if (folder instanceof File) {
            File f = (File) folder
            if (f.isDirectory()) {
                name = f.name
                Matcher matcher = (name =~ FOLDER_REGEX_TO_SKIP_DEFAULT)
                if (matcher.matches()) {
                    log.info "\t\tskip folder ($name) based on regex group: ${matcher[0]} -- $f"
                    should = false
                } else {
                    if (forceUpdate) {
                        log.info "Force update set true, no time comparison: $d"
                        should = true
                    } else {
                        if (isFolderCurrent(folder, knownFolders)) {
                            log.info "\t\tFolder ($folder) did not match skip regex, but is current, no crawl"
                            should = false
                        } else {
                            log.info "Folder ($folder) did not match skip regex, and is NOT CURRENT, CRAWL"
                            should = true
                        }
                    }
                }
            } else {
                log.warn "Skip trying to crawl file ($f) where we expect a folder! (should never reach this code...)"
                should = false
            }
        } else {
            log.warn "Unknown folder type (i.e. not 'File'): $folder -- class: ${folder.getClass()}"
            should = false
        }
        return should
    }

    /**
     * Check if this folder is newer than what is known/saved in the system
     * @param folder
     * @param knownFolders
     * @return
     */
    boolean isFolderCurrent(File folder, Map<String, Long> knownFolders) {
        boolean current = false
        Long lastCrawlTime = knownFolders[folder.path]
        if (lastCrawlTime) {
            if (lastCrawlTime >= folder.lastModified()) {
                log.info "Folder is current in system, no need to re-crawl: $folder"
                current = true
            } else {
                log.info "Folder is out of date in system, NEED to re-crawl: $folder"
            }
        } else {
            log.info "Folder does not exist in system, NOT current: $folder"
        }
        return current
    }

    /**
     * * Check if this file is newer than what is known/saved in the system
     * @param file
     * @param knownFiles
     * @return
     */
    boolean isFileCurrent(File file, Map<String, Long> knownFiles) {
        Long lastIndexTime = knownFiles[file.path]
        if (file.lastModified() > lastIndexTime) {
            return false
        }
        return true
    }

    /**
     * * Check if this file is newer than what is known/saved in the system
     *  todo add logic to testCrawl, have moved from analysis focus to currency
     * @param file
     * @param knownFiles
     * @return
     *
     * @todo revisit where we check currency: shouldProcess or isFileCurrent... duplicating (cheap) efforts here)
     */
    boolean isFileCurrent(File file, Long lastProcessedTime) {
        if (file?.lastModified() > lastProcessedTime) {
            log.debug "File lastModified (${file.lastModified()}) more recently than lastProcessedTime: $lastProcessedTime"
            return false
        } else {
            return true
        }
    }

    /**
     * Filter helper to determine if this file is something we want to process/store, or if it is 'noise'
     * This implementation uses regex to filter out temp and system files
     *
     * Note: for *.txt will check to see if this is the result of text serialization (i.e. file exists with a parent name (file.name minus '.txt'), if so skip it as already the result of processing
     * @param file
     * @return should or not
     */
    boolean shouldProcessFile(java.io.File file, Pattern fileRegexToSkip, List<String> analysisExtensions) {
        boolean should = true
        if (file.name ==~ fileRegexToSkip) {
            log.info "File name (${file.name}) matches regex to skip, so skipping it"
            should = false

        } else {
            should = true       // redundant, but helpful in debugging

        }
        return should
    }

    /**
     * Check if the given file matches the output pattern of a 'parent' file that has been extracted (.txt) or analyzed and serialzed(.pos)
     * @param file
     * @return true if this looks like either extraction or analysis (pos == part of speech tagged text file)
     */
/*
    static boolean isExtractedSerialization(File file, List<String> analysisExtensions){
        boolean isExtracted = false

        analysisExtensions.each { String ext ->
            if (file.path.endsWith(ext)) {
                int i = (ext.size() + 1) * -1
                String pname = file.path[0..i]
                log.debug "Check to see if this ($file) is the extracted text of a parent doc: $pname"
                File parent = new File(pname)
                if (parent.exists()) {
                    log.debug "\tfile:$file is extracted/serialized text, skip it as a source file... "
                    isExtracted = true

                } else {
                    log.info "\t\tfile matches extension '$ext', but apprently not the result of extraction/serialization: $file"
                }

            } else {
                log.debug "\t\t file does not end with extension $ext -- $file"
            }
        }
        return isExtracted
    }
*/

    /**
     * Look for same file name with '.txt' at the end, if it exists assume we have already extracted text from rich text document (via tika...)
     * @param file
     * @return
     */
    static boolean hasBeenExtracted(File file) {
        if (isTextFile(file)) {
            log.debug "File: $file is already a text file, no extraction needed"
            return true
        } else {
            File extractedfile = new File(file.path + '.txt')
            if (extractedfile.exists() || isTextFile(file)) {
                return true
            }
        }
        return false
    }

    /**
     * Look for filename with '.pos' ending, if exists then assume we have already done NLP analysis (probably just part of speech tagging (POS), but perhaps NER as well)
     *
     * todo -- refactor --f'ed up coding
     */
/*
    static boolean hasBeenNLPAnalyzed(File file, List<String> analysisExtensions){
        boolean hasBeen = false
        analysisExtensions.each { String ext ->
            File nlpFile = new File(file.absolutePath + ext)
            if (nlpFile.exists()) {
                hasBeen = true
            }
        }
        return hasBeen
    }
*/

    /**
     * Helper func to determine if we want to spend extra time/resources on extracting content of file (alternatively we might just want to store name, path, time,...
     * This implementation uses regex to flag files for extaction
     * @param file
     * @return
     */
    static boolean shouldExtractFile(File file) {
        if (file.name ==~ FILE_EXTENSIONS_TO_EXTRACT_DEFAULT) {
            log.debug "\t\t File name (${file.name}) matches regex to extract, so extract it"
            return true
        } else {
            log.info "\tfile does not match extraction pattern, don't spend time extracting: $file"
        }
        return false
    }


    static Map buildMetaMapFolder(def dirObject, LinkOption linkOption = LinkOption.NOFOLLOW_LINKS) {
        Map metadata = [:]
        Path path = null
        if (dirObject instanceof Path) {
            log.debug "pathObject $dirObject is already instance of Path, no conversion needed..."
            path = dirObject
        } else {
            log.info "pathObject $dirObject is instance of '${dirObject.class.crawlName}', trying to convert..."
            path = Paths.get(dirObject)
        }

        if (Files.exists(path, linkOption)) {
            metadata.name = path.getFileName()?.toString()
            metadata.path = path
            def fs = path.fileSystem
            if (fs.toString().containsIgnoreCase('linux')) {
                metadata.filesystem = 'Linux'
                metadata.root = path.root
            } else if (fs.toString().containsIgnoreCase('windows')) {
                metadata.filesystem = 'Windows'
            } else {
                log.warn "unrecognized filesystem: ${path.fileSystem}"
                metadata.filesystem = path.fileSystem.toString()
            }

            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

            metadata.ctime = attr.creationTime()
            metadata.accessTime = attr.lastAccessTime()
            metadata.modifiedTime = attr.lastModifiedTime()
            Constants.DEFAULT_FOLDER_SIZE = 4096
            if(attr.size() > Constants.DEFAULT_FOLDER_SIZE) {
                metadata.size = attr.size()
            }

            FileOwnerAttributeView file = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
            try {             // Exception Handling to avoid any errors
                UserPrincipal user = file.getOwner();                           // Taking owner name from the file
            } catch (Exception e) {
                log.warn(e);
            }

            try {             // Exception Handling to avoid any errors
                UserPrincipal user = file.getOwner();                           // Taking owner name from the file
                metadata.owner = user.name
            } catch (Exception e) {
                log.warn(e);
            }

        } else {
            if (Files.exists(path)) {
                log.warn "Encountered a link, and we were told to skip those, so skipping metadata for: $dirObject"
            }
        }
        return metadata
    }
}
