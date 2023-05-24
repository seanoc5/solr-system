package com.oconeco.models

import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSaver
import org.apache.log4j.Logger
import org.apache.solr.common.SolrInputDocument

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
    /** object type label */
    String type
    /** if hierarchacical (quite common) what is the depth here? */
    Integer depth = null
    /** size of thing -- (ie count of files and subdirs in a folder */
    Long size
    /** name of 'source', where this came from, e.g.: hostname, browser profile, email account,.... */
    String locationName
    /** name of 'crawl', useful for segmenting the source into various logical sub-sections, e.g. email folders, or filesystem start folders with different content contexts */
    String crawlName

    /** date created  (i.e. how <em>old</em> is it?) */
    Date createdDate
    /** track updated/lastmodified date */
    Date lastModifiedDate

    /** link to parent (if any) */
    def parent
    /** links to children (if any) */
    List children
    /** rough metric for how recent this is (remove?) */
    def recency
    /** rough metric for how popular or well-used this dir is */
    def popularity

    /** type assigned programmatically (by this package's analysis code) */
    List<String> assignedTypes
    /** user tagging */
    List<String> taggedTypes
    /** machine learning clusters (unsupervised) */
    List<String> mlClusterType
    /** machine learning classifics (supervised) */
    List<String> mlClassificationType

    /** a string that should uniquely identify this thing, and hence, expose duplicate items */
    String uniquifier = null

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
     * @param depth - depth of thing relative to start (calling code will provide this)
     */
    SavableObject(def thing, String locationName, Integer depth) {
        this(thing, locationName)
        this.depth = depth
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
        sid.setField(SolrSaver.FLD_SIZE, size)
        sid.setField(SolrSaver.FLD_DEPTH, depth)
        sid.setField(SolrSaver.FLD_ASSIGNED_TYPES, assignedTypes)
        // todo -- consider switching to a batch time, rather than creating a new timestamp for each doc
        sid.setField(SolrSaver.FLD_INDEX_DATETIME, new Date())
        sid.setField(SolrSaver.FLD_LASTMODIFIED, lastModifiedDate)
        sid.setField(SolrSaver.FLD_UNIQUE, uniquifier)

        if(taggedTypes){
            sid.setField(SolrSaver.FLD_TAG_SS, taggedTypes)
        } else {
//            log.debug "no tagged types?? $sid"
        }

        if(this.crawlName) {
            sid.addField(SolrSaver.FLD_CRAWL_NAME, this.crawlName)
        } else {
            log.info "No crawl name given: ${this}"
        }
        return sid
    }

    String buildUniqueString(){
        String uniq = type + ':' + name + '::' + size
        return uniq
    }

}
