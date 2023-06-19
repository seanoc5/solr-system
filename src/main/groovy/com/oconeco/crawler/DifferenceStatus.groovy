package com.oconeco.crawler

import com.oconeco.models.SavableObject
import org.apache.log4j.Logger
import org.apache.solr.common.SolrDocument

/**
 * Small class to record differences
 */
class DifferenceStatus {
    Logger log = Logger.getLogger(this.class.name);
    SavableObject object
    SolrDocument solrDocument

    Boolean differentIds
    Boolean differentLastModifieds
    Boolean differentSizes
    Boolean differentDedups
    Boolean differentPaths
    Boolean differentLocations

    Boolean sourceIsNewer
    Boolean noMatchingSavedDoc
//    List<String> messages = []
    List<String> differences = []
    List<String> similarities = []
    boolean significantlyDifferent = false

    DifferenceStatus(SavableObject object, SolrDocument solrDocument) {
        this.object = object
        if(solrDocument) {
            this.solrDocument = solrDocument
            noMatchingSavedDoc = false
            String solrID = solrDocument.getFirstValue('id')
            if(solrID) {
                if (object.id == solrID) {
                    similarities << "Object.id == solrDoc id ($solrID)"
                } else {
                    String msg = "Object.id (${object.id}) != solrDoc id ($solrID)"
                    log.warn "$msg"
                    differences << msg
                    significantlyDifferent = true
                }
            } else {
                log.warn "No solr id from solr Doc: $solrDocument, did the field list arg get borked in getSolrFolderDocs()??"
            }

        } else {
            String msg = "no existing solr doc to compare to, assuming new Savable object"
            log.debug "\t\t $msg: $object"
            differences << msg
            noMatchingSavedDoc = true
        }
    }

    String toString() {
        String s = "Diff Status:($object) "
        if(similarities){
            s=s+ " Similarities:${similarities} "
        }
        if(differences){
            s = s+ " Differences: ${differences} "
        }
        return s
    }
}
