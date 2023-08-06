package misc

import com.oconeco.models.FFBookmark
import com.oconeco.models.FFFolder
import com.oconeco.models.FFObject
import com.oconeco.persistence.SolrSystemClient
import groovy.json.JsonSlurper
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.LogManager
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument

/**
 * hacked script to check for duplicates.
 * it appears my "Job" bookmarks were a near-subset of my personal bookmarks. There must have been crossed wires.
 * Anyhow: this script can be ignored, it was just hacking to find and confirm the majority of lw bookmarks were dups
 */
Logger log = LogManager.getLogger(this.class.name);
String locationName = 'Firefox Profile'
String crawlName = 'sean.oconnor@lucidworks.com'

File cacheFolder = new File("C:/Users/bentc/Desktop/")
if (cacheFolder.exists() && cacheFolder.isDirectory()) {
    log.info "Using cache folder:(${cacheFolder.absolutePath}...)"
} else {
    boolean success = cacheFolder.mkdirs()
    log.info "MAKING cache folder:(${cacheFolder.absolutePath}) :: success:$success"
}

Map sourceMap = [seanoc5   : new File("C:/Users/bentc/Desktop/ff-bookmarks-2023-08-04.json"),
                 lucidworks: new File("C:/Users/bentc/Desktop/ff-lucidworks-bookmarks-2023-08-05.json")
]
Map<String, List> flattendMap = [:]

sourceMap.each { String label, File exportedBookmarks ->
    log.info "Using source bookmarks file($label): ${exportedBookmarks.absolutePath}"

    List<FFObject> childObjects = []

    def fileSaveClosure = { FFFolder ffFolder, int depth ->
        int childSize = ffFolder.childItems.size()
        log.info "\t\t$depth) solrSaveClosure for folder:($ffFolder) -- child items:(${childSize})"
        List toSaveList = [ffFolder] + ffFolder.childItems
        childObjects.addAll(toSaveList)
    }


    if (exportedBookmarks?.exists() && exportedBookmarks.canRead()) {
        JsonSlurper slurper = new JsonSlurper()
        def json = slurper.parse(exportedBookmarks)
        log.debug "ROOT json size (top level): ${json.size()}"

        FFFolder rootFolder = new FFFolder(json, null, locationName, crawlName)
        log.info "Root folder: $rootFolder"

        rootFolder.walktree(fileSaveClosure, 0)

    } else {
        log.error "No exportedBookarks file: ${exportedBookmarks.absolutePath}, nothing to do...."
    }

    log.info "Child objects size: ${childObjects.size()}"


    File outfile = new File(cacheFolder.parentFile, "ff.${label}.items.csv")
    log.info "Writing output to file:(${outfile.absolutePath})..."
    outfile.withWriter { BufferedWriter writer ->
        List<String> fieldNames = FFBookmark.DEFAULT_CSV_FIELDS
        writer.write(fieldNames.join(',') + '\n')
        childObjects.each { FFObject child ->
            String s = child.toCSVRecord(fieldNames)
            writer.write("${s}\n")
        }
    }
    log.info "finished writing records to file:($outfile) -- size:${outfile.size()}"
    flattendMap.put(label, childObjects)
}
List gmailVals = flattendMap['seanoc5']
Map<String, List<Object>> gmailByIds = gmailVals.groupBy { it.id }
Map<String, List<Object>> gmailByGuids = gmailVals.groupBy { it.guid }
List gmailBks = gmailVals.findAll { it.type == 'FFBookmark' }
Set<String> gmailIds = gmailByIds.keySet()
Set<String> gmailGuids = gmailByGuids.keySet()
def foo = gmailIds.countBy { it }.grep { it.value > 1 }.collect { it.key }

List lwVals = flattendMap['lucidworks']
//List<String> lwIds = lwVals.collect {it.id}
Map<String, List<Object>> lwByIds = lwVals.groupBy { it.id }
Map<String, List<Object>> lwByGuids = lwVals.groupBy { it.guid }
List<String> lwBks = lwVals.findAll { it.type == 'FFBookmark' }
Set<String> lwIds = lwByIds.keySet()
Set<String> lwGuids = lwByGuids.keySet()

def bothIds = gmailIds.intersect(lwIds)
def bothGuids = gmailGuids.intersect(lwGuids)
log.warn "Both ids size: ${bothIds.size()} -- Both guids:${bothGuids.size()}"

def dupIdMapGmail = gmailByIds.subMap(bothIds)
def dupGuidMap = gmailByGuids.subMap(bothGuids)

dupIdMapGmail.each { String id, List<FFObject> vals ->
    FFObject gmObject = vals[0]
    String gmPath = gmObject.path

    def lwList = lwByIds.get(id)
    if (lwList) {
        FFObject lwObject = lwList[0]
        String lwPath = lwObject.path
        if (lwPath == gmPath) {
            log.debug "Same paths: $lwPath for id:$id"
        } else {
            log.warn "Different paths, LW:($lwPath) != gmail:($gmPath)"
        }
    } else {
        log.warn "No matching LW object!!"
    }

}

log.info "Done..."
