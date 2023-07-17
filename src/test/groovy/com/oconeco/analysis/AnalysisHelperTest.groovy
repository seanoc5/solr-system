//package com.oconeco.analysis
//
//import org.apache.lucene.analysis.Analyzer
//import org.apache.lucene.analysis.standard.StandardAnalyzer
//import spock.lang.Specification
//
//class AnalysisHelperTest extends Specification {
////    TokenizerHelper helper = new TokenizerHelper()
//    String text1 = "This is one(1) simple sentence to analyze."
//    String text2 = "A second sentence--more involved--to analyze; adding complexity and 20220101 date and www.host.com url."
//    String textRepetitive = 'five five four five four three five four three two five four three two one'
//    List<String> skipWords = ['a', 'and', 'is', 'this', 'to']
//
//    Map<String, Integer> termFreqsOrderedTen = ['one': 1, 'two': 2, 'three': 3, 'four': 4, 'five': 5,
//                                                six  : 6, seven: 7, eight: 8, nine: 9, ten: 10]
//    Map<String, Integer> termFreqsRandomfifteen = [six   : 6, seven: 7, eight: 8, nine: 9, ten: 10,
//                                                   eleven: 11, twelve: 12, thirteen: 13, fourteen: 14, fifteen: 15,
//                                                   'one' : 1, 'two': 2, 'three': 3, 'four': 4, 'five': 5]
//
//
//    def "Tokenize basic text directly"() {
//        given:
//        Analyzer standardAnalyzer = new StandardAnalyzer()
//
//        when:
//        def terms1 = AnalysisHelper.tokenize(text1)
//        def terms2 = AnalysisHelper.tokenize(text2)
//
//        then:
//        terms1.size() == 8
//        terms1[3] == '1'
//
//        terms2.size() == 15
//        terms2[10] == '20220101'
//
//    }
//
//    def "compare whitespace analysis to standard analysis"() {
//
//        when:
//        def termsWS = AnalysisHelper.tokenizeWhiteSpace(text2)
//        def termsStd = AnalysisHelper.tokenizeStandard(text2)
//
//        then:
//        termsWS.size() == 13
//        termsWS[2] == 'sentence--more'
//        termsWS[4] == 'analyze;'
//
//        termsStd.size() == 15
//        termsStd[2] == 'sentence'
//        termsStd[3] == 'more'
//    }
//
//    def "compare terms in two texts"() {
//        given:
////        StandardAnalyzer standardAnalyzerWithSkip = new StandardAnalyzer()
//
//        when:
//        def resultsBasic = AnalysisHelper.compareTextTerms(text1, text2)
//        def resultsWithSkip = AnalysisHelper.compareTextTerms(text1, text2, skipWords)
//
//        then:
//        resultsBasic.leftOnly.size() == 5
//        resultsBasic.leftOnly.containsAll(['this', 'is', 'one', 'simple', '1'])
//        resultsBasic.rightOnly.size() == 12
//        resultsBasic.rightOnly.containsAll(['a', 'second', 'more', 'involved', 'complexity', 'adding'])
//        resultsBasic.sharedTerms.size() == 3
//        resultsBasic.sharedTerms.containsAll(['sentence', 'to', 'analyze',])
//
//        resultsWithSkip.leftOnly.size() == 3
//        resultsWithSkip.leftOnly.containsAll(['one', 'simple', '1'])
//        resultsWithSkip.rightOnly.size() == 9
//        resultsWithSkip.rightOnly.containsAll(['second', 'more', 'involved', 'adding', 'complexity'])
//        resultsWithSkip.sharedTerms.size() == 2
//        resultsWithSkip.sharedTerms.containsAll(['sentence', 'analyze',])
//    }
//
//    def "gather term frequencies"() {
//        when:
//        Map<String, Integer> freqs1 = AnalysisHelper.gatherTermFrequencies(text1)
//        def freqs1Multiple = freqs1.findAll { String t, Integer i ->
//            i > 1
//        }
//        Map<String, Integer> freqs2 = AnalysisHelper.gatherTermFrequencies(text2)
//        def freqs2Multiple = freqs2.findAll { String t, Integer i -> i > 1 }
//        Map<String, Integer> freqsRepetitive = AnalysisHelper.gatherTermFrequencies(textRepetitive)
//        def freqsRepetitiveMultiple = freqsRepetitive.findAll { String t, Integer i -> i > 1 }
//
//        then:
//        freqs1.size() == 8
//        freqs1Multiple.size() == 0
//
//        freqs2.size() == 14
//        freqs2Multiple.size() == 1
//
//        freqsRepetitive.size() == 5
//        freqsRepetitiveMultiple.size() == 4
//    }
//
//    def "gather term percents"() {
//        when:
//        def termPercentages = AnalysisHelper.gatherTermPercentages(textRepetitive)
//        def five = termPercentages['five']
//
//        then:
//        termPercentages instanceof Map
//        termPercentages.size() == 5
//        five.percent.toInteger() == 33
//    }
//
///*
//    def "compare term percents"() {
//        given:
//        String baseText = "one two three two three three BaseOnly"
//        String testText = "one two three testOnly"
//        def basetermPercentages = AnalysisHelper.gatherTermPercentages(baseText)
//        def testtermPercentages = AnalysisHelper.gatherTermPercentages(testText)
//
//        when:
//        def comparison = AnalysisHelper.compareTermPercentages(testtermPercentages, basetermPercentages, 1)
//
//        then:
//        basetermPercentages instanceof Map
//        basetermPercentages.size() == 4
////        five.percent == 33
//    }
//*/
//
//    // fubar...? not sure I understand this test anymore...
///*
//    def "bucketize termFreqs"() {
//        when:
//        Map<Integer, List> bucketsTen = AnalysisHelper.bucketizeTermFrequencies(termFreqsOrderedTen, 5)
//        Map<Integer, List> bucketsFifteen = AnalysisHelper.bucketizeTermFrequencies(termFreqsRandomfifteen)
//
//        then:
//        bucketsTen instanceof Map
//        bucketsTen.size() == 5
//        bucketsTen[1].containsAll(['one', 'two'])
//        bucketsTen[5].containsAll(['nine', 'ten'])
//
//        bucketsFifteen.size() == 5
//        bucketsFifteen[1].size()==3
//        bucketsFifteen[1].containsAll(['one','two','three'])
//        bucketsFifteen[5].containsAll(['thirteen','fourteen','fifteen'])
//    }
//*/
//
//
//    def "analyze arbitrary text content -- jsp licence terms"(){
//        given:
//        String jspText = getClass().getResource('/analysis/jsp-api-license.txt').text
//        def jspTokens = AnalysisHelper.tokenize(jspText)
//
//        when:
//        Map jspTermFrequencies = AnalysisHelper.gatherTermFrequencies(jspTokens)
//
////        Map<Integer, List> bucketsJsp = AnalysisHelper.bucketizeTermFrequencies(jspTermFrequencies)
//        Map<String, Map<String, Number>> termsWthBuckets = AnalysisHelper.addTermFreqBuckets(jspTermFrequencies)
//        Map groupedTerms = termsWthBuckets.groupBy {it.value.bucket}
//        // sanity check that our count is the same
//        int termCount = termsWthBuckets.collect {String t, Map map -> map.frequency }.sum()
//
//        then:
//        groupedTerms.size()==5
//        termCount==2709 || termCount==2710      // something weird between windows and linux
//
//    }
//
//    /**
//     * load two different text files (both licenses) and look for similarities and outliers in term counts
//     * @return
//     */
//    def "analyze arbitrary text content -- two java licences"(){
//        given:
//        String actText = getClass().getResource('/analysis/activation-license.txt').text
//        def actTokens = AnalysisHelper.tokenize(actText)
//        String jspText = getClass().getResource('/analysis/jsp-api-license.txt').text
//        def jspTokens = AnalysisHelper.tokenize(jspText)
//
//        when:
//        Map actTermFrequencies = AnalysisHelper.gatherTermFrequencies(actTokens)
//        Map jspTermFrequencies = AnalysisHelper.gatherTermFrequencies(jspTokens)
//
//        Map<String, Map<String, Number>> actTermsWthBuckets = AnalysisHelper.addTermFreqBuckets(actTermFrequencies)
////        Map<Integer, List> bucketsAct = AnalysisHelper.bucketizeTermFrequencies(actTermFrequencies)
//
//        Map<String, Map<String, Number>> jspTermsWthBuckets = AnalysisHelper.addTermFreqBuckets(jspTermFrequencies)
////        Map<Integer, List> bucketsJsp = AnalysisHelper.bucketizeTermFrequencies(jspTermFrequencies)
//        Map actOutliers = [:]
//        actTermsWthBuckets.each {String term, Map<String, Number> freqMap ->
//            int actBucket = freqMap.bucket
//            def jspMap = jspTermsWthBuckets[term]
//            if(jspMap) {
//                int jspBucket = jspMap.bucket
//                int diff = actBucket - jspBucket
//                if(diff > 0) {
//                    if (Math.abs(diff) >= 2) {
//                        println "Significant differece: $diff (act:$actBucket) - (jsp:$jspBucket)"
//                    }
//                }
//            } else {
//                println("Act term: $term (freq map: $freqMap) but no match in jsp map...")
//            }
//        }
//
//        then:
////        bucketsAct.size()==5        // todo -- revisit decent test criteria... out of sync, and I've forgotten the goal here...
////        bucketsJsp.size()==5
//        actTermFrequencies.size()==573
//        jspTermFrequencies.size()==581  || jspTermFrequencies.size()==582        // something weird between windows and linux
//        // todo -- review this code, and make it right...
////        "this test code is complete" == 'current status'
//
//    }
//
//}
