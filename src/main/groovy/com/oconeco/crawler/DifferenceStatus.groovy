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
    List<String> messages = []

    DifferenceStatus(SavableObject object, SolrDocument solrDocument) {
        this.object = object
        if(solrDocument) {
            this.solrDocument = solrDocument
            noMatchingSavedDoc = false
        } else {
            String msg = "no existing solr doc to compare to, assuming new Savable object"
            log.debug "\t\t $msg: $object"
            messages << msg
            noMatchingSavedDoc = true
        }
    }

    String toString() {
        String s = "Diff Status:($object) $messages"
    }
}
