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
    def folderNeedsUpdate(SavableObject fsfolder, SolrDocument existingSolrDoc){
        Date fsLastModified = fsfolder.lastModifiedDate
        def slm = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_LAST_MODIFIED)
    }

    /**
     *
     * @param fsfolder
     * @param existingSolrDoc
     */
    DifferenceStatus compareFSFolderToSavedDoc(FSFolder fsfolder, SolrDocument existingSolrDoc){
        DifferenceStatus status = new DifferenceStatus(fsfolder,existingSolrDoc)

        String fsId = fsfolder.id
        String solrId = existingSolrDoc.getFirstValue('id')
        if(fsId == solrId) {
            status.differentIds = false
        } else {
            log.warn "FSFolder id ($fsId) does not match solr id($solrId"
            status.differentIds = true
        }

        Date fsLastModified = fsfolder.lastModifiedDate
        Date solrLastModified = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_LAST_MODIFIED)
        if(fsLastModified == solrLastModified) {
            log.debug "FSFolder and Solr doc have same last modified date: $solrLastModified"
            status.differentLastModifieds = false
//            status.sourceIsNewer = false
        } else if(fsLastModified > solrLastModified){
            log.info "\t----FSFolder is newer ($fsLastModified) than solr folder ($solrLastModified)"
            status.differentLastModifieds = true
            status.sourceIsNewer = true
        } else {
            log.info "\t----FSFolder ($fsLastModified) is NOT the same date as solr folder ($solrLastModified), Solr is newer???"
            status.differentLastModifieds = true
            status.sourceIsNewer = false
        }

        Long solrSize = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_SIZE)
        if( fsfolder.size == solrSize) {
            status.differentSizes = false
        } else {
            status.differentSizes = true
            log.info "\t\tDifferent sizes, fsfolder: ${fsfolder.size} != solr: $solrSize"
        }

        String solrDedup = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_DEDUP)
        if( fsfolder.dedup == solrDedup) {
            status.differentDedups = false
        } else {
            status.differentDedups = true
            log.info "\t\tDifferent dedups, fsfolder: ${fsfolder.dedup} != solr: $solrDedup"
        }

        String solrPath = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_PATH_S)
        if( fsfolder.path == solrPath) {
            status.differentPaths = false
        } else {
            status.differentPaths = true
            log.info "\t\tDifferent paths, fsfolder: ${fsfolder.path} != solr: $solrPath"
        }

        String solrLocation = existingSolrDoc.getFirstValue(SolrSystemClient.FLD_LOCATION_NAME)
        if( fsfolder.locationName == solrLocation) {
            status.differentLocations = false
        } else {
            status.differentLocations = true
            log.info "\t\tDifferent locations, fsfolder: ${fsfolder.locationName} != solr: $solrLocation"
        }

        return status
    }

    boolean shouldUpdate(DifferenceStatus status){
        boolean should = false
        if(status.differentIds) {
            // todo -- check logic and assumptions
            should = true
            log.debug "\t----different ids, flagging for update($should|'true'): $status"

        } else if(status.differentDedups){
            should=true
            log.debug "\t----different dedups, flagging for update($should|'true'): $status"

        } else if(status.differentSizes) {
            should=true
            log.debug "\t----different sizes, flagging for update($should|'true'): $status"

        } else if(status.differentPaths) {
            should=true
            log.info "\t----different paths, flagging for update($should|'true'): $status"

        } else if(status.differentLocations) {
            // todo -- check logic and assumptions
            should=false
            log.info "\t----different locations, BUT NOT CONSIDERED WORTHY of reindexing, NOT flagging for update($should|'false'): $status"

//        } else if(status.differentSizes) {
//            should=true
//            log.debug "\t----different sizes, flagging for update($should|'true'): $status"
        }
        return should
    }
}
