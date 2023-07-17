//package com.oconeco.analysis
//
//import com.google.common.math.Quantiles
//import org.apache.logging.log4j.LogManager
//import org.apache.logging.log4j.core.Logger
//import org.apache.lucene.analysis.Analyzer
//import org.apache.lucene.analysis.TokenStream
//import org.apache.lucene.analysis.core.WhitespaceAnalyzer
//import org.apache.lucene.analysis.standard.StandardAnalyzer
//import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
//
///**
// * WIP for class to help do analysis things:
// * looking at terms, patterns, statistics...
// *
// * quite likely there are better existing libraries/solutions, but this is the developer thinking through options...
// */
//class AnalysisHelper {
//    static final Logger log = LogManager.getLogger(this.class.name)
//    /**
//     * 0 percent differnce indicates any terms that were filtered out for minimum frequency, 100 means any that occured in one but not the other
//     */
//    static Map DFLT_DIFF_RANGE = [zero: 0, similar: 10, moderate: 30, strong: 60, veryStrong: 80, unmatched: 100]
//
//    public static List<String> tokenize(String text, List<String> skipWords = [], Analyzer analyzer = new StandardAnalyzer()) throws IOException {
//        List<String> result = new ArrayList<String>();
//        TokenStream tokenStream = null
//        try {
//            tokenStream = analyzer.tokenStream('noField', text);
//            CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
//            tokenStream.reset();
//            while (tokenStream.incrementToken()) {
//                String t = attr.toString()
//                if (skipWords.contains(t)) {
//                    log.debug "Skipping term: $t"
//                } else {
//                    log.debug "Adding term: $t"
//                    result.add(attr.toString());
//                }
//            }
//        } finally {
//            tokenStream.close()
//        }
//        return result;
//    }
//
//
//    /**
//     * Convenience method to call tokenize with Whitespace tokenizer
//     * @param text to tokenize
//     * @return lits of strings of tokens, there should be no lowercasing or other analysis with white space tokenizing
//     */
//    static List<String> tokenizeWhiteSpace(String text, List<String> skipwords = []) {
//        List<String> result = tokenize(text, skipwords, new WhitespaceAnalyzer());
//        return result
//    }
//
//
//    static List<String> tokenizeStandard(String text, List<String> skipwords = []) {
//        List<String> result = tokenize(text, skipwords, new StandardAnalyzer());
//        return result
//    }
//
//
//    /**
//     * Parse both left and right text with the analyzer provided
//     * Note: stopwords are set in the analyzer (from the calling method)
//     * @param leftText
//     * @param rightText
//     * @param analyzer
//     * @return map of left, right, and shared terms
//     */
//    static Map<String, List<String>> compareTextTerms(String leftText, String rightText, List<String> skipwords = [], Analyzer analyzer = new StandardAnalyzer()) {
//        List<String> leftTerms = tokenize(leftText, skipwords, analyzer)
//        List<String> rightTerms = tokenize(rightText, skipwords, analyzer)
//        Map<String, List<String>> results = compareTermLists(leftTerms, rightTerms)
//        return results
//    }
//
//
//    /**
//     * process already analyzed/tokenized list of terms
//     * @param leftTerms
//     * @param rightTerms
//     * @param skipTerms - optional (additional?) list of skip or stop words
//     * @return
//     */
//    static Map<String, List<String>> compareTermLists(List<String> leftTerms, List<String> rightTerms, List<String> skipTerms = []) {
//        List<String> leftOnly = leftTerms - rightTerms
//        List<String> rightOnly = rightTerms - leftTerms
//        List<String> shared = rightTerms.intersect(leftTerms)
//
//        Map results = [leftOnly: (leftOnly - skipTerms), rightOnly: (rightOnly - skipTerms), sharedTerms: (shared - skipTerms)]
//        return results
//    }
//
//
//    static Map<String, Integer> gatherTermFrequencies(String text, List<String> skipwords = []) {
//        List terms = tokenize(text, skipwords)
//        def frequencies = gatherTermFrequencies(terms)
//    }
//
//
//    static Map<String, Integer> gatherTermFrequencies(List<String> terms) {
//        Map<String, Integer> termFrequencies = [:].withDefault { 0 }
//        terms.each {
//            termFrequencies[it]++
//        }
//        return termFrequencies
//    }
//
//
//    static Map<String, Map<String, Double>> gatherTermPercentages(String text, List<String> skipwords = [], Analyzer analyzer = new StandardAnalyzer()) {
//        List<String> terms = tokenize(text, skipwords, analyzer)
//        Map<String, Integer> percents = gatherTermPercentages(terms)
//    }
//
//
//    static Map<String, Map<String, Double>> gatherTermPercentages(List<String> terms) {
////    static Map<String, Map<String, Integer>> gatherTermPercentages(List<String> terms) {
//        int totalCount = terms.size()
//        Map<String, Integer> termFrequencies = gatherTermFrequencies(terms)
//        Map<String, Integer> termPercents = termFrequencies.collectEntries { String term, Integer count ->
//            Double percent = (count / totalCount) * 100        // deal with integers for ease, lack of rounding, don't need precision of floats
//            return [term, [percent: percent, count: count]]
//        }
//        return termPercents
//    }
//
//    /**
//     * deprecated -- this is not a useful thing... I think...?
//     *
//     * compare two Maps of terms and percents, return differences in percents: testFreq - baseFreq
//     * For example: testFreq = 80% and baseFreq = 20% -> 60% difference (positive value shows test has more)
//     *
//     * @param testTermPercents map of term:frequencyPercent (integer 0-100) that we are comparing against a base (more general, more broad)) set of term percents
//     * @param baseTermPercents map of term:frequencyPercent (integer 0-100) which acts as a basis to compare against (i.e. "typical" frequencies
//     * @param minFrequency
//     * @param differenceRanges
//     * @return Map of term and integer of difference in percent of testPercent - basePercent
//     */
///*
//    static Map<String, Map<String, Object>> compareTermPercentages(Map<String, Map<String, Integer>> testTermPercents, Map<String, Map<String, Integer>> baseTermPercents, int minFrequency = 1, Map differenceRanges = DFLT_DIFF_RANGE) {
//        Map<String, Map<String, Object>> termDifferences = testTermPercents.collect { String testTerm, Map<String, Integer> testCountMap ->
//            log.info "term: $testTerm -- $testCountMap"
//            int testCount = testCountMap.count
//            if (testCount >= minFrequency) {
//                Map baseCountMap = baseTermPercents[testTerm]
//                if (baseCountMap) {
//                    log.info "Base count map: $baseCountMap -- "
//                    Integer basePercent = baseCountMap.percent
//                    Integer percentDifference = testCountMap.percent - basePercent
//                } else {
//                    log.info "\t\tTerm ($testTerm) in test collection, but not base!"
//                }
//            } else {
//                log.info "\t\t$testTerm had too low freq (${testCount} < $minFrequency)"
//            }
//        }
//        return termDifferences
//    }
//*/
//
//
//    /**
//     * Slice map of terms and freq count into 5 buckets
//     * @param termTrequencies
//     * @param scale
//     * @param minTermCount allow "skipping' of single term freq in bucketization, often there are so many single terms, it throws off the bucketization, if 2 or more, all the 'skipped' terms will be added to the first bucket
//     * @return
//     */
//    static Map<Integer, List> bucketizeTermFrequencies(Map<String, Integer> termTrequencies, int scale = 5, int minTermCount =2) {
//        Map<Integer, List<String>> buckets = [:].withDefault {[]}
//        Map tfSorted = termTrequencies.sort {it.value}
//        List indexes = (1..scale)
//        Collection<Number> dataset =  minTermCount ? tfSorted.values().findAll {it >= minTermCount} : tfSorted.values()
//        HashMap<Integer, Double> bucketIndexes = Quantiles.scale(scale).indexes(indexes).compute(dataset.sort())
//        if(bucketIndexes[1]==2){
//            if(bucketIndexes[2]==2) {
//                log.info "\t\tBucket index 1 and 2 == '2', so dial bucket index 1 back to '1'..."
//                bucketIndexes[1] = 1
//            } else {
//                log.debug "bucket index 1==2, but idx 2==${bucketIndexes[2]}... everything normal...?"
//            }
//        }
//        log.info "Bucket indexes: ${bucketIndexes}"
//
//        termTrequencies.each {termEntry ->
////        tfSorted.each {String term, Integer freq ->
//            def bucketEntry = bucketIndexes.find { Integer idx, Double freqBucketPoint ->
//                termEntry.value <= freqBucketPoint
//            }
//            Integer i = bucketEntry.key
//            buckets[i] << termEntry
//        }
//
//        return buckets
//    }
//
//    /**
//     * collect a map of term, [frequency, bucketNumber]
//     * @param termFrequencies
//     * @param scale
//     * @param minTermCount
//     * @return Map with term, freq, and bucket
//     */
//    static Map<String, Map<String, Object>> addTermFreqBuckets(Map<String, Integer> termFrequencies, int scale = 5, int minTermCount =2) {
//        log.warn "Incomplete or broken analysis code?? addTermFreqBuckets..."
//        Map<String, Map<String, Object>> termsWithBuckets = [:]
//        List indexes = (1..scale)
//        Collection<Number> dataset =  minTermCount ? termFrequencies.values().findAll {it >= minTermCount} : termFrequencies.values()
//        HashMap<Integer, Double> bucketIndexes = Quantiles.scale(scale).indexes(indexes).compute(dataset.sort())
//        if(bucketIndexes[1]==2 && bucketIndexes[2]==2) {
//                log.info "\t\tBucket index 1 and 2 == '2', so dial bucket index 1 back to '1'..."
//                bucketIndexes[1] = 1
//        }
//        log.info "Bucket indexes: ${bucketIndexes}"
//
//        termFrequencies.each {termEntry ->
//            def bucketEntry = bucketIndexes.find { Integer idx, Double freqBucketPoint -> termEntry.value <= freqBucketPoint }
//            Integer bucketNumber = bucketEntry.key
//            termsWithBuckets[termEntry.key] = [frequency:termEntry.value, bucket:bucketNumber]
//        }
//
//        return termsWithBuckets
//    }
//
//}
//
