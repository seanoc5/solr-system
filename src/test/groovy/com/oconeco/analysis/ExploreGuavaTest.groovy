package com.oconeco.analysis

//import com.google.common.math.Quantiles
import spock.lang.Specification

/**
 * Quick unit tests to explore Google Guava functionality
 * https://guava.dev/releases/23.0/api/docs/com/google/common/math/Quantiles.html
 * https://en.wikipedia.org/wiki/Quantile
 */
class ExploreGuavaTest extends Specification {
    Integer[] indexes5 = [1, 2, 3, 4, 5]


    def "hello guava int quintiles"() {
        given:
        List percents = [1, 2, 2, 2, 2, 3, 4, 4, 4, 5, 6, 8, 9, 10, 11]

        when:
        Map<Integer, Double> quintiles = Quantiles.scale(5).indexes(indexes5).compute(percents)

        then:
        quintiles instanceof Map<Integer, Double>
        quintiles[1] == 2.0
        quintiles[1] == 2.0
        quintiles[2] == 3.6
        quintiles[3] == 4.4
        quintiles[4] == 8.2
    }

}
