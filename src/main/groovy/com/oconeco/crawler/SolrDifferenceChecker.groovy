package com.oconeco.crawler

import com.oconeco.models.FSFolder
import com.oconeco.persistence.BaseClient
import com.oconeco.persistence.SolrSystemClient
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.LogManager

import org.apache.solr.common.SolrDocument
/**
 * codify what qualifies as a difference worthy of 're-indexing'
 */
class SolrDifferenceChecker extends BaseDifferenceChecker{
    Logger log = LogManager.getLogger(this.class.name)
    String checkFolderFields = BaseClient.DEFAULT_FIELDS_TO_CHECK.join(' ')
    String checkFileFields = BaseClient.DEFAULT_FIELDS_TO_CHECK.join(' ')

    SolrDifferenceChecker() {
        super()
    }

    SolrDifferenceChecker(String checkFolderFields, String checkFileFields) {
        super(checkFolderFields, checkFileFields)
    }

    DifferenceStatus compareFSFolderToSavedDocMap(FSFolder fsfolder, Map<String, SolrDocument> existingSolrDocMap) {
        DifferenceStatus differenceStatus = null
        if (fsfolder) {
            String fsId = null
            if (fsfolder && existingSolrDocMap?.size() > 0) {
                fsId = fsfolder.id
                SolrDocument solrDocument = existingSolrDocMap.get(fsId)
                if (solrDocument) {
                    log.debug "\t\tFound existing solr doc to compare, id:'${solrDocument.id}'"
                    differenceStatus = compareFSFolderToSavedDoc(fsfolder, solrDocument)
                } else {
                    log.debug "\t\tNo existing solr doc to compare, id:'${fsId}'"
                    differenceStatus = new DifferenceStatus(fsfolder, null)
                }
            } else if (!existingSolrDocMap) {
                log.debug "\t\tNo existingSolrDocMap, assuming first time crawling this folder? $fsfolder"
                differenceStatus = new DifferenceStatus(fsfolder, null)
            }
            // save the results to the actual savable object (this/FSFolder)
            fsfolder.differenceStatus = differenceStatus
        } else {
            log.warn "No valid FSFolder($fsfolder) given as argument, this is likely a bug!!?! "
        }
        return differenceStatus
    }


    /**
     *
     * @param fsfolder
     * @param existingSolrDoc
     */
    DifferenceStatus compareFSFolderToSavedDoc(FSFolder fsfolder, SolrDocument existingSolrDoc) {
        DifferenceStatus status = new DifferenceStatus(fsfolder, existingSolrDoc)
        String msg
        if (!fsfolder) {
            msg = "FSFolder is null"
            log.warn "$msg: folder:$fsfolder - existingSolrDoc: $existingSolrDoc"
            similarities << msg
        } else if (!existingSolrDoc) {
             msg = "existingSolrDoc is null"
            log.debug "$msg: folder:$fsfolder"
            status.noMatchingSavedDoc = true
        } else {
            String fsId = fsfolder.id
            String solrId = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_ID)
            if (fsId == solrId) {
                status.differentIds = false
                status.similarities << "FSFolder id ($fsId) matches solr id($solrId"
            } else {
                msg = "FSFolder id ($fsId) does not match solr id($solrId"
                status.differences << msg
                log.warn msg
                status.differentIds = true
            }

            Date fsLastModified = fsfolder.lastModifiedDate
            Date solrLastModified = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_LAST_MODIFIED)
            if (fsLastModified == solrLastModified) {
                 msg = "FSFolder and Solr doc have SAME last modified date: $solrLastModified"
                status.similarities << msg
                status.differentLastModifieds = false
                status.sourceIsNewer = false
                log.debug msg
            } else if (fsLastModified > solrLastModified) {
                msg = "FSFolder (${fsfolder.path}) is newer ($fsLastModified) than solr folder ($solrLastModified)"
                status.differences << msg
                log.debug '\t\t' + msg
                status.differentLastModifieds = true
                status.sourceIsNewer = true
            } else {
                msg = "FSFolder (${fsfolder.path}) lastModified ($fsLastModified) is NOT the same date as solr folder ($solrLastModified), Solr is newer???"
                status.differences << msg
                log.info "\t---- $msg"
                status.differentLastModifieds = true
                status.sourceIsNewer = false
            }

            Long solrSize = (Long)existingSolrDoc.getFirstValue(SolrSystemClient.FLD_SIZE)
            if (fsfolder.size == solrSize) {
                msg = "Same sizes, fsfolder: ${fsfolder.size} != solr: $solrSize"
                log.debug msg
                status.similarities << msg
                status.differentSizes = false
            } else {
                status.differentSizes = true
                msg = "Different sizes, fsfolder(${fsfolder.path}): ${fsfolder.size} != solr: $solrSize (${fsfolder.size - solrSize})"
                log.debug "\t\t$msg"
                status.differences << msg
            }

            String solrDedup = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_DEDUP)
            if (fsfolder.dedup == solrDedup) {
                msg = "Same dedup: ${fsfolder.dedup}"
                log.debug msg
                status.similarities << msg
                status.differentDedups = false
            } else {
                status.differentDedups = true
                msg = "Different dedups (${fsfolder.path}), fsfolder: ${fsfolder.dedup} != solr: $solrDedup"
                log.debug "\t\t$msg"
                status.differences << msg
            }

            String solrPath = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_PATH_S)
            if (fsfolder.path == solrPath) {
                msg = "Paths are same: ${solrPath}"
                status.similarities << msg
                log.debug "\t\t$msg"
                status.differentPaths = false
            } else {
                status.differentPaths = true
                msg = "Different paths, fsfolder: ${fsfolder.path} != solr: $solrPath"
                log.info "\t\t$msg"
                status.differences << msg
            }

            String solrLocation = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_LOCATION_NAME)
            if (fsfolder.locationName == solrLocation) {
                msg = "Same locationName: $solrLocation"
                status.similarities << msg
                log.debug "\t\t$msg"
                status.differentLocations = false
            } else {
                status.differentLocations = true
                msg = "Different locations, fsfolder(${fsfolder.path}): ${fsfolder.locationName} != solr: $solrLocation"
                log.info "\t\t$msg"
                status.differences << msg
            }
        }
        return status
    }



    boolean shouldUpdate(DifferenceStatus status) {
        boolean should = false
        String msg
        if (status) {
            if (status.noMatchingSavedDoc) {
                should = true
                msg = "no matching saved/solr doc, flagging for update"
//                status.differences << msg
                log.debug "\t\t--$msg"
            } else {
                if (status.differentIds) {
                    // todo -- check logic and assumptions
                    msg = "different ids, flagging for update"
                    should = true
                    log.debug "\t--$msg: $status"
//                    status.differences << msg

                } else if (status.differentDedups) {
                    should = true
                    String objDedup = status.object.dedup
                    String solrDedup = status.solrDocument.getFirstValue(SolrSystemClient.FLD_DEDUP)
                    msg = "different dedups, flagging for update (FSFolder: ${objDedup}) != (solrdedup: $solrDedup)"
                    log.debug "\t\t--$msg: $status"
//                    status.differences << msg

                } else if (status.differentSizes) {
                    should = true
                    msg = "different sizes, flagging for update"
                    log.debug "\t\t----$msg: $status"
//                    status.differences << msg

                } else if (status.differentPaths) {
                    should = true
                    msg = "different paths, flagging for update (how do we get different paths...?) "
                    log.warn "\t\t--: $status"
//                    status.differences << msg
                } else if (status.differentLastModifieds) {
                    // todo -- check logic and assumptions
                    should = true
                    msg = "different lastModifiedDate, FLAGGING for update"
                    log.info "\t\t--$msg: $status"
//                    status.differences << msg
                } else if (status.differentSizes) {
                    should = true
                    msg = "different sizes, flagging for update"
                    log.debug "\t----$msg: $status"
//                    status.differences << msg

                } else if (status.differentLocations) {
                    // todo -- check logic and assumptions
                    should = false
                    String objectLocation = status.object.locationName
                    String solrLocation = status.solrDocument.getFirstValue(SolrSystemClient.FLD_LOCATION_NAME)
                    msg = "different locations, BUT NOT CONSIDERED WORTHY of reindexing, NOT flagging for update (fsfolder location: ${objectLocation}) != (solr doc location: ${solrLocation})}"
                    log.info "\t----$msg: $status"
//                    status.differences << msg

                } else {
                    msg = "no differences found, flagging as NO update"
//                    status.similarities << msg
                }
            }
            if (!msg) {
                log.warn "\t\t....No message for status: $status???"
            }
        } else {
            log.warn "No valid DifferenceStatus, coding bug??!?!"
            should = true
        }
        return should
    }
}
