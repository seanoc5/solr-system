package com.oconeco

import org.apache.log4j.Logger

Logger log = Logger.getLogger(this.class.name);

File srcList = new File('/home/sean/work/oconeco/solr_system/data/lucidworks.dirs.lst')
List<String> lines = srcList.readLines()

log.info "Lines: ${lines.size()}"

