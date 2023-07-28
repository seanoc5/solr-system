package com.oconeco.persistence

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger

/**
 * Help standardize and streamline exporting and importing of content.
 * Specifically should implement and use deep paging (cursor mark)
 * @see
 * <li> https://yonik.com/solr/paging-and-deep-paging/
 * <li> https://solr.apache.org/docs/9_0_0/core/org/apache/solr/search/CursorMark.html
 * <li> https://solr.apache.org/guide/solr/latest/query-guide/pagination-of-results.html#using-cursors
 * <li>
 */
class ExportImport {
    Logger log = LogManager.getLogger(this.class.name);
    SolrSystemClient client

    ExportImport(SolrSystemClient client) {
        this.client = client
    }

    def exportCollection(String collection, String q, int batchSize=1000){
//        if (client && client.)
    }
}
