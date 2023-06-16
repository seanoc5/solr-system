package com.oconeco.crawler

import com.oconeco.models.FSFolder
import com.oconeco.models.SavableObject
import com.oconeco.persistence.SolrSystemClient
import org.apache.log4j.Logger
import org.apache.solr.common.SolrDocument

/**
 * codify what qualifies as a difference worthy of 're-indexing'
 */
class DifferenceChecker {
    Logger log = Logger.getLogger(this.class.name);
    List<String> differences = []
    List<String> similarities = []


    DifferenceStatus compareFSFolderToSavedDocMap(FSFolder fsfolder, Map<String, SolrDocument> existingSolrDocMap) {
        DifferenceStatus differenceStatus = null
        if (fsfolder && existingSolrDocMap?.keySet()) {
            SolrDocument solrDocument = existingSolrDocMap.get(fsfolder.id)
            if (solrDocument) {
                log.info "Found existing solr doc to compare: $solrDocument"
            }
            differenceStatus = compareFSFolderToSavedDoc(fsfolder, solrDocument)

        } else {
            differenceStatus = new DifferenceStatus(fsfolder, null)

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

        return status
    }

    boolean shouldUpdateFolder(SavableObject fsfolder, SolrDocument existingSolrDoc) {
        DifferenceStatus status = compareFSFolderToSavedDoc(fsfolder, existingSolrDoc)
        boolean should = shouldUpdate(status)
        return should
    }

    boolean shouldUpdate(DifferenceStatus status) {
        boolean should = false
        if (status.differentIds) {
            // todo -- check logic and assumptions
            should = true
            log.debug "\t----different ids, flagging for update($should|'true'): $status"

        } else if (status.differentDedups) {
            should = true
            log.debug "\t\t----different dedups, flagging for update($should|'true'): $status"

        } else if (status.differentSizes) {
            should = true
            log.debug "\t\t----different sizes, flagging for update($should|'true'): $status"

        } else if (status.differentPaths) {
            should = true
            log.info "\t\t----different paths, flagging for update($should|'true'): $status"
        } else if (status.differentLastModifieds) {
            // todo -- check logic and assumptions
            should = true
            log.info "\t\t----different lastModifiedDate, FLAGGING for update($should|'true'): $status"


        } else if (status.differentSizes) {
            should = true
            log.debug "\t----different sizes, flagging for update($should|'true'): $status"

        } else if (status.differentLocations) {
            // todo -- check logic and assumptions
            should = false
            log.info "\t----different locations, BUT NOT CONSIDERED WORTHY of reindexing, NOT flagging for update($should|'false'): $status"
        }
        return should
    }
}
