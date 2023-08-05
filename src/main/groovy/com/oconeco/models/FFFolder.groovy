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

    FFFolder(def bookmark, SavableObject parent, String locationName, String crawlName) {
        super(bookmark, parent, locationName, crawlName)
        children = bookmark.children
        groupObject = true
        bookmark.children?.each{
            log.info "\t\tChild: ${it.size()}"
        }
//        childGroups = children.findAll{it.
//        id = SavableObject.buildId(locationName, path)
//        dedup = buildDedupString()
//        log.debug "FFPlace:(${this.toString()})"
        List<FFObject> items = []
        int childCount = 0
        log.info "test ${object.size()}"

        object.children?.each {
            log.info "it"

            FFObject child = null
            if (it.typeCode == TYPE_FOLDER_CODE) {
                child = new FFFolder(it, this, this.locationName, this.crawlName)
                childGroups << child
                items << child
                childCount++
            } else if (it.typeCode == TYPE_BOOKMARK_CODE) {
                child = new FFBookmark(it, this, this.locationName, this.crawlName)
                childItems << child
                items << child
                childCount++
            } else if (it.typeCode == TYPE_SEPARATOR_CODE) {
                log.info('test')
                log.info "\t\tSkipping separator: $this"
            } else {
                log.warn "Unknown child type: $it"
            }
        }
        if (items) {
            log.info "Created (${items.size()}) child items"
        } else {
            log.info "\t\tno child items for this object: $this"
        }

    }



    def gatherChildren(){
        List<FFObject> items = []
        int childCount = 0
//        def
        log.info "test ${object.size()}"

        object.children?.each {
            log.info "it"

            FFObject child = null
            if (it.typeCode == TYPE_FOLDER_CODE) {
                child = new FFFolder(it, this, this.locationName, this.crawlName)
                childGroups << child
                items << child
                childCount++
            } else if (it.typeCode == TYPE_BOOKMARK_CODE) {
                child = new FFBookmark(it, this, this.locationName, this.crawlName)
                childItems << child
                items << child
                childCount++
            } else if (it.typeCode == TYPE_SEPARATOR_CODE) {
                log.info('test')
                log.info "\t\tSkipping separator: $this"
            } else {
                log.warn "Unknown child type: $it"
            }
        }
        if (items) {
            log.info "Created (${items.size()}) child items"
        } else {
            log.info "\t\tno child items for this object: $this"
        }

    }

//    @Override
//    String buildDedupString() {
//        String d = "$type:$locationName:$name"
//    }
}
