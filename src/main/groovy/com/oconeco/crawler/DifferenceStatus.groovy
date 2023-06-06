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

    DifferenceStatus(SavableObject object, SolrDocument solrDocument) {
        this.object = object
        this.solrDocument = solrDocument
    }
}
