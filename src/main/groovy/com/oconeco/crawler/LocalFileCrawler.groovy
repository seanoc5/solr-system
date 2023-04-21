package com.oconeco.crawler


import com.oconeco.analysis.FolderAnalyzer
import com.oconeco.models.FolderFS
import groovy.io.FileType
import org.apache.commons.io.FilenameUtils
import org.apache.log4j.Logger

import java.util.regex.Pattern

class LocalFileCrawler extends BaseCrawler{
    private static Logger log = Logger.getLogger(this.class.name);

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
            'misc.testCrawl',
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

    // don't process these files, don't add them to the index, assume you will never even want to find if a file exists or not.
    static Pattern FILE_REGEX_TO_SKIP = ~/([.~]*lock.*|_.*|.*\.te?mp$|.*\.class$|robots.txt|.*\.odb$|.*\.pos)/

    // NOTE: this is the default operation: track not skip or index, no need to specify these, this is the catch-all
    // track the file metdata and that it exissts, but don't extract, index, or analyze
    // static Pattern FILE_REGEX_TO_TRACK = Pattern.compile('(.*json|.*csv|.*xml)')

    // INDEXING file is meant to run an extraction process (i.e. tika) and then send that info to solr for indexing/persistence/searchability (analysis below goes one step further)
    static Pattern FILE_EXTENSIONS_TO_INDEX = ~/(bash.*|csv|groovy|ics|ipynb|java|lst|md|php|py|rdf|rss|scala|sh|tab|te?xt|tsv)/

    // ANALYSIS is meant to go beyond indexing, and actually do more qualitative analysis (NLP, content sectioning, tagging,...)
    static Pattern FILE_EXTENSIONS_TO_ANALYZE = ~"(aspx\\?|cfm|docx\\?|html|od.|pptx\\?|pdf|ps|pub|rss|xlsx|zhtml\\?)"

    LocalFileCrawler(String name, String location, Object source) {
        super(name, location, source)
    }

    @Override
    def startCrawl(def startFolder, Map namePatterns) {
        log.info "Start crawl (startfolder: $startFolder) with name patterns map: $namePatterns"
        throw new AbstractMethodError("Incomplete code problem, someone complete this function: startCrawl(Map namePatterns)")
        return null
    }

    def gatherFoldersToCrawl(File sourceFolder, FolderAnalyzer folderAnalyzer){

    }

    /**
     * get List of files deemed appropriate to crawl (based on String list of foldernames (vs regex) to skip
     * todo resolve better/righter approach here?
     * @param sourceFolder
     * @param foldersNamesToSkip
     * @return List of folders to crawl
     * @deprecated look to getFoldersToCrawlMap for more recent approach...
     */
    public static List<File> getFoldersToCrawl(File sourceFolder, Map folderNamePatterns = null, int level = 1, int statusDepth = 3) {
        log.debug "\t\tCrawl source folder: ${sourceFolder.absolutePath} -- skip folders: $folderNamesToSkip -- level: $level"
        if (level <= statusDepth) {
            log.info "\t\tLvl:$level) Crawl source folder: ${sourceFolder.absolutePath}"
        }
        Pattern ignoreFolderNames = folderNamePatterns[FolderAnalyzer.LBL_IGNORE]
        if (ignoreFolderNames) {
            log.debug "Got folder names to ignore: ${ignoreFolderNames}"
        } else {
            log.info "No folder names to ignore, nothing matching (${FolderAnalyzer.LBL_IGNORE}) in folderNamePatterns:(${folderNamePatterns})"
        }

        List<File> foldersToCrawl = [sourceFolder]
        List<File> foldersToIgnore = []
        sourceFolder.eachFile(FileType.DIRECTORIES) { File subfolder ->
            boolean ignore = (subfolder.name ==~ ignoreFolderNames)
            if (ignore) {
                log.info "\t\t------------ Skipping folder matching skip foldername: ${subfolder.name} \t\t--\t\t ${subfolder.canonicalPath}"
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

    /**
     * Get map results of folders to crawl, with ignored files list as well
     * @param sourceFolder
     * @param folderNamePatterns
     * @param level
     * @param statusDepth
     * @return map of folders to crawl, as well as those folders we chose to ignore...
     * todo -- remove list-only version above?
     */
    public static Map<String, List<File>> getFoldersToCrawlMap(File sourceFolder, Map folderNamePatterns = null, int level = 1, int statusDepth = 3) {
        log.debug "\t\tCrawl source folder: ${sourceFolder.absolutePath} -- skip folders: $folderNamesToSkip -- level: $level"
        if (level <= statusDepth) {
            log.info "\t\tLvl:$level) Crawl source folder: ${sourceFolder.absolutePath}"
        }
        Pattern ignoreFolderNames = folderNamePatterns[FolderAnalyzer.LBL_IGNORE]
        if (ignoreFolderNames) {
            log.debug "Got folder names to ignore: ${ignoreFolderNames}"
        } else {
            log.info "No folder names to ignore, nothing matching (${FolderAnalyzer.LBL_IGNORE}) in folderNamePatterns:(${folderNamePatterns})"
        }

//        List<File> foldersToCrawl = [sourceFolder]
//        List<File> foldersToIgnore = []
        Map<String, List<File>> crawlMap = [foldersToCrawl: [sourceFolder], foldersToIgnore: []]
        sourceFolder.eachFile(FileType.DIRECTORIES) { File subfolder ->
            boolean ignore = (subfolder.name ==~ ignoreFolderNames)
            if (ignore) {
                log.info "\t\t------------ Skipping folder matching skip foldername: ${subfolder.name} -- ${subfolder.canonicalPath}"
                crawlMap.foldersToIgnore << subfolder
            } else {
                log.debug "\tAdding folder: ${subfolder.name.padLeft(30)} -- $subfolder.absolutePath"
                Map<String, List<File>> subCrawlMap = getFoldersToCrawlMap(subfolder, folderNamePatterns, level + 1, statusDepth)

                if (subCrawlMap.foldersToCrawl?.size() > 0) {
                    log.debug "foldersToCrawl: ${subCrawlMap.foldersToCrawl.size()}"
                    crawlMap.foldersToCrawl.addAll(subCrawlMap.foldersToCrawl)
                } else {
                    log.info "No subFolders to crawl: ${subCrawlMap.foldersToCrawl}"
                }
                if (subCrawlMap.foldersToIgnore) {
                    log.debug "Found sub folders to ignore: ${subCrawlMap.foldersToIgnore}"
                    crawlMap.foldersToIgnore.addAll(subCrawlMap.foldersToIgnore)
                }
            }
        }
        return crawlMap
    }


    /**
     * FolderFS constructor can recurse through all child folders and build a tree of files to index (as well as ignore certain files and folders)
     * Is this approach too heavy? Any value of this over a separate crawler process?
     * @param folder
     * @param ignoreFileNames
     * @param ignoreFolderNames
     * @param depth
     * @return list of FolderFS
     * @deprecated I don't think the constructor approach is right, deprecating in favor of more explicit (and less surprising) method based approach (the original approach)
     */
    static List<FolderFS> gatherFolderFSToCrawl(File folder, Pattern ignoreFileNames, Pattern ignoreFolderNames, int depth = 1) {
        if (depth < 3) {
            log.info "\t\tLvl:$depth) Crawl source FS folder: ${folder.absolutePath}\""
        }
        FolderFS ffs = new FolderFS(folder, depth + 1, ignoreFileNames, ignoreFolderNames)
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


    /**
     *
     * @param sourcefolder
     * @param filesToSkip
     * @param extensionToIndex
     * @param extensionToAnalyze
     * @return
     */
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
