package com.oconeco

import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.LogManager


Logger log = LogManager.getLogger(this.class.name);

File srcList = new File('/home/sean/work/oconeco/solr_system/data/lucidworks.dirs.lst')
List<String> lines = srcList.readLines()

log.info "Lines: ${lines.size()}"

