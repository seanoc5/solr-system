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

    Map savedGroup = [id: groupName, name: groupName, type: "Group", lastModified: matchingDate, path: "/test/$groupName", size: matchingSize, dedup: groupDedup]
    Map savedItem = [id: itemName, name: itemName, type: "Item", lastModified: matchingDate, path: "/test/group1/item1", size: matchingSize, dedup: itemDedup]

    Map crawledGroupMatching = [id: groupName, name: groupName, type: "Group", lastModified: matchingDate, path: "/test/${groupName}", size: matchingSize, dedup: groupDedup]
    Map crawledItemMatching = [id: itemName, name: itemName, type: "Item", lastModified: matchingDate, path: "/test/group1/item1", size: matchingSize, dedup: itemDedup]

    Map crawledGroupDifferentName = [id: groupName, name: "${groupName}-1", type: "Group", lastModified: matchingDate, path: "/test/${groupName}-1", size: (matchingSize + 1000), dedup: groupDedup]
    Map crawledItemDifferentName = [id: "$itemName", name: "${itemName}-1", type: "Item", lastModified: matchingDate, path: "/test/group1/${itemName}-1", size: matchingSize, dedup: itemDedup]

    Map crawledGroupDifferentSize = [id: groupName, name: "${groupName}", type: "Group", lastModified: matchingDate, path: "/test/${groupName}", size: (matchingSize + 100), dedup: groupDedup]

    // basically checking date string->Date parsing, maybe add some other difference here?
    Map crawledGroupMinorDifferences = [id: groupName, name: "${groupName}", type: "Group", lastModified: matchingDate, path: "/test/${groupName}", size: "$matchingSize", dedup: groupDedup]
    Map crawledItemMinorDifferences = [id: "$itemName", name: "${itemName}", type: "Item", lastModified: "$matchingDate", path: "/test/group1/${itemName}", size: "$matchingSize", dedup: itemDedup]

    // todo -- test id differences? I don't thing we do yet, and probably should


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


    def "check when objects are different, but not needing update"() {
        given:
        BaseDifferenceChecker groupDifferenceChecker = new BaseDifferenceChecker()
//         BaseDifferenceChecker itemDifferenceChecker = new BaseDifferenceChecker()

        when:
        BaseDifferenceStatus groupStatus = groupDifferenceChecker.compareCrawledGroupToSavedGroup(crawledGroupMinorDifferences, savedGroup)
//         BaseDifferenceStatus itemStatus = groupDifferenceChecker.compareCrawledGroupToSavedGroup(crawledItemMinorDifferences, savedItem)

        then:
        groupStatus != null
        groupStatus.differentIds == false
        groupStatus.differentDedups == false
        groupStatus.differentLastModifieds == false
        groupStatus.differentLocations == false
        groupStatus.differentPaths == false
        groupStatus.differentSizes == false
        groupDifferenceChecker.shouldUpdate(groupStatus) == false
    }

    def "check when folders are different sizes, but not no size checking"() {
        given:
        BaseDifferenceChecker checkerNoSize = new BaseDifferenceChecker(false)
        BaseDifferenceChecker checkerWithSize = new BaseDifferenceChecker(true)

        when:
        BaseDifferenceStatus groupStatusNoSize = checkerNoSize.compareCrawledGroupToSavedGroup(crawledGroupDifferentSize, savedGroup)
        BaseDifferenceStatus groupStatusWithSize = checkerWithSize.compareCrawledGroupToSavedGroup(crawledGroupDifferentSize, savedGroup)

        then:
        groupStatusNoSize != null
        groupStatusNoSize.significantlyDifferent == false
        groupStatusWithSize != null
        groupStatusWithSize.significantlyDifferent == true
        groupStatusWithSize.differences.size() == 1
        groupStatusWithSize.differences[0].containsIgnoreCase('different sizes')
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
        status.differentPaths == true
        status.differentSizes == true
        differenceChecker.shouldUpdate(status) == true
    }
}
