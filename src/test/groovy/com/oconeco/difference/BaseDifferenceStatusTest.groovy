package com.oconeco.difference

import spock.lang.Specification

class BaseDifferenceStatusTest extends Specification {
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


    BaseDifferenceStatus statusMatching = new BaseDifferenceStatus(crawledGroupMatching, savedGroup)
    BaseDifferenceStatus statusDifferent = new BaseDifferenceStatus(crawledGroupMatching, savedGroup)

    def "IsSignificantlyDifferent"() {
        when:
        statusMatching.similarities = ['manufactured test', 'explicitly no differences', 'implicitly no comparisons performed in this unit test']
        statusMatching.significantlyDifferent = false

        then:
        statusMatching.significantlyDifferent == false
        statusMatching.differences.size() == 0
        statusMatching.similarities.size() == 3
    }

}
