package com.oconeco.models

import com.oconeco.crawler.DifferenceStatus
import com.oconeco.persistence.SolrSystemClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.solr.common.SolrInputDocument
import org.apache.tika.metadata.Metadata
/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

abstract class SavableObject {
    Logger log = LogManager.getLogger(this.class.name);
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
    Long size = 0
    /** name of 'source', where this came from, e.g.: hostname, browser profile, email account,.... */
    String locationName
    /** name of 'crawl', useful for segmenting the source into various logical sub-sections,
     * e.g. email folders, or filesystem start folders with different content contexts */
    String crawlName

    /** date created  (i.e. how <em>old</em> is it?) */
    Date createdDate
    /** track updated/lastmodified date */
    Date lastModifiedDate
    Date lastAccessDate

    String content = null
    Long contentSize = null

    boolean hidden = false

    /** link to parent (if any) */
    def parent
    String parentId

    /** links to children (if any) */
    List<SavableObject> children
/** track individual diference/update status */
    DifferenceStatus differenceStatus

    /** rough metric for how recent this is (remove?) */
    def recency
    /** rough metric for how popular or well-used this dir is */
    def popularity

    // todo -- look into using categories as well?
    /** type assigned programmatically (by this package's analysis code) */
    List<String> labels = []
    /** user tagging */
    List<String> tags = []
    /** machine learning clusters (unsupervised) */
    List<String> mlClusterType
    /** machine learning classifics (supervised) */
    List<String> mlClassificationType

    /** a string that should catch duplicate entries this thing, and hence */
    String dedup = null
    Metadata metadata

    // these may not be applicable for all, but perhaps enough to move into this base object
    String mimeType
    String owner

//    List<String> permissions
    Boolean archive = false
    Boolean compressed = false
    /** general flag on should the system ignore this object.
     * Note: crawler may still include object (and maybe even content) and save it,
     * but this flag can mean it should be filtered out of search & analysis
     */
    Boolean ignore = null
    /** discriminator between group (e.g. folder) or item */
    Boolean groupObject

    Map<String, Map<String, Object>> matchedLabels = [:]



    /**
     * More common constrcutor - set the source thing, as well as depth (descendent object will add find & the other details
     * @param thing -(file, folder, bookmark, url,...)
     * @param locationName - name source, e.g. hostname for filesystem crawls, profile for browser history/bookmarks, email account...
     * @param parent - depth of thing relative to start (calling code will provide this)
     */
    SavableObject(def thing, SavableObject parent, String locationName, String crawlName) {
        this.thing = thing
        if (parent) {
            this.parent = parent
            this.depth = parent.depth + 1
        } else {
            log.debug "no valid parent given: $thing"
        }
        this.locationName = locationName
        this.crawlName = crawlName
        log.debug "Creating savable object thing: $thing"
    }


    /**
     * create a solr doc with the (most) common params, descendant object will add more details to @SolrInputDocument
     * @return SolrInputDocument
     */
    SolrInputDocument toSolrInputDocument() {
        SolrInputDocument sid = new SolrInputDocument()
        sid.setField(SolrSystemClient.FLD_ID, id)
        sid.setField(SolrSystemClient.FLD_TYPE, type)

        sid.setField(SolrSystemClient.FLD_NAME_S, name)
        sid.setField(SolrSystemClient.FLD_NAME_T, name)
        sid.setField(SolrSystemClient.FLD_PATH_S, path)
        sid.setField(SolrSystemClient.FLD_PATH_T, path)
        sid.setField(SolrSystemClient.FLD_SIZE, size)
        sid.setField(SolrSystemClient.FLD_DEPTH, depth)

        sid.setField(SolrSystemClient.FLD_LOCATION_NAME, locationName)
        if (this.crawlName) {
            sid.addField(SolrSystemClient.FLD_CRAWL_NAME, this.crawlName)
        } else {
            log.warn "No crawl name given: ${this}"
        }

        sid.setField(SolrSystemClient.FLD_CREATED_DATE, createdDate)

        // todo -- consider switching to a batch time, rather than creating a new timestamp for each doc
        sid.setField(SolrSystemClient.FLD_INDEX_DATETIME, new Date())
        if (lastModifiedDate) {
            sid.setField(SolrSystemClient.FLD_LAST_MODIFIED, lastModifiedDate)
        } else {
            log.warn "No modified date for savableObject: $this"
        }

        if (hidden) {
            sid.setField(SolrSystemClient.FLD_HIDDEN_B, hidden)
        }

        sid.setField(SolrSystemClient.FLD_LABELS, labels)

        if (tags) {
            sid.setField(SolrSystemClient.FLD_TAG_SS, tags)
        }

        if (parentId) {
            sid.setField(SolrSystemClient.FLD_PARENT_ID, parentId)
        }

        if(content){
            sid.setField(SolrSystemClient.FLD_CONTENT_BODY, content)
            sid.setField(SolrSystemClient.FLD_CONTENT_BODY_SIZE, content.size())
        }
        if(metadata){
            sid.setField(SolrSystemClient.FLD_METADATA, metadata)
        }

        if (dedup) {
            log.debug "\t\tDedup already set: $dedup"
            sid.setField(SolrSystemClient.FLD_DEDUP, dedup)
        } else if (type && name && size != null) {
            dedup = buildDedupString()
            if (dedup) {
                sid.setField(SolrSystemClient.FLD_DEDUP, dedup)
                log.debug "\t\tDid not have a dedup ($dedup) -- now we do as we build solrInput doc: $this"
            } else {
                log.warn "\t\tno dedup value set??? $this"
            }
        } else {
            log.warn "\t\t.....Cannot reliably build a dedup string, missing type:$type, or name:$name, or size:$size"
        }

        if(matchedLabels){
            sid.setField(SolrSystemClient.FLD_MATCHED_LABELS, matchedLabels.toString())
        }

        return sid
    }


    /** base/default combination to signal duplicate savable objects
     * override as needed to get better duplicate detection
     * @return string with groupable values that should flag duplicates (ala solr facet counts)
     */
    String buildDedupString() {
        String prevDedup = dedup
        if(type && name && size != null) {
            log.debug "\t\tgood params for dedup: (type && name && size != null): (type:$type  name:$name  size:$size)"
        } else {
            log.warn "Something is null: (type && name && size == null): (type:$type  name:$name  size:$size)"
        }
        dedup = type + ':' + name + '::' + size
        if(prevDedup){
            if(!prevDedup.equalsIgnoreCase(dedup)) {
                log.info "\t\tAlready had a dedup:($prevDedup), it was DIFFERENT from new/current dedup ($dedup)?? "
            } else {
                log.debug "\t\tAlready had a dedup:($prevDedup), same as new/current dedup ($dedup)?? "
            }
        }
        return dedup
    }


    /**
     * Build a basic id (solr uniqify)
     * @param location name of source (hostname, external disk name, email account...)
     * @param path that uniquely identifies a given item in the given 'location' name
     * @return concatonated string uniquely identifying this item (duplicate ids overwrite/replace any previous Solr Doc item)
     */
    static String buildId(String location, String path) {
        String s = location + ':' + path
        return s
    }




    /**
     * custom string return value for this
     * @return string/label
     */
    String toString() {
        String s = null
        if (labels) {
            s = "${type}:(${name}) [depth:$depth] :: (${labels[0]})"
        } else if (ignore) {
            s = "${type}:(${name}) [depth:$depth] :: [IGNORE]"
        } else {
            s = "${type}:(${name}) [depth:$depth] "
        }

        return s
    }
}
