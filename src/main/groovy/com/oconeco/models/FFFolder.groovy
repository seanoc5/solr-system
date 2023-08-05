package com.oconeco.models


import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

/**
 * util class to encapsulate Firefox bookmark info (starting with exported bookmarks file)
 */
class FFFolder extends FFObject {
    Logger log = LogManager.getLogger(this.class.name)
    public static final String TYPE = 'FFFolder'

    /**
     * standard constructor
     * @param object A json object, or similar to turn into a more structured Firefox Folder object
     * @param parent - parent object or null
     * @param locationName larger domain of content (i.e. Firefox bookmarks in general -- multiple profiles...?)
     * @param crawlName - more focused domain of content, ie. ff profile
     */
    FFFolder(def object, SavableObject parent, String locationName, String crawlName) {
        super(object, parent, locationName, crawlName)

        groupObject = true      // todo -- look at interface to handle group vs item differentiation
        crawlChildren()
        dedup = buildDedupString() // todo -- move this to persistence layer? not needed/useful until we save it, and look for dups...???
    }

    /**
     * part of recursive processing setup, convert the raw json "children" to "proper" FFFolder groups/subfolders and items/bookmarks
     * use the already existing raw json import of Folder children objects
     * @return
     */
    public List<FFObject> crawlChildren() {
        crawlChildren(this.thing)
    }

    /**
     * part of recursive processing setup, convert the raw json "children" to "proper" FFFolder groups/subfolders and items/bookmarks
     * allow for potential explicit (sub)object to process (rare/unlikely??). Typically this will be just the existing raw-json Folder object from slurper
     * @return
     */
    public List<FFObject> crawlChildren(object) {
        List<FFObject> items = []
        int childCount = 0

        log.debug "crawlChildren() --children size: ${object?.children?.size()}"

        object.children?.each {
            childCount++

            FFObject child = null
            if (it.typeCode == TYPE_FOLDER_CODE) {
                child = new FFFolder(it, this, this.locationName, this.crawlName)
                childGroups << child
                items << child
                log.debug "\t\t$childCount) Added Folder: $child"
            } else if (it.typeCode == TYPE_BOOKMARK_CODE) {
                child = new FFBookmark(it, this, this.locationName, this.crawlName)
                childItems << child
                items << child
                log.debug "\t\t$childCount) Added Bookmark: $child"

            } else if (it.typeCode == TYPE_SEPARATOR_CODE) {
                log.info "\t\t$childCount) Skipping separator: $it"
            } else {
                log.warn "Unknown child type: $it"
            }
        }
        return items
    }


//    @Override
//    String buildDedupString() {
//        String d = "$type:$locationName:$name"
//    }

    List<FFObject> gatherPersistableObjects(){
        List<FFObject> list = [this] + childItems
//        list.addAll(this.childItems)
        return list
    }

    @Override
    String toString() {
        String s=  "${type}:(${name}) [depth:$depth] SubFolders:(${childGroups.size()}) -- folder bookmarks:(${childItems.size()})"
    }

    @Override
    String getType() {
        FFFolder.TYPE
    }
}
