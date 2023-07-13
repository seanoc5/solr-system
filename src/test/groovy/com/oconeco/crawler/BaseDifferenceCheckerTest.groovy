package com.oconeco.crawler

import com.oconeco.difference.BaseDifferenceChecker
import com.oconeco.difference.BaseDifferenceStatus
import spock.lang.Specification

class BaseDifferenceCheckerTest extends Specification {
    String locationName = 'spock'
    String crawlName = 'test'
    Date matchingDate = new Date()
    String itemName = 'foo'
    String groupName = 'bar'
    Long matchingSize = 1234
    String itemDedup = "$locationName:$itemName:$matchingSize"
    String groupDedup = "$locationName:$groupName:$matchingSize"

    Map savedGroup = [id:groupName, name:groupName, type:"Group", lastModified:matchingDate, path:"/test/$groupName", size:matchingSize, dedup:groupDedup]
    Map savedItem = [id:itemName, name:itemName, type:"Item", lastModified:matchingDate, path:"/test/group1/item1", size:matchingSize, dedup:itemDedup]

    Map crawledGroupMatching = [id:groupName, name:groupName, type:"Group", lastModified:matchingDate, path:"/test/${groupName}", size:matchingSize, dedup:groupDedup]
    Map crawledItemMatching = [id:itemName, name:itemName, type:"Item", lastModified:matchingDate, path:"/test/group1/item1", size:matchingSize, dedup:itemDedup]

    Map crawledGroupDifferentName = [id:groupName, name:"${groupName}-1", type:"Group", lastModified:matchingDate, path:"/test/${groupName}-1", size:(matchingSize + 1000), dedup:groupDedup]
    Map crawledItemDifferentName = [id:"$itemName", name:"${itemName}-1", type:"Item", lastModified:matchingDate, path:"/test/group1/${itemName}-1", size:matchingSize, dedup:itemDedup]


    def "check when objects should be current"() {
        given:
        BaseDifferenceChecker differenceChecker = new BaseDifferenceChecker()

        when:
        BaseDifferenceStatus status = differenceChecker.compareCrawledGroupToSavedGroup(crawledGroupMatching, savedGroup)

        then:
        status != null
        status.differentIds == false
        status.differentDedups == false
        status.differentLastModifieds == false
        status.differentLocations == false
        status.differentPaths == false
        status.differentSizes == false
        differenceChecker.shouldUpdate(status) == false
    }


    def "check when objects are different"() {
        given:
         BaseDifferenceChecker differenceChecker = new BaseDifferenceChecker()

         when:
         BaseDifferenceStatus status = differenceChecker.compareCrawledGroupToSavedGroup(crawledGroupDifferentName, savedGroup)

         then:
         status != null
         status.differentIds == false
         status.differentDedups == false
         status.differentLastModifieds == false
         status.differentLocations == false
         status.differentPaths == false
         status.differentSizes == false
         differenceChecker.shouldUpdate(status) == false
    }
}
