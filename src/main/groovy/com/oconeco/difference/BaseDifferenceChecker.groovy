package com.oconeco.difference


import com.oconeco.models.SavableObject
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
    // todo -- attempt to keep multithreaded?
//    SavableObject crawledObject
//    Object savedObject

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
    boolean shouldUpdate(SolrDifferenceStatus status) {
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
//                    String objDedup = status.object.dedup
//                    String solrDedup = status.savedDocument.getFirstValue(SolrSystemClient.FLD_DEDUP)
                    msg = "different dedups, flagging for update ($status.differences)"
                    // todo -- fixme refactor to better message
//                    msg = "different dedups, flagging for update (FSFolder: ${objDedup}) != (solrdedup: $solrDedup)"
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
//                    String objectLocation = status.object.locationName
//                    String solrLocation = status.savedDocument.getFirstValue(SolrSystemClient.FLD_LOCATION_NAME)
//                    msg = "different locations, BUT NOT CONSIDERED WORTHY of reindexing, NOT flagging for update (fsfolder location: ${objectLocation}) != (solr doc location: ${solrLocation})}"
                    msg = "different locations, CONSIDERED WORTHY of reindexing (double check this logic), status differences:(${status.differences})"
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

    /**
     * check groups to see if the is a change (and hence avoid checking each child object)
     * @param crawledGroup (i.e. folder/dir)
     * @param savedGroup - saved info from persistence later (i.e. solr document)
     * @return
     */
    BaseDifferenceStatus compareCrawledGroupToSavedGroup(SavableObject crawledGroup, Map<String, Object> savedGroup) {
        BaseDifferenceStatus status = new BaseDifferenceStatus(crawledGroup, savedGroup)
        String msg
        if (!crawledGroup) {
            msg = "GRoup Object ($crawledGroup) is null -- this almost certainly will be a problem!!! [[savedGroup:($savedGroup)??]]"
            log.warn "$msg: folder:$crawledGroup - savedGroup: $savedGroup"
            similarities << msg
        } else {
            if (!savedGroup) {
                msg = "savedGroup is null ($savedGroup)"
                log.debug "$msg: folder:$crawledGroup"
                status.noMatchingSavedDoc = true
                status.significantlyDifferent = true
                // todo -- revisit and see if this is muddled code... can/should we short circuit checking methods???
            } else {
                String crawledID = crawledGroup.id
                String savedId = getSavedId(savedGroup)
                if (crawledID == savedId) {
                    status.differentIds = false
                    status.similarities << "Crawled id ($crawledID) matches saved id($savedId)"
                } else {
                    msg = "crawledGroup id ($crawledID) does not match solr id($savedId"
                    status.differences << msg
                    log.warn "\t\t>>>>$msg"
                    status.differentIds = true
                    status.significantlyDifferent = true
                }

                Date fsLastModified = crawledGroup.lastModifiedDate
                Date solrLastModified = getSavedLastModifiedDate(savedGroup)
                if (fsLastModified == solrLastModified) {
                    msg = "crawledGroup and SaveObject doc have SAME last modified date: $solrLastModified"
                    status.similarities << msg
                    status.differentLastModifieds = false
                    status.sourceIsNewer = false
                    log.debug "\t\t$msg"
                } else if (fsLastModified > solrLastModified) {
                    msg = "crawledGroup (${crawledGroup.path}) is newer ($fsLastModified) than solr folder ($solrLastModified)"
                    status.differences << msg
                    log.info "\t\t>>>>${msg}"
                    status.differentLastModifieds = true
                    status.sourceIsNewer = true
                    status.significantlyDifferent = true
                } else {
                    msg = "crawledGroup (${crawledGroup.path}) lastModified ($fsLastModified) is NOT the same date as saved folder ($solrLastModified), SaveObject is newer???"
                    status.differences << msg
                    log.warn "\t\t>>>> $msg (what logic is approapriate? currently we overwrite 'newer' saved/saved object with 'older' source object...???)"
                    status.differentLastModifieds = true
                    status.sourceIsNewer = false
                    status.significantlyDifferent = true
                    // is this true? should we overwrite a newer saved file with an older source object?
                }

                Long savedSize = (Long) getSavedSize(savedGroup)
                if (crawledGroup.size == savedSize) {
                    msg = "Same sizes, fsfolder: ${crawledGroup.size} != saved: $savedSize"
                    log.debug "\t\t${msg}"
                    status.similarities << msg
                    status.differentSizes = false
                } else {
                    status.differentSizes = true
                    msg = "Different sizes, fsfolder(${crawledGroup.path}): ${crawledGroup.size} != saved: $savedSize (${crawledGroup.size - savedSize})"
                    log.debug "\t\t>>>>$msg"
                    status.differences << msg
                    status.significantlyDifferent = false
                }

                String savedDedup = getSavedDedup(savedGroup)
                if (crawledGroup.dedup == savedDedup) {
                    msg = "Same dedup: ${crawledGroup.dedup}"
                    log.debug "\t\t${msg}"
                    status.similarities << msg
                    status.differentDedups = false
                } else {
                    status.differentDedups = true
                    msg = "Different dedups (${crawledGroup.path}), fsfolder: ${crawledGroup.dedup} != saved: $savedDedup"
                    log.debug "\t\t>>>>$msg"
                    status.differences << msg
                    status.significantlyDifferent = true
                    // todo -- revisit and see if this is muddled code... can/should we short circuit checking methods???
                }

                String savedPath = getSavedPath(savedGroup)
                if (crawledGroup.path == savedPath) {
                    msg = "Paths are same: ${savedPath}"
                    status.similarities << msg
                    log.debug "\t\t$msg"
                    status.differentPaths = false
                } else {
                    status.differentPaths = true
                    msg = "Different paths, Crawled path: ${crawledGroup.path} != saved: $savedPath"
                    log.warn "\t\t>>>>$msg (change this to info level once we are confident in logic)"
                    status.differences << msg
                    status.significantlyDifferent = true
                }

                String solrLocation = getSavedLocation(savedGroup)
                if (crawledGroup.locationName == solrLocation) {
                    msg = "Same locationName: $solrLocation"
                    status.similarities << msg
                    log.debug "\t\t$msg"
                    status.differentLocations = false
                } else {
                    status.differentLocations = true
                    msg = "Different locations, fsfolder(${crawledGroup.path}): ${crawledGroup.locationName} != solr: $solrLocation (why do we have different locations!?!?)"
                    log.warn "\t\t$msg"
                    status.differences << msg
                    status.significantlyDifferent = true
                    // todo -- revisit and see if this is muddled code... can/should we short circuit checking methods???
                }
            }
        }
    }

    String getSavedLastModifiedDate(Object savedObj) {
        return savedObj.lastModifiedDate
    }

    String getSavedId(Object savedObj) {
        return savedObj.id
    }

/*
    String get(Object savedObj){
        return savedObj.
    }

    String get(Object savedObj){
        return savedObj.
    }

*/


    String getSavedPath(Object savedObj) {
        return savedObj.path
    }

    String getSavedDedup(Object savedObj) {
        return savedObj.dedup
    }

    Long getSavedSize(Object savedObj) {
        def size = savedObj.size
        if (size instanceof Long) {
            return size
        } else {
            try {
                Long s = new Long(size)
                return s
            } catch (Exception e) {
                log.warn "Casting exception: for obj($savedObj) getting 'Long' value from $size"
            }
        }
        return  null
    }

    String getSavedLocation(Object savedObj) {
        return savedObj.location
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
