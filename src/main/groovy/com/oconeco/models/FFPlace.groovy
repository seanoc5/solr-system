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
 * todo -- consider interface??
 * structure for handling information FireFox web browser profile sqlite database entry: places (from $profiledir/$profilename/places.sqlite),
 * e.g. /Users/[[youruseraccthere]]/AppData/Roaming/Mozilla/Firefox/Profiles/fi02zmh7.[[ff-profilename-here]]/places.sqlite
 *
 */
class FFPlace extends FFObject {
    Logger log = LogManager.getLogger(this.class.name)
    public static final String TYPE = 'FFPlace'


    FFPlace(GroovyResultSet resultSet, SavableObject parent, String locationName, String crawlName) {
        super(resultSet, parent, locationName, crawlName)
        type = TYPE
        name = resultSet.url
        path = resultSet.url
        if (!this.crawlName) {
            log.warn "Crawl NOT name already set?? $crawlName"
            this.crawlName = crawlName
        }
        if (!this.locationName) {
            log.warn "Location name NOT already set?? $locationName"
            this.locationName = locationName
        }

        id = SavableObject.buildId(locationName, path)

        Long lvd = resultSet.last_visit_date
        if (lvd) {
            long l = lvd / 1000       // need to adjust for Java Date magnitude
            Date d = new Date(l)
            lastModifiedDate = new Date(l)
        } else {
            log.debug "No last visit date on FFPlace:($this) -- lvd:$lvd"
        }

        dedup = buildDedupString()
        log.debug "FFPlace:(${this.toString()})"
    }

    @Override
    String buildDedupString() {
        String d = "$type:$locationName:$name"
    }
}
