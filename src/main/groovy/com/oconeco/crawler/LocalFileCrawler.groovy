package com.oconeco.crawler

import com.oconeco.analysis.FolderAnalyzer
import com.oconeco.models.FolderFS
import groovy.io.FileType
import org.apache.commons.io.FilenameUtils
import org.apache.log4j.Logger

import java.util.regex.Pattern

class LocalFileCrawler {
    private static Logger log = Logger.getLogger(this.class.name);

    static final String STATUS_SKIP = 'skip'
    static final String STATUS_TRACK = 'track'
    static final String STATUS_INDEX_CONTENT = 'indexContent'
    static List<String> folderNamesToSkip = [
            '__snapshots__',
            '.csv',
            '.cache',
            '.gradle',
            '.git',
            '.github',
            '.idea',
            '.m2',
            '.settings',
            '.svn',
            '.vscode',
            'assets',
            'bin',
            'build',
            'cache',
            'classes',
            'css',
            'fonts',
            'gradle',
            'hibernate',
            'images',
            'img',
            'layouts',
            'lib',
            'libs',
            'logo',
            'logs',
            'material-ui',
            'META-INF',
            'modules',
            'old',
            'out',
            'package',
            'packages',
            'persistence',
            'pkg',
            'plugins',
            'proc',
            'skin',
            'skins',
            'spring',
            'styles',
            'svg',
            'target',
            'taglib',
            'temp',
            'templates',
            'testCrawl',
            'test',
            'tests',
            'themes',
            'tmp',
            'vendor',
            'web-app',
            'webapp',
            'wrapper',

            'node_modules',
            '$global',
            'streams',
            '.svn',
            'prop-base',
            'httpcache',
            'professional-services',
            'react',
//            '',
    ]

    static final String STATUS_ANALYZE = 'analyze'

    // don't process these files, don't add them to the index, assume you will never even want to find if a file exists or not.
    static Pattern FILE_REGEX_TO_SKIP = Pattern.compile(/([.~]*lock.*|_.*|.*\.te?mp$|.*\.class$|robots.txt|.*\.odb$|.*\.pos)/)
//    static Pattern FILE_REGEX_TO_SKIP = Pattern.compile('(\\~\\$.*|_.*|.*\\.te?mp$|.*\\.class$|robots.txt|.*\\.odb$|.*\\.pos)')

    // NOTE: this is the default operation: track not skip or index, no need to specify these, this is the catch-all
    // track the file metdata and that it exissts, but don't extract, index, or analyze
    // static Pattern FILE_REGEX_TO_TRACK = Pattern.compile('(.*json|.*csv|.*xml)')

    // INDEXING file is meant to run an extraction process (i.e. tika) and then send that info to solr for indexing/persistence/searchability (analysis below goes one step further)
    static Pattern FILE_EXTENSIONS_TO_INDEX = Pattern.compile(/(bash.*|csv|groovy|ics|ipynb|java|lst|md|php|py|rdf|rss|scala|sh|tab|te?xt|tsv)/)
//    Pattern FILE_EXTENSIONS_TO_INDEX = Pattern.compile(/.*[._](csv|groovy|history|ics|ini|java|log|php|rdf|rss|xml)$/)

    // ANALYSIS is meant to go beyond indexing, and actually do more qualitative analysis (NLP, content sectioning, tagging,...)
    static Pattern FILE_EXTENSIONS_TO_ANALYZE = Pattern.compile("(aspx\\?|cfm|docx\\?|html|od.|pptx\\?|pdf|ps|pub|rss|xlsx|zhtml\\?)")


//    /**
//     * get list of folders deemed appropriate to crawl based on regex of folders to skip
//     * todo resolve better/righter approach here?
//     *
//     * @param sourceFolder
//     * @param foldersToExclude
//     * @return
//     */
//    public static Set<File> getFoldersToCrawl(File sourceFolder, Pattern foldersToExclude) {
//        log.info "Crawl source folder: ${sourceFolder.absolutePath} -- exclude regex: $foldersToExclude"
//        Set<File> foldersToCrawl = [sourceFolder]
//        sourceFolder.eachFileRecurse(FileType.DIRECTORIES) { File subfolder ->
//            if (subfolder.name ==~ foldersToExclude) {
//                log.info "\t\tSkipping folder matching skip regex: ${subfolder.name}"
//            } else {
//                log.debug "\t\tAdding folder: ${subfolder.name}"
//                foldersToCrawl << subfolder
//            }
//        }
//        return foldersToCrawl
//    }


    /**
     * get List of files deemed appropriate to crawl (based on String list of foldernames (vs regex) to skip
     * todo resolve better/righter approach here?
     * @param sourceFolder
     * @param foldersNamesToSkip
     * @return List of folders to crawl
     */
    public static List<File> getFoldersToCrawl(File sourceFolder, Map folderNamePatterns = null, int level = 1, int statusDepth = 3) {
        log.debug "\t\tCrawl source folder: ${sourceFolder.absolutePath} -- skip folders: $folderNamesToSkip -- level: $level"
        if (level <= statusDepth) {
            log.info "\t\tLvl:$level) Crawl source folder: ${sourceFolder.absolutePath}"
        }
        Pattern ignoreFolderNames = folderNamePatterns[FolderAnalyzer.LBL_IGNORE]

        List<File> foldersToCrawl = [sourceFolder]
        sourceFolder.eachFile(FileType.DIRECTORIES) { File subfolder ->
            boolean ignore = (subfolder.name ==~ ignoreFolderNames)
            if (ignore) {
                log.info "\t\t------------ Skipping folder matching skip foldername: ${subfolder.name} -- ${subfolder.canonicalPath}"
            } else {
                log.debug "\tAdding folder: ${subfolder.name.padLeft(30)} -- $subfolder.absolutePath"
                List<File> crawlFolders = getFoldersToCrawl(subfolder, folderNamePatterns, level + 1, statusDepth)
                foldersToCrawl.addAll(crawlFolders)
                if (foldersToCrawl.size() > 0) {
                    log.debug "foldersToCrawl: ${foldersToCrawl.size()}"
                }
            }
        }
        return foldersToCrawl
    }


    static List<FolderFS> gatherFolderFSToCrawl(File folder,Pattern ignoreFileNames,  Pattern ignoreFolderNames, int depth = 1){
        if(depth < 3){
            log.info "\t\tLvl:$depth) Crawl source FS folder: ${folder.absolutePath}\""
        }
        FolderFS ffs = new FolderFS(folder, ignoreFileNames, ignoreFolderNames, depth + 1)
        List<FolderFS> fsFolders = [ffs]
//        int childDepth = depth + 1
//        folder.eachDir {File subdir ->
//            if(subdir.name ==~ ignoreFolderNames){
//                FolderFS subFfs = new FolderFS(subdir)
//                subFfs.assignedTypes << BaseObject.IGNORE_LABEL
//                log.info "IGNORE subdir: $subFfs (i.e store this folder, but don't descend)"
//                fsFolders << subFfs
//            } else {
//                List<FolderFS> childFSFolders = gatherFolderFSToCrawl(subdir, ignoreFileNames, ignoreFolderNames, childDepth)
//                fsFolders.addAll(childFSFolders)
//            }
//        }
        return fsFolders
    }


/*
    public static List<FolderFS> getFSFoldersToCrawl(FolderFS sourceFSFolder, Map folderNamePatterns = null, int level = 1, int statusDepth = 3) {
        log.debug "\t\tCrawl source FS folder: ${sourceFSFolder.absolutePath} -- skip folders: $folderNamesToSkip -- level: $level"
        if (level <= statusDepth) {
            log.info "\t\tLvl:$level) Crawl source FS folder: ${sourceFSFolder.me.absolutePath}"
        }
        Pattern ignoreFolderNames = folderNamePatterns[FolderAnalyzer.LBL_IGNORE]

        List<FolderFS> foldersFSToCrawl = [sourceFSFolder]
        sourceFSFolder.me.eachFile(FileType.DIRECTORIES) { File subfolder ->
            boolean ignore = (subfolder.name ==~ ignoreFolderNames)
            if (ignore) {
                log.info "\t\t------------ Skipping folder matching skip foldername: ${subfolder.name} -- ${subfolder.canonicalPath}"
            } else {
                log.debug "\tAdding folder: ${subfolder.name.padLeft(30)} -- $subfolder.absolutePath"
                List<FolderFS> crawlFSFolders = getFSFoldersToCrawl(subfolder, folderNamePatterns, level + 1, statusDepth)
                foldersFSToCrawl.addAll(crawlFolders)
                if (foldersFSToCrawl.size() > 0) {
                    log.debug "foldersToCrawl: ${foldersFSToCrawl.size()}"
                }
            }
        }
        return foldersFSToCrawl
    }
*/


    /**
     * get map of files in a given folder with grouping on (with defaults)
     */
//    public static Map<String, List> getFilesToCrawl(File sourcefolder) {
//        getFilesToCrawl(sourcefolder, FILE_REGEX_TO_SKIP, FILE_EXTENSIONS_TO_ANALYZE, FILE_EXTENSIONS_TO_INDEX)
//    }


    /**
     * get map of files in a given folder with grouping on:
     *      skip:       skip entirely--or, more helpfully: store names in a single multivalued field in a "skip" doc in solr for the given folder/dir
     *      track:      save file info, but do not extract/index
     *      index:      save file into, extract/index
     *      analyze:    save file into, extract/index, analyze (nlp, structural breakdown,...)
     * @param sourcefolder
     * @param filesToExclude
     * @return
     */
    public static Map<String, List> getFilesToCrawl(File sourcefolder, Pattern filesToExclude = FILE_REGEX_TO_SKIP, Pattern extensionToIndex = FILE_EXTENSIONS_TO_INDEX, Pattern extensionToAnalyze = FILE_EXTENSIONS_TO_ANALYZE) {
        Map<String, Collection<File>> filesToCrawl = [skip: [], track: [], index: [], analyze: [], extensions: []]
        // create a map of files grouped by category: skip, track, index, analyze

        sourcefolder.eachFile {
            log.debug "File: $it"
        }

        sourcefolder.eachFile(FileType.FILES) { File file ->
            String fname = file.name
//            log.info "Filename: $fname"
            String ext = FilenameUtils.getExtension(fname)


            List parts = fname.split('\\.')
            if (!fname.startsWith('.') && parts.size() > 1) {
                String extension = parts.size() > 1 ? parts[-1] : ''
                filesToCrawl.extensions << extension
            } else {
                log.debug "No extension found (fname: $fname) -- parts: $parts"
            }
            if (fname.contains("html")) {
                log.debug "Html: $fname"
            }

            if (fname ==~ filesToExclude) {
                log.debug "\t\tSKIP file (to exclude): ${fname}"
                filesToCrawl.skip << file

            } else if (ext) {
                if (ext ==~ extensionToIndex) {
                    log.debug "\t\tExtract/Index file: $fname"
                    filesToCrawl.index << file

                } else if (ext ==~ extensionToAnalyze) {
                    log.info "\t\tAnalyze file: $fname"
                    filesToCrawl.analyze << file

                } else {
                    log.debug "\t\ttrack file: $fname"
                    filesToCrawl.track << file
                }

            } else {
                log.debug "\t\ttrack file: $fname"
                filesToCrawl.track << file

            }
        }
        return filesToCrawl
    }


    public static Map<File, Map> getFilesToCrawlList(File sourcefolder, Pattern filesToSkip = FILE_REGEX_TO_SKIP, Pattern extensionToIndex = FILE_EXTENSIONS_TO_INDEX, Pattern extensionToAnalyze = FILE_EXTENSIONS_TO_ANALYZE) {
        log.debug "Get files to crawl from folder: ($sourcefolder)"
        Map<File, Map> filesToCrawl = [:]

        int i = 0
        sourcefolder.eachFile(FileType.FILES) { File file ->
            i++
            String fname = file.name
            String ext = FilenameUtils.getExtension(fname)
            log.debug "\t\tFile: $file"

            if (fname ==~ filesToSkip) {
                log.info "\t\t\t\t$i) SKIP file (to exclude): ${fname}"
                filesToCrawl.put(file, [extension: ext, status: STATUS_SKIP])

            } else if (ext) {
                if (ext ==~ extensionToIndex) {
                    log.info "\t\t$i) Extract/Index file: $fname"
                    filesToCrawl.put(file, [extension: ext, status: STATUS_INDEX_CONTENT])

                } else if (ext ==~ extensionToAnalyze) {
                    log.info "\t\t$i) ANALYZE file: $fname"
                    filesToCrawl.put(file, [extension: ext, status: STATUS_ANALYZE])

                } else {
                    log.debug "\t\t$i) track file: $fname"
                    filesToCrawl.put(file, [extension: ext, status: STATUS_TRACK])
                }

            } else {
                log.debug "\t\t$i) track file: $fname"
                filesToCrawl.put(file, [extension: ext, status: STATUS_TRACK])
            }
        }

        return filesToCrawl
    }


    // NOTE - file-folder for each recurse not a great idea because you can't skip crawling "noise" folders, so we use the actual recursive call: getFoldersToCrawl()
/*    public static List<File> getFoldersToCrawlRecurse(String srcFolder) {
        File sourceFolder = new File(srcFolder)
        getFoldersToCrawlRecurse(sourceFolder)
    }

    public static List<File> getFoldersToCrawlRecurse(File sourceFolder) {
        List<File> foldersToCrawl = []
        sourceFolder.eachDirRecurse {File subfolder ->
            if (folderNamesToSkip.contains(subfolder.name)) {
                log.debug "\t\tSkipping folder matching skip foldername: ${subfolder.name} -- ${subfolder.canonicalPath}"
                foldersToCrawl << subfolder
            } else {
                log.debug "\tAdding folder: ${subfolder.name.padLeft(30)} -- $subfolder.absolutePath"
                foldersToCrawl << subfolder
            }
        }
        return foldersToCrawl
    }*/


}
