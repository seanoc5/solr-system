package com.oconeco.crawler


import com.oconeco.persistence.SolrSystemClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger

/**
 * codify what qualifies as a difference worthy of 're-indexing'
 */
class BaseDifferenceChecker {
    Logger log = LogManager.getLogger(this.class.name)
    String checkGroupFields
    String checkItemFields
//    Boolean shouldUpdatePersistence = null

    BaseDifferenceChecker() {
        this.checkGroupFields = SolrSystemClient.DEFAULT_FIELDS_TO_CHECK.join(' ')
        this.checkItemFields = SolrSystemClient.DEFAULT_FIELDS_TO_CHECK.join(' ')
    }

    /**
     * constructor to pass in what fields to check
     * todo -- revisit to check if this is actually useful/valid code approach
     * @param checkGroupFields
     * @param checkItemFields
     */
    BaseDifferenceChecker(String checkGroupFields, String checkItemFields) {
        this.checkGroupFields = checkGroupFields
        this.checkItemFields = checkItemFields
    }


    /**
     * check DifferenceStatus to determine if any existing differences are significant enough to require an update
     * @param status predefined record of differences (status) to check to determine if there are compelling differences
     * @return true if the caller should update the saved record (solr, ....)
     */
    boolean shouldUpdate(DifferenceStatus status) {
        boolean shouldUpdatePersistence = true
        String msg
        if (status) {
            if (status.noMatchingSavedDoc) {
                shouldUpdatePersistence = true
                msg = "no matching saved/solr doc, flagging for update"
//                status.differences << msg
                log.debug "\t\t--$msg"
            } else {
                if (status.differentIds) {
                    shouldUpdatePersistence = true
                    msg = "different ids, flagging for update (CAUTION: why would we ever compare different IDs???)"
                    log.warn "\t--$msg: $status"

                } else if (status.differentDedups) {
                    shouldUpdatePersistence = true
                    String objDedup = status.object.dedup
                    String solrDedup = status.solrDocument.getFirstValue(SolrSystemClient.FLD_DEDUP)
                    msg = "different dedups, flagging for update (FSFolder: ${objDedup}) != (solrdedup: $solrDedup)"
                    log.info "\t\t--$msg: $status"

                } else if (status.differentSizes) {
                    shouldUpdatePersistence = true
                    msg = "different sizes, flagging for update"
                    log.debug "\t\t----$msg: $status"

                } else if (status.differentPaths) {
                    shouldUpdatePersistence = true
                    msg = "different paths, flagging for update (how do we get different paths...?) "
                    log.warn "\t\t--: $status"

                } else if (status.differentLastModifieds) {
                    // todo -- check logic and assumptions
                    shouldUpdatePersistence = true
                    msg = "different lastModifiedDate, FLAGGING for update"
                    log.info "\t\t--$msg: $status"

                } else if (status.differentLocations) {
                    // todo -- check logic and assumptions
                    shouldUpdatePersistence = true
                    String objectLocation = status.object.locationName
                    String solrLocation = status.solrDocument.getFirstValue(SolrSystemClient.FLD_LOCATION_NAME)
//                    msg = "different locations, BUT NOT CONSIDERED WORTHY of reindexing, NOT flagging for update (fsfolder location: ${objectLocation}) != (solr doc location: ${solrLocation})}"
                    msg = "different locations, CONSIDERED WORTHY of reindexing (double check this logic), flagging for update (fsfolder location: ${objectLocation}) != (solr doc location: ${solrLocation})}"
                    log.warn "\t----$msg: $status"

                } else {
                    msg = "no differences found, flagging as NO update"
                    shouldUpdatePersistence = false
                }
            }
            if (!msg) {
                log.warn "\t\t....No message for status: $status???"
            }
        } else {
            log.warn "No valid DifferenceStatus, coding bug??!?!"
            shouldUpdatePersistence = true
        }

        return shouldUpdatePersistence
    }


    @Override
    public String toString() {
        return "BaseDifferenceChecker{" +
                ", checkGroupFields='" + checkGroupFields + '\'' +
                ", checkItemFields='" + checkItemFields + '\'' +
//                ", shouldUpdatePersistence=" + shouldUpdatePersistence +
                '}';
    }

}
