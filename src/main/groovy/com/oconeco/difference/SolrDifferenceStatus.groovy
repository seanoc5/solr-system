//package com.oconeco.difference
//
//import com.oconeco.models.SavableObject
//import org.apache.logging.log4j.core.Logger
//import org.apache.logging.log4j.LogManager
//
///**
// * Small class to record differences
// */
//class BaseDifferenceStatus extends BaseDifferenceStatus {
//    Logger log = LogManager.getLogger(this.class.name);
//
//
//    BaseDifferenceStatus(SavableObject object, Object savedDoc) {
//        super(object, savedDoc)
//        //    BaseDifferenceStatus(SavableObject object, def solrDocument) {
//        if (solrDocument) {
//            this.savedDocument = solrDocument
//            noMatchingSavedDoc = false
//            String solrID = solrDocument.getFirstValue('id')
//            if (solrID) {
//                if (object.id == solrID) {
//                    similarities << "Object.id == solrDoc id ($solrID)"
//                } else {
//                    String msg = "Object.id (${object.id}) != solrDoc id ($solrID)"
//                    log.warn "$msg"
//                    differences << msg
//                    significantlyDifferent = true
//                }
//            } else {
//                log.warn "No solr id from solr Doc: $solrDocument, did the field list arg get borked in getSolrFolderDocs()??"
//            }
//
//        } else {
//            String msg = "no existing solr doc to compare to, assuming new Savable object"
//            log.debug "\t\t $msg: $object"
//            differences << msg
//            noMatchingSavedDoc = true
//        }
//
//    }
//
//}
