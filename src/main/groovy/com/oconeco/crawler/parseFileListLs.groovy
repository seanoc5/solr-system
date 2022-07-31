package com.oconeco.crawler

import org.apache.log4j.Logger

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Parse a file listing via `ls`, e.g. ls -alR / > ~/work/data/fileLists/mylist.lst
 */

Logger log = Logger.getLogger(this.class.name);

Map<String, Integer> months = [
        'Jan':1, 'Feb': 2, 'Mar':3, "Apr":4,
        'May':5, 'Jun':6, 'Jul':7, 'Aug':8,
        'Sep':9, 'Oct': 10, 'Nov':11, 'Dec':12]
File filelist = new File('/home/sean/work/data/fileLists/head.lst')

String directoryName = ''
Date myNow = new Date()
Integer thisMonth = myNow[Calendar.MONTH]


Pattern filePattern = Pattern.compile(/(?i)^([a-z-]+) +(\d+) +([^ ]+) +([^ ]+) +(\d+) +([0-9-]+) +([0-9:.]+) +([^ ]+)(.*)/)
//Pattern filePattern = Pattern.compile(/(?i)^([a-z-]+) +(\d+) +([^ ]+) +([^ ]+) +(\d+) +([a-z]{3}) +(\d+) +([\d:]+) +(.*)/)
//Pattern filePattern = Pattern.compile(/^([a-z-]+) +(\d+) +([^ ]+) +([^ ]+) +(\d+) +.*/)
Pattern dirNamePattern = Pattern.compile(/\/.*:/)
//Pattern filePattern = /([dlrwx-]{10}) (\d+) /

filelist.eachLine {String line ->
    if(line.startsWith('/')){
        directoryName = line[0..-2]
    } else if (line.startsWith('total')) {
        String cstr = line[6..-1]
        Integer count = new Integer(cstr)
        log.info "Directory($directoryName) count: $count"
    } else if(line.trim()==''){
        log.info "Skip blank line"
    } else {
        Matcher matcher = line =~ filePattern
        if(matcher.matches()) {
            String perms = matcher.group(1)
            String countStr = matcher.group(2)
            String owner = matcher.group(3)
            String group = matcher.group(4)
            String sizeStr = matcher.group(5)
            String fileDate = matcher.group(6)
            String fileTime = matcher.group(7)
            String timezone = matcher.group(8)

            String fileName = matcher.group(9)
            log.info "Match: $perms - $countStr - $owner - $group - $sizeStr - $fileDate"
        } else {
            log.warn "No match? $line -- $filePattern"
        }
    }
}
