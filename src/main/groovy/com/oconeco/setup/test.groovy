package com.oconeco.setup

import com.oconeco.persistence.SolrSystemClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger


Logger log = LogManager.getLogger(this.class.name);
log.info "Starting script: ${this.class.name}..."

SolrSystemClient client = new SolrSystemClient()


log.info "Done...?"
