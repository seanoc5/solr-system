package com.oconeco.setup

import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSystemClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

Logger log = LogManager.getLogger(this.class.name);
log.info "Starting script: ${this.class.name}..."

def collsNeeded = [
        Constants.DEFAULT_COLL_NAME,
        'system_analysis',
        'system_vocabulary',
        'system_concepts',
        'system_logmessages',
//        'system_',
]

SolrSystemClient client = new SolrSystemClient()
def collections = client.getCollections()

log.info "Done...?"
