package com.oconeco.models

import com.oconeco.analysis.BaseAnalyzer
import com.oconeco.persistence.SolrSaver
import org.apache.solr.common.SolrInputDocument

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

abstract class BaseObject {
    public static final String DEFAULT_TYPE = 'undefined'
    public static final String IGNORE_LABEL = 'ignore'
    /** the source thing (File, Folder,...) */
    def me
    /** absolute file path for files/folders, or other unique id for other descendants */
    def id
    /** label or name of me */
    String name
    /** object type label */
    String type
    /** size of me -- (ie count of files and subdirs in a folder */
    def size

    /** link to parent (if any) */
    def parent
    /** links to children (if any) */
    List children = []
    /** if hierarchacical (quite common) what is the depth here? */
    Long depth = 0

    /** rough metric for how recent this is (remove?) */
    def recency
    /** rough metric for how popular or well-used this dir is */
    def popularity

    /** type assigned programmatically (by this package's analysis code) */
    List<String> assignedTypes = []
    /** user tagging */
    List<String> taggedTypes = []
    /** machine learning clusters (unsupervised) */
    List<String> mlClusterType = []
    /** machine learning classifics (supervised) */
    List<String> mlClassificationType = []


    abstract analyze(BaseAnalyzer analyzer)

    SolrInputDocument toSolrInputDocument(){
        SolrInputDocument sid = new SolrInputDocument()
        sid.setField(SolrSaver.FLD_ID, id)
        sid.setField(SolrSaver.FLD_TYPE, type)
        sid.setField(SolrSaver.FLD_NAME_S, name)
        sid.setField(SolrSaver.FLD_NAME_T, name)
        sid.setField(SolrSaver.FLD_SIZE, size)
        sid.setField(SolrSaver.FLD_DEPTH, depth)
        sid.setField(SolrSaver.FLD_ASSIGNED_TYPES, assignedTypes)
        sid.setField(SolrSaver.FLD_TAG_SS, taggedTypes)

//        sid.addField(SolrSaver.FLD_, )
//        sid.addField(SolrSaver.FLD_, )
        return sid
    }

}
