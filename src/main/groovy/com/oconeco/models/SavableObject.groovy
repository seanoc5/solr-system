package com.oconeco.models

import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSaver
import org.apache.log4j.Logger
import org.apache.solr.common.SolrInputDocument

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

class SavableObject {
    Logger log = Logger.getLogger(this.class.name);
    /** the source thing (File, Folder, bookmark, email, todo, browser history,...) */
    def thing
    /** absolute file path for files/folders, or other unique id for other descendants -- set by descendant object/implementation */
    String id
    /** label or name of thing */
    String name
    /** path or similar */
    String path
    /** object type label */
    String type
    /** if hierarchacical (quite common) what is the depth here? */
    Integer depth = 0
    /** size of thing -- (ie count of files and subdirs in a folder */
    Long size
    /** name of 'source', where this came from, e.g.: hostname, browser profile, email account,.... */
    String locationName
    /** name of 'crawl', useful for segmenting the source into various logical sub-sections,
     * e.g. email folders, or filesystem start folders with different content contexts */
    String crawlName

    /** date created  (i.e. how <em>old</em> is it?) */
    Date createdDate
    /** track updated/lastmodified date */
    Date lastModifiedDate

    boolean hidden = false

    /** link to parent (if any) */
    def parent
    /** links to children (if any) */
    List<SavableObject> children
    /** rough metric for how recent this is (remove?) */
    def recency
    /** rough metric for how popular or well-used this dir is */
    def popularity

    // todo -- look into using categories as well?
    /** type assigned programmatically (by this package's analysis code) */
    List<String> labels
    /** user tagging */
    List<String> tags
    /** machine learning clusters (unsupervised) */
    List<String> mlClusterType
    /** machine learning classifics (supervised) */
    List<String> mlClassificationType

    /** a string that should catch duplicate entries this thing, and hence */
    String dedup = null

    // these may not be applicable for all, but perhaps enough to move into this base object
    String mimeType
    String owner

//    List<String> permissions
    Date lastAccessDate
//    Date lastModifyDate
    Boolean archive = false
    Boolean compressed = false
    /** general flag on should the system ignore this object.
     * Note: crawler may still include object (and maybe even content) and save it,
     * but this flag can mean it should be filtered out of search & analysis
     */
    Boolean ignore = false
    /** debug/tracking information on what (if any) string matched the ignore regex pattern */
    String matchedIgnoreText = null


    /**
     * placeholder 'empty' constructor -- probably should use a 'bigger' constructor
     */
    SavableObject() {
        log.warn "Empty constructor SavableObject()... (developer should use a better constructor"
    }


    /**
     * Simplest constructor -- assumes descendent objects will add all the real details (try to use 'bigger' constructor with more details
     * @param thing (file, folder, bookmark, url,...)
     */
    SavableObject(def thing) {
        this.thing = thing
        log.info "Simple constructor: SavableObject(thing) [${thing.class.name}:${thing}]"
    }


    /**
     * 'minimum' constructor with thing and source name, likely want to add a depth param...
     * @param thing - what is being created, e.g. (file, folder, bookmark, url,...)
     * @param locationName - name source, e.g. hostname for filesystem crawls, profile for browser history/bookmarks, email account...
     */
    SavableObject(def thing, String locationName, String crawlName = Constants.LBL_UNKNOWN, Integer depth = null) {
        this.thing = thing
        this.locationName = locationName
        this.crawlName = crawlName
        this.depth = depth
        log.debug "Typical constructor: SavableObject(thing, locationName, [crawlName], [depth]) -- ($thing, $locationName, $crawlName, $depth)"
    }


    /**
     * More common constrcutor - set the source thing, as well as depth (descendent object will add find & the other details
     * @param thing -(file, folder, bookmark, url,...)
     * @param locationName - name source, e.g. hostname for filesystem crawls, profile for browser history/bookmarks, email account...
     * @param parent - depth of thing relative to start (calling code will provide this)
     */
    SavableObject(def thing, SavableObject parent, String locationName) {
        this(thing, locationName)
        this.parent = parent
        this.depth = parent.depth + 1
        log.debug "Creating savable object thing: $thing"
    }


    /**
     * create a solr doc with the (most) common params, descendant object will add more details to @SolrInputDocument
     * @return SolrInputDocument
     */
    SolrInputDocument toSolrInputDocument(){
        SolrInputDocument sid = new SolrInputDocument()
        sid.setField(SolrSaver.FLD_ID, id)
        sid.setField(SolrSaver.FLD_TYPE, type)

        sid.setField(SolrSaver.FLD_NAME_S, name)
        sid.setField(SolrSaver.FLD_NAME_T, name)
        sid.setField(SolrSaver.FLD_PATH_S, path)
        sid.setField(SolrSaver.FLD_PATH_T, path)
        sid.setField(SolrSaver.FLD_SIZE, size)
        sid.setField(SolrSaver.FLD_DEPTH, depth)

        sid.setField(SolrSaver.FLD_LOCATION_NAME, locationName )
        if(this.crawlName) {
            sid.addField(SolrSaver.FLD_CRAWL_NAME, this.crawlName)
        } else {
            log.warn "No crawl name given: ${this}"
        }

        sid.setField(SolrSaver.FLD_CREATED_DATE, createdDate)

        // todo -- consider switching to a batch time, rather than creating a new timestamp for each doc
        sid.setField(SolrSaver.FLD_INDEX_DATETIME, new Date())
        if(lastModifiedDate) {
            sid.setField(SolrSaver.FLD_LAST_MODIFIED, lastModifiedDate)
        } else {
            log.warn "No modified date for savableObject: $this"
        }

        if(hidden)
            sid.setField(SolrSaver.FLD_HIDDEN_B, hidden)

//        sid.setField(SolrSaver.FLD_LAST_MODIFIED, lastModifiedDate)
//        if(labels)
            sid.setField(SolrSaver.FLD_LABELS, labels)

        if(tags)
            sid.setField(SolrSaver.FLD_TAG_SS, tags)

        if(dedup) {
            sid.setField(SolrSaver.FLD_DEDUP, dedup)
        } else {
            log.info "\t\tno dedup value set??? $this"
        }

//        sid.setField(SolrSaver.FLD_, )

        return sid
    }


    /** base/default combination to signal duplicate savable objects
     * override as needed to get better duplicate detection
     * @return string with groupable values that should flag duplicates (ala solr facet counts)
     */
    String buildDedupString(){
        String uniq = type + ':' + name + '::' + size
        return uniq
    }


    /**
     * compare the name of the thing to regex pattern of what to ignore,
     * @param fsFile Java
     * @param ignoreFiles
     * @return
     */
    String matchIgnorePattern(Pattern ignoreFiles){
        Matcher matcher = name =~ ignoreFiles
        if(matcher.matches()){
            String firstGroupMatch = matcher.group(1)
            matchedIgnoreText = firstGroupMatch        // todo - consider more complicated patterns, get all matches/groups??
            log.info "\t\tIGNORE: LocalFileSystemCrawler ignoring file (${this.class.simpleName}: $name) -- matched on: '$matchedIgnoreText' -- pattern ($ignoreFiles)"       // todo change back to debug
        } else {
            log.debug "\t\tnot ignoring: $name"
        }
        return matchedIgnoreText
    }

}
