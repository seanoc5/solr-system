package com.oconeco.crawler

import org.apache.log4j.Logger
/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   10/15/22, Saturday
 * @description:
 */

abstract class BaseCrawler {
    private static Logger log = Logger.getLogger(this.class.name);

    static final String STATUS_SKIP = 'skip'
    static final String STATUS_TRACK = 'track'
    static final String STATUS_INDEX_CONTENT = 'indexContent'
    static final String STATUS_ANALYZE = 'analyze'
//    static final String NAME_PATTERN_SKIP = 'analyze'

    String crawlName
    String crawlLocation
    String startingSource
    int statusDepth
//    Map<String, Pattern> namePatterns = null
//    Pattern skipPattern = []

    /**
     * Standard constructor of a crawl with name, location/host, source/path, and depth of crawl that should print status msgs
     * @param name name of crawl -- free form, and human friendly
     * @param location location or host to differentiate multiple types of crawls (i.e. filesystem crawls from different computers)
     * @param source the path or place to start the crawl (url, uri,...)
     * @param statusDepth depth of crawl that should emit status messages (log messages, etc)
     */
    BaseCrawler(String name, String location, int statusDepth = 3) {
        crawlName = name
        crawlLocation = location
//        startingSource = source
        this.statusDepth = statusDepth
        log.debug "Base crawler abstract object constructor: name:$name, location:$location, statusDepth:$statusDepth"
    }


//    abstract def startCrawl()
    /**
     * abstract placeholder for the command (implemented in concrete class) to start the crawl
     * @param source starting source for crawl (i.e. top folder for filesystem, website url,...)
     * @param namePatterns map of tags and name patterns to match, there are a few common tags which have standard meaning/actions:
     * <ul>
     * <li> STATUS_SKIP
     * <li> STATUS_TRACK
     * <li> STATUS_INDEX_CONTENT
     * <li> STATUS_ANALYZE
     * </ul>
     * @return something useful (def==Object) which is basically "anything"
     */
    abstract def startCrawl(def source, Map namePatterns)
}
