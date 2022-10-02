import com.oconeco.models.FolderFS
import org.apache.log4j.Logger

import java.util.regex.Pattern

/**
 * @deprecated early test for processing files, see @locateFoldersFSConstructor
 */
//DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir)))
Logger log = Logger.getLogger(this.class.name);

File srcFile = new File('/home/sean/work/OconEco')
Pattern foldersToExclude = ~/.gradle/

log.info "Crawl source folder: ${srcFile} -- exclude regex: $foldersToExclude"
Map dirMap = [:]

int i = 0
srcFile.eachDirRecurse { File folder ->
    i++
    def files = folder.listFiles()
    int cnt = files.size()
    File parent = folder.parentFile
    FolderFS parentFolder = dirMap[parent]
    FolderFS fileFolder = new FolderFS(folder, files, parentFolder)
        dirMap[folder] = fileFolder
    if (i % 1000 == 0) {
        log.info "\t\t$i) $folder :: ${cnt}"
    } else {
        log.debug "\t\t$i) $folder :: ${cnt}"
    }
}

def grouped = dirMap.groupBy { key, val ->
    List<String> parts = key.toString().split('/')
    String s = null
    if (parts[-1]) {
        s = parts[-1]
    } else {
        s = parts[-2]
    }
    return s
}

grouped.each { String name, def groupedDirs ->
    int gcount = groupedDirs.size()
    if (gcount > 1) {
        log.info "Process name: $name with $gcount count..."
    } else {
        log.info "Process name: $name -- single entry"
    }
    groupedDirs.each { File dir, def files ->
        log.info "\t\tProcess dir: $dir   -> with ${files.size()} files "
    }
}

log.info "done?"

