package com.oconeco.crawler

import com.oconeco.models.FSFolder
import com.oconeco.persistence.SolrSystemClient
import org.apache.log4j.Logger
import org.apache.solr.common.SolrDocument

/**
 * codify what qualifies as a difference worthy of 're-indexing'
 */
class DifferenceChecker {
    Logger log = Logger.getLogger(this.class.name)
    List<String> differences = []
    List<String> similarities = []


    DifferenceStatus compareFSFolderToSavedDocMap(FSFolder fsfolder, Map<String, SolrDocument> existingSolrDocMap) {
        DifferenceStatus differenceStatus = null
        String fsId = null
        if (fsfolder && existingSolrDocMap?.size()>0) {
            fsId = fsfolder.id
            SolrDocument solrDocument = existingSolrDocMap.get(fsId)
            if (solrDocument) {
                log.debug "\t\tFound existing solr doc to compare, id:'${solrDocument.id}'"
                differenceStatus = compareFSFolderToSavedDoc(fsfolder, solrDocument)
            } else {
                log.info "\t\tNo existing solr doc to compare, id:'${fsId}'"
            }

        } else {
            differenceStatus = new DifferenceStatus(fsfolder, null)
        }
        // save the results to the actual savable object (this/FSFolder)
        fsfolder.differenceStatus = differenceStatus
        return differenceStatus
    }


    /**
     *
     * @param fsfolder
     * @param existingSolrDoc
     */
    DifferenceStatus compareFSFolderToSavedDoc(FSFolder fsfolder, SolrDocument existingSolrDoc) {
        DifferenceStatus status = new DifferenceStatus(fsfolder, existingSolrDoc)

        if (!fsfolder) {
            String msg = "FSFolder is null"
            log.warn "$msg: folder:$fsfolder - existingSolrDoc: $existingSolrDoc"
            similarities << msg
        } else if (!existingSolrDoc) {
            String msg = "existingSolrDoc is null"
            log.debug "$msg: folder:$fsfolder"
            status.noMatchingSavedDoc = true
        } else {
            String fsId = fsfolder.id
            String solrId = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_ID)
            if (fsId == solrId) {
                status.differentIds = false
                similarities << "FSFolder id ($fsId) matches solr id($solrId"
            } else {
                String msg = "FSFolder id ($fsId) does not match solr id($solrId"
                differences << msg
                log.warn msg
                status.differentIds = true
            }

            Date fsLastModified = fsfolder.lastModifiedDate
            Date solrLastModified = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_LAST_MODIFIED)
            if (fsLastModified == solrLastModified) {
                String msg = "FSFolder and Solr doc have SAME last modified date: $solrLastModified"
                similarities << msg
                status.differentLastModifieds = false
                log.debug msg
            } else if (fsLastModified > solrLastModified) {
                String msg = "FSFolder (${fsfolder.path}) is newer ($fsLastModified) than solr folder ($solrLastModified)"
                differences << msg
                log.info '\t\t' + msg
                status.differentLastModifieds = true
                status.sourceIsNewer = true
            } else {
                String msg = "FSFolder (${fsfolder.path}) lastModified ($fsLastModified) is NOT the same date as solr folder ($solrLastModified), Solr is newer???"
                differences << msg
                log.info "\t---- $msg"
                status.differentLastModifieds = true
                status.sourceIsNewer = false
            }

            Long solrSize = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_SIZE)
            if (fsfolder.size == solrSize) {
                String msg = "Same sizes, fsfolder: ${fsfolder.size} != solr: $solrSize"
                log.debug msg
                similarities << msg
                status.differentSizes = false
            } else {
                status.differentSizes = true
                String msg = "Different sizes, fsfolder(${fsfolder.path}): ${fsfolder.size} != solr: $solrSize"
                log.info "\t\t$msg"
                differences << msg
            }

            String solrDedup = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_DEDUP)
            if (fsfolder.dedup == solrDedup) {
                String msg = "Same dedup: ${fsfolder.dedup}"
                log.debug msg
                similarities << msg
                status.differentDedups = false
            } else {
                status.differentDedups = true
                String msg = "Different dedups (${fsfolder.path}), fsfolder: ${fsfolder.dedup} != solr: $solrDedup"
                log.info "\t\t$msg"
                differences << msg
            }

            String solrPath = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_PATH_S)
            if (fsfolder.path == solrPath) {
                String msg = "Paths are same: ${solrPath}"
                similarities << msg
                log.debug "\t\t$msg"
                status.differentPaths = false
            } else {
                status.differentPaths = true
                String msg = "Different paths, fsfolder: ${fsfolder.path} != solr: $solrPath"
                log.info "\t\t$msg"
                differences << msg
            }

            String solrLocation = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_LOCATION_NAME)
            if (fsfolder.locationName == solrLocation) {
                String msg = "Same locationName: $solrLocation"
                similarities << msg
                log.debug "\t\t$msg"
                status.differentLocations = false
            } else {
                status.differentLocations = true
                String msg = "Different locations, fsfolder(${fsfolder.path}): ${fsfolder.locationName} != solr: $solrLocation"
                log.info "\t\t$msg"
                differences << msg
            }
        }
        return status
    }

/*
    boolean shouldUpdateFolder(SavableObject fsfolder, SolrDocument existingSolrDoc) {
        DifferenceStatus status = compareFSFolderToSavedDoc(fsfolder, existingSolrDoc)
        boolean should = shouldUpdate(status)
        return should
    }
*/

    boolean shouldUpdate(DifferenceStatus status) {
        boolean should = false
        String msg
        if(status) {
            if (status.noMatchingSavedDoc) {
                should = true
                msg = "no matching saved/solr doc, flagging for update"
                log.debug "\t\t--$msg"
            } else {
                if (status.differentIds) {
                    // todo -- check logic and assumptions
                    msg = "different ids, flagging for update"
                    should = true
                    log.debug "\t--$msg: $status"

                } else if (status.differentDedups) {
                    should = true
                    msg = "different dedups, flagging for update"
                    log.debug "\t\t--$msg: $status"

                } else if (status.differentSizes) {
                    should = true
                    msg = "different sizes, flagging for update"
                    log.debug "\t\t----$msg: $status"

                } else if (status.differentPaths) {
                    should = true
                    msg = "different paths, flagging for update"
                    log.info "\t\t--: $status"
                } else if (status.differentLastModifieds) {
                    // todo -- check logic and assumptions
                    should = true
                    msg = "different lastModifiedDate, FLAGGING for update"
                    log.info "\t\t--$msg: $status"
                } else if (status.differentSizes) {
                    should = true
                    msg = "different sizes, flagging for update"
                    log.debug "\t----$msg: $status"

                } else if (status.differentLocations) {
                    // todo -- check logic and assumptions
                    should = false
                    msg = "different locations, BUT NOT CONSIDERED WORTHY of reindexing, NOT flagging for update"
                    log.info "\t----$msg: $status"

                } else {
                    msg = "no differences found, flagging as NO update"
                }
            }
            if (msg) {
                status.messages << msg
            } else {
                log.info "\t\t....No message for status: $status???"
            }
        } else {
            log.warn "No valid DifferenceStatus, coding bug??!?!"
            should = true
        }
        return should
    }
}
