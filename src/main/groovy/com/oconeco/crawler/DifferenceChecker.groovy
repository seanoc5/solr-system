package com.oconeco.crawler

import com.oconeco.models.SavableObject
import com.oconeco.persistence.SolrSaver
import org.apache.solr.common.SolrDocument

class DifferenceChecker {
    def folderNeedsUpdate(SavableObject fsfolder, SolrDocument existingSolrDoc){
        Date fsLastModified = fsfolder.lastModifiedDate
        def slm = existingSolrDoc.getFirstValue(SolrSaver.FLD_LAST_MODIFIED)

    }
}
