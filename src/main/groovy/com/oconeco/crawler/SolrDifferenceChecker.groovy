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
class SolrDifferenceChecker extends BaseDifferenceChecker {
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
        log.debug "\t\t....call compareFSFolderToSavedDocMap($fsfolder, existingdocs size: ${existingSolrDocMap.size()})"
        DifferenceStatus differenceStatus = null
        if (fsfolder) {
            String fsId = null
            if (existingSolrDocMap?.size() > 0) {
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
            msg = "FSFolder ($fsfolder) is null -- this almost certainly will be a problem!!! [[existingSolrDoc:($existingSolrDoc)??]]"
            log.warn "$msg: folder:$fsfolder - existingSolrDoc: $existingSolrDoc"
            similarities << msg
        } else {
            if (!existingSolrDoc) {
                msg = "existingSolrDoc is null"
                log.debug "$msg: folder:$fsfolder"
                status.noMatchingSavedDoc = true
                status.significantlyDifferent = true        // todo -- revisit and see if this is muddled code... can/should we short circuit checking methods???
            } else {
                String fsId = fsfolder.id
                String solrId = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_ID)
                if (fsId == solrId) {
                    status.differentIds = false
                    status.similarities << "FSFolder id ($fsId) matches solr id($solrId"
                } else {
                    msg = "FSFolder id ($fsId) does not match solr id($solrId"
                    status.differences << msg
                    log.warn "\t\t>>>>$msg"
                    status.differentIds = true
                    status.significantlyDifferent = true        // todo -- revisit and see if this is muddled code... can/should we short circuit checking methods???

                }

                Date fsLastModified = fsfolder.lastModifiedDate
                Date solrLastModified = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_LAST_MODIFIED)
                if (fsLastModified == solrLastModified) {
                    msg = "FSFolder and Solr doc have SAME last modified date: $solrLastModified"
                    status.similarities << msg
                    status.differentLastModifieds = false
                    status.sourceIsNewer = false
                    log.debug "\t\t$msg"
                } else if (fsLastModified > solrLastModified) {
                    msg = "FSFolder (${fsfolder.path}) is newer ($fsLastModified) than solr folder ($solrLastModified)"
                    status.differences << msg
                    log.info "\t\t>>>>${msg}"
                    status.differentLastModifieds = true
                    status.sourceIsNewer = true
                    status.significantlyDifferent = true        // todo -- revisit and see if this is muddled code... can/should we short circuit checking methods???

                } else {
                    msg = "FSFolder (${fsfolder.path}) lastModified ($fsLastModified) is NOT the same date as solr folder ($solrLastModified), Solr is newer???"
                    status.differences << msg
                    log.warn "\t\t>>>> $msg (what logic is approapriate? currently we overwrite 'newer' solr/saved object with 'older' source object...???)"
                    status.differentLastModifieds = true
                    status.sourceIsNewer = false
                    status.significantlyDifferent = true        // is this true? should we overwrite a newer solr file with an older source object?

                }

                Long solrSize = (Long) existingSolrDoc.getFirstValue(SolrSystemClient.FLD_SIZE) ?: 0
                if (fsfolder.size == solrSize) {
                    msg = "Same sizes, fsfolder: ${fsfolder.size} != solr: $solrSize"
                    log.debug "\t\t${msg}"
                    status.similarities << msg
                    status.differentSizes = false
                } else {
                    status.differentSizes = true
                    msg = "Different sizes, fsfolder(${fsfolder.path}): ${fsfolder.size} != solr: $solrSize (${fsfolder.size - solrSize})"
                    log.debug "\t\t>>>>$msg"
                    status.differences << msg
                    status.significantlyDifferent = false        // todo -- revisit and see if this is muddled code... can/should we short circuit checking methods???
                }

                String solrDedup = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_DEDUP)
                if (fsfolder.dedup == solrDedup) {
                    msg = "Same dedup: ${fsfolder.dedup}"
                    log.debug "\t\t${msg}"
                    status.similarities << msg
                    status.differentDedups = false
                } else {
                    status.differentDedups = true
                    msg = "Different dedups (${fsfolder.path}), fsfolder: ${fsfolder.dedup} != solr: $solrDedup"
                    log.debug "\t\t>>>>$msg"
                    status.differences << msg
                    status.significantlyDifferent = true        // todo -- revisit and see if this is muddled code... can/should we short circuit checking methods???
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
                    log.warn "\t\t>>>>$msg (change this to info level once we are confident in logic)"
                    status.differences << msg
                    status.significantlyDifferent = true        // todo -- revisit and see if this is muddled code... can/should we short circuit checking methods???
                }

                String solrLocation = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_LOCATION_NAME)
                if (fsfolder.locationName == solrLocation) {
                    msg = "Same locationName: $solrLocation"
                    status.similarities << msg
                    log.debug "\t\t$msg"
                    status.differentLocations = false
                } else {
                    status.differentLocations = true
                    msg = "Different locations, fsfolder(${fsfolder.path}): ${fsfolder.locationName} != solr: $solrLocation (why do we have different locations!?!?)"
                    log.warn "\t\t$msg"
                    status.differences << msg
                    status.significantlyDifferent = true        // todo -- revisit and see if this is muddled code... can/should we short circuit checking methods???
                }
            }

        }
        return status
    }


}
