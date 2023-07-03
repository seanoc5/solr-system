//package com.oconeco.crawler
//
//import com.oconeco.helpers.Constants
//import com.oconeco.models.FSFolder
//import org.apache.logging.log4j.core.Logger
//
//import java.nio.file.Path
//import java.util.regex.Pattern
//
//Logger log = LogManager.getLogger(this.class.name);
//
//File contentDir = new File(getClass().getResource('/content').toURI())
//def startPath = contentDir.toPath()
//log.info "test Content dir: ${contentDir.absolutePath}"
//
//String locationName = 'testScript'
//String crawlName = 'content'
//def filterFilesPattern = ~/.*\.(groovy|txt|json|lst)$/
//Pattern ignorePattern = Constants.DEFAULT_FILENAME_PATTERNS[Constants.LBL_IGNORE]
//
//File lastFolder
//File currentFolder
//int count = 0
//
//def preDirVisitor = {
//    count++
//    log.info "$count) ++++ PRE  dir:   $it"
//    currentFolder = it
//}
//
//def postDirVisitor = {
//    count++
//    log.debug "$count) ---- POST dir : $it"
//    lastFolder = currentFolder
//    int depth = getRelativeDepth(startPath, it)
//    FSFolder fsFolder = new FSFolder(it, currentFolder,  locationName, crawlName)           // todo revisit....
//    def details = fsFolder.addFileDetails()
//    def children = fsFolder.buildChildrenList(ignorePattern)
//    log.debug "Folder ($fsFolder) -- Children count: ${children.size()} -- Details($details)"
//}
//
//// bredth first - get startFolder AND files, then descend
//def sortByTypeThenName = { a, b ->
//        a.isFile() != b.isFile() ? b.isFile() <=> a.isFile() : a.name <=> b.name
//}
//
//Map optionsMap = [
////        type   : FileType.DIRECTORIES,
//        preDir : preDirVisitor,
//        postDir: postDirVisitor,
//        sort   : sortByTypeThenName,
//]
//
//
//contentDir.traverse(optionsMap) {
//    count++
//    if (it.isDirectory()) {
//        log.info "$count) CHILD DIR?? (${lastFolder?.name}:${currentFolder?.name}) dir : $it"
//    } else {
//        log.info "\t\t$count) file: $it"
//    }
//}
//
//public int getRelativeDepth(Path startPath, File f) {
//    Path reletivePath = startPath.relativize(f.toPath())
//    String relPath = reletivePath.toString()
//    int depth = relPath > '' ? reletivePath.getNameCount() : 0
//    depth
//}
