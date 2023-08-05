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
 * todo -- consider interface??
 * structure for handling information about a filesystem object,
 */
class FFObject extends SavableObject {
    Logger log = LogManager.getLogger(this.class.name)

    public static final int TYPE_BOOKMARK_CODE = 1
    public static final int TYPE_FOLDER_CODE = 2
    public static final int TYPE_SEPARATOR_CODE = 3
    public static final int MICROS_LEN = '1686926500000000'.length()
    public static final String TYPE = 'FFPObject'

    /** firefox id for tracking/comparison & will concat with type to create SolrSystem id */
    String ffId = ''
    String uri = ''
    def children = []

    FFObject(def object, SavableObject parent, String locationName, String crawlName) {
        super(object, parent, locationName, crawlName)
        type = TYPE
        id = type + ':' + object.guid
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

//    @Override
//    String buildDedupString() {
//        String d = "$type:$locationName:$name"
//    }
}
