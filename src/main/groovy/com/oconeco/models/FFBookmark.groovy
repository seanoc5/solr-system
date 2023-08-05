package com.oconeco.models

import groovy.sql.GroovyResultSet
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
class FFBookmark extends FFObject {
    Logger log = LogManager.getLogger(this.class.name)
    public static final String TYPE = 'FFPlace'

    FFBookmark(def object, SavableObject parent, String locationName, String crawlName) {
        super(object, parent, locationName, crawlName)
        String t = object.tags
        if (t){
            tags = t.split(',')
        }
        this.ffId = object.id
        mimeType = object.type
        uri = object.uri


        dedup = buildDedupString()
    }



    @Override
    String buildDedupString() {
        String d = "$type:$locationName:$uri"
    }
}
