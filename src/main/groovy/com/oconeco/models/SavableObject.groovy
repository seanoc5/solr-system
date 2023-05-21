package com.oconeco.models


import org.apache.log4j.Logger

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

class SavableObject {
    Logger log = Logger.getLogger(this.class.name);
    /** the source thing (File, Folder,...) */
    def thing
    /** absolute file path for files/folders, or other unique id for other descendants -- set by descendant object/implementation */
    String id
    /** label or name of thing */
    String name
    /** object type label */
    String type
    /** if hierarchacical (quite common) what is the depth here? */
    Integer depth = 0
    /** size of thing -- (ie count of files and subdirs in a folder */
//    def size
//    /** hostname, browser profile, email account,.... */
//    String sourceName
//    /** track updated/lastmodified date */
//    Date lastModifiedDate
//    /** link to parent (if any) */
//    def parent
//    /** links to children (if any) */
//    List children = []
//    /** rough metric for how recent this is (remove?) */
//    def recency
//    /** rough metric for how popular or well-used this dir is */
//    def popularity
//
//    /** type assigned programmatically (by this package's analysis code) */
//    List<String> assignedTypes = []
//    /** user tagging */
//    List<String> taggedTypes = []
//    /** machine learning clusters (unsupervised) */
//    List<String> mlClusterType = []
//    /** machine learning classifics (supervised) */
//    List<String> mlClassificationType = []

    SavableObject() {
        log.info "Empty constructor..."
    }
/**
     * Simplest constructor -- assumes descendent objects will add all the real details
     * @param thing (file, folder, bookmark, url,...)
     */
//    SavableObject(def thing) {
//        this.thing = thing
//    }

    SavableObject(File thing) {
        this.thing = thing
    }

//    SavableObject(def thing, String source) {
//        this.thing = thing
//        this.sourceName = source
//    }


    /**
     * More common constrcutor - set the source thing, as well as depth (descendent object will add find & the other details
     * @param thing -(file, folder, bookmark, url,...)
     * @param depth - depth of thing relative to start (calling code will provide this)
     */
//    SavableObject(def thing, String source, Integer depth) {
//        this.thing = thing
//        this.sourceName = source
//        this.depth = depth
//    }



/*
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

        if(taggedTypes){
            sid.setField(SolrSaver.FLD_TAG_SS, taggedTypes)
        } else {
//            log.debug "no tagged types?? $sid"
        }

//        sid.addField(SolrSaver.FLD_CRAWL_NAME, )
//        sid.addField(SolrSaver.FLD_, )
        return sid
    }
*/

}
