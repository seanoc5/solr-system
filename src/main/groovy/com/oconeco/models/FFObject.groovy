package com.oconeco.models

import com.oconeco.persistence.SolrSystemClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.solr.common.SolrInputDocument

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

/**
 * todo -- consider interface??
 * structure for handling information about a filesystem object,
 */
class FFObject extends SavableObject {
    Logger log = LogManager.getLogger(this.class.name)

    public static final int TYPE_BOOKMARK_CODE = 1
    public static final int TYPE_FOLDER_CODE = 2
    public static final int TYPE_SEPARATOR_CODE = 3
    // todo -- are there more codes? I think we just ignore anything not 1 or 2 at the moment...
    public static final int MICROS_LEN = '1686926500000000'.length()
    //semi-random microsecond value from an actual bookmark/entry -- ignore the value, just using the magnitude
    public static final String TYPE = 'FFObject'

    /** firefox id for tracking/comparison & will concat with type to create SolrSystem id */
    String ffId = ''
    /** FF bookmark "global" id... some are definately NOT global, but the important ones seem to be */
    String guid
    /** probably just an order int for display */
    Integer idx = null

    FFObject(def object, SavableObject parent, String locationName, String crawlName) {
        super(object, parent, locationName, crawlName)
        type = this.getType()
        id = type + ':' + object.guid
        guid = object.guid
        ffId = object.id
        idx = object.index
        name = object.title

        long lm = object.lastModified
        if (lm) {
            lastModifiedDate = convertMicrosecondsToDate(lm)
        } else {
            log.warn "No lastModified date for FireFox object: ($this)"
        }

        long da = object.lastModified
        if (da) {
            createdDate = convertMicrosecondsToDate(da)
        } else {
            log.warn "No dateAdded date for FireFox object: ($this)"
        }

        // todo -- save FF type to metadata??
        // metadata.fftype = object.type //???
    }

    def walktree(def closure, int depth = 0) {
        log.info "$depth) walk tree, this folder:$this (closure...)"
        childGroups.each { FFFolder cf ->
            closure(cf, depth)
            def rs = cf.walktree(closure, depth++)
            log.debug "$depth) result state: $rs"
        }
    }

    /**
     * helper to convert microseconds to date (better approach in 'normal java' but didn't find it quickly, so....
     * @param microseconds (e.g. 1686926500000000)
     */
    Date convertMicrosecondsToDate(def microseconds) {
        Date date = null
        if (microseconds) {
            long milliseconds = microseconds / 1000
            if (microseconds instanceof String && microseconds.isNumber()) {
                if (microseconds.length() == MICROS_LEN) {
                    date = new Date(milliseconds)
                } else if (microseconds.length() == (MICROS_LEN - 4)) {
                    date = new Date(microseconds)
                    log.warn "Called convertMicrosecondsToDate with regular milliseconds ($microseconds) rather than microseconds????"
                }
            } else if (microseconds instanceof Long) {
                date = new Date(milliseconds)
//                if (date.year > 2000 && date.year < 2030) {
//                    log.debug "\t\tconvertMicrosecondsToDate($microseconds) looks good: $date"
//                } else {
//                    log.warn "\t\tconvertMicrosecondsToDate($microseconds) looks BAD: $date (assumed microseconds passed in, but perhaps it was milliseconds???"
//                }
            }
        }
        return date
    }


    @Override
    SolrInputDocument toPersistenceDocument() {
        SolrInputDocument doc = super.toPersistenceDocument()
        if (ffId) {
            doc.addField('ffId_s', ffId)
        } else {
            log.warn "no FF ID ($ffId) for this object/bookmark:($this)"
        }
        if (guid) {
            doc.addField('ffGuid_s', guid)
        } else {
            log.warn "no FF GUID ($guid) for this object/bookmark:($this)"
        }
        if (idx!=null) {
            doc.addField('ffIndex_i', idx)
        } else {
            log.info "no FF ID (?null??) for this object/bookmark:($this)"
        }

        return doc
    }

//    @Override
//    String buildDedupString() {
//        String d = "$type:$locationName:$name"
//    }
}
