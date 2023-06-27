package com.oconeco.analysis

import com.oconeco.helpers.Constants
import com.oconeco.models.FSFile
import com.oconeco.models.FSFolder
import com.oconeco.models.SavableObject
import org.apache.log4j.Logger

import java.util.regex.Matcher
import java.util.regex.Pattern
/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

class BaseAnalyzer {
    static final Logger log = Logger.getLogger(this.class.name);

    public static final String IGNORE = 'ignore'           //for folders: save metadata with 'ignore' flag, for files don't save anything
    // default is equivalent to 'locate' in linux, just basic location/size/date information, no parsing
    public static final String DEFAULT = 'default'           //catch all for anything not matched elsewhere (pattern typically ignored, just a placeholder)

    public static final String TRACK = 'track'              //'like locate, file name, path, date, size'
    public static final String PARSE = 'parse'              //'simple TIKA-like content extraction'
    public static final List BASIC_BUNDLE = [TRACK, PARSE]              //typical content extraction
    public static final String ARCHIVE = 'archive'              //typical content extraction
    public static final List ARCHIVE_BUNDLE = [ARCHIVE, TRACK]              //unarchive, then track, don't assume parsing (yet)

    public static final String SOURCE_CODE = 'sourceCode'              //beginning of source code specific analysis (more to do)
    public static final List SOURCE_BUNDLE = [TRACK, SOURCE_CODE]              //beginning of source code specific analysis (more to do)

    public static final String LOGS = 'logs'              //beginning of source code specific analysis (more to do)
    public static final List LOGS_BUNDLE = [TRACK, LOGS]              //beginning of source code specific analysis (more to do)

    public static final String HEAVYPARSE = 'heavyParse'    //'OCR (not immplemented yet) and other more advanced content extraction'

    public static final String SEGMENT_SECTIONS = 'segmentSections' // break documents into sections/chapters
    public static final String SEGMENT_PARAGRAPHS = 'segmentParagraphs' // break documents into paragraphs
    public static final String SEGMENT_SENTENCES = 'segmentSentence' // break documents into sentences

    public static final String NLP_POS = 'nlpPOS'           // tag sentences with parts of speech
    public static final String NLP_NER = 'nlpNER'           // tag sentences with parts of speech
    /** tag sentences with parts of speech */
    public static final List NLP_BUNDLE = [TRACK, PARSE, SEGMENT_SECTIONS, SEGMENT_PARAGRAPHS, SEGMENT_SENTENCES, NLP_POS, NLP_NER]

    /** special patterns to short-circuit further processing */
    Pattern ignoreItem
    /** special patterns to short-circuit further processing */
    Pattern ignoreGroup

    // ciritical mapping for labels and analysis chain for folder and file names
    Map<String, Map<String, Object>> groupNameMap
    Map<String, Map<String, Object>> itemNameMap

    // optional additional mapping for folder & file paths
    Map<String, Map<String, Object>> groupPathMap
    Map<String, Map<String, Object>> itemPathMap

    /**
     * Create a basic analyzer with default group and item maps (ignore and default entries only)
     * todo -- is this valid? least surprise issues? blank constructor intitializing default values...??
     */
    BaseAnalyzer() {
        this(Constants.DEFAULT_FOLDERNAME_LOCATE, Constants.DEFAULT_FILENAME_LOCATE)
        log.info "No arg constructor BaseAnalyzer: $this  (consider passing in basic folder & file pattern maps??)"
    }


    /**
     * Base constructor focusing on folder and file name mapping. Key is the label to assign based on pattern match, and then analysis is the chain of analyzers to use
     * @param groupNameMap
     * @param itemNameMap
     * @param folderPathMap
     * @param filePathMap
     */
    BaseAnalyzer(Map<String, Map<String, Object>> groupNameMap, Map<String, Map<String, Object>> itemNameMap, Map<String, Map<String, Object>> groupPathMap = null, Map<String, Map<String, Object>> itemPathMap = null) {
        Map<String, Object> gnm = groupNameMap[IGNORE]
        if (gnm) {
            ignoreGroup = gnm.pattern
            this.groupNameMap = groupNameMap.findAll {it.key != IGNORE}
            log.info "Found special ignoreGroup entry ($ignoreGroup), setting to Analyzer prop: 'ignoreGroup' and removing from groupNameMap ($groupNameMap) "
        } else {
            this.groupNameMap = groupNameMap
            log.info "No Ignore entry found!! $groupNameMap"
        }

        Map<String, Object> inm = itemNameMap[IGNORE]
        if (inm) {
            ignoreItem = inm.pattern
            this.itemNameMap = itemNameMap.findAll {it.key != IGNORE}
            log.info "Found special ignoreItem entry ($ignoreItem), setting to Analyzer prop: 'ignoreItem' and removing from itemNameMap ($itemNameMap) "
        } else {
            this.itemNameMap = itemNameMap
            log.info "No Ignore ITEM entry found!! $itemNameMap"
        }

        if(groupPathMap) {
            log.info "\t\tFound groupPathMap: $groupPathMap ($this)"
            this.groupPathMap = groupPathMap
        }
        if(itemPathMap) {
            log.info "\t\tFound itemPathMap: $itemPathMap ($this)"
            this.itemPathMap = itemPathMap
        }
    }


    /**
     * extract patterns from config file
     * @param config
     * todo -- review and ensure this is accurate/current
     */
    BaseAnalyzer(ConfigObject config) {
        this.groupNameMap = config?.namePatterns?.folders
        this.itemNameMap = config?.namePatterns?.files
        this.groupPathMap = config?.pathPatterns?.folders
        this.itemPathMap = config?.pathPatterns?.files
    }



    /**
     * Check for special 'ignore group' pattern, and move it to Analyzer property
     * the idea is that if this pattern matches, short-cirtuit any further analysis -- this is 'exclusive',
     * other entries are not exclusive, multiple matches permissable
     * @param groupNameMap
     * @return
     */
    def extractIgnoreGroupPattern(Map<String, Map<String, Object>> groupNameMap) {
        Map<String, Object> gnm = groupNameMap[IGNORE]
        def rc
        if (gnm) {
            ignoreGroup = gnm.pattern
            log.info "Found special ignoreGroup entry ($ignoreGroup), setting to Analyzer prop: 'ignoreGroup' and removing from groupNameMap ($groupNameMap) "
            rc = groupNameMap.remove(IGNORE)
        }
        return rc
    }


    /**
     * Check for special 'ignore item' pattern, and move it to Analyzer property
     * the idea is that if this pattern matches, short-cirtuit any further analysis -- this is 'exclusive',
     * other entries are not exclusive, multiple matches permissable
     * @param itemNameMap
     * @return
     */
    def extractIgnoreItemPattern(Map<String, Map<String, Object>> itemNameMap) {
        def item = itemNameMap[IGNORE]
        def rc
        if (item) {
            ignoreItem = item.pattern
            log.info "Found special ignoreGroup entry ($ignoreItem), setting to Analyzer prop: 'ignoreGroup' and removing from itemNameMap ($itemNameMap) "
            rc = itemNameMap.remove(IGNORE)
        }
        return rc
    }



    List<String> analyze(List<SavableObject> objectList) {
        def results = []
        objectList.each { SavableObject object ->
            def rc = analyze(object)
            results << rc
        }
        return results
    }


    List<Map<String, Map<String, Object>>> analyze(SavableObject object) {
        log.debug "analyze object: $object"

        def results = []
        Map<String, Map<String, Object>> matchedEntries = applyLabelMaps(object)
        results.addAll(matchedEntries)
        matchedEntries.each { String label, Map val ->
            def analysis = val.analysis
            if (analysis) {
                log.info "!!!! Apply analysis: $label->$analysis"
            } else {
                log.warn "Nothing to process? $label - $analysis"
            }
            // todo - more code here
        }
        return results

    }


    /**
     *
     * @param object
     * @return
     */
    Map<String, Map<String, Object>> applyLabelMaps(SavableObject object) {
        Map<String, Map<String, Object>> results = [:]
        log.info "applyLabelMaps: $object"
        if (object.type == FSFolder.TYPE) {
            Map<String, Map<String, Object>>  nameMap = groupNameMap
            if (nameMap) {
                results << applyLabels(object, object.name, nameMap)
            }

            Map<String, Map<String, Object>>  pathMap = groupPathMap
            if (pathMap) {
                results << applyLabels(object, object.path, pathMap)
            }

        } else if (object.type == FSFile.TYPE) {
            Map<String, Map<String, Object>>  nameMap = this.itemNameMap
            if (nameMap) {
                results << applyLabels(object, object.name, nameMap)
            }

            Map<String, Map<String, Object>> pathMap = itemPathMap
            if (pathMap) {
                results << applyLabels(object, object.path, nameMap)
            }
        }
        return results
    }


    Map<String, Map<String, Object>> applyLabels(SavableObject object, String name, Map<String, Map<String, Object>> labelMap) {
        Map<String, Map<String, Object>> matchingLabels = [:]
        log.info "applyLabels: name($name) -> object($object)"
        def defaultLabel = null
        labelMap.each { label, mapVal ->
            log.debug "\t\tLabel($label) - map($mapVal)"
            Pattern pattern = mapVal.pattern
            if (pattern) {
                Matcher matcher = pattern.matcher(name)
//                if (name ==~ pattern) {
                if (matcher.matches()) {
                    String s = matcher.group(1)
                    object.labels << label
                    matchingLabels.put(label, mapVal)
                    log.info "\t\tMatch: $label name($name) -- matches str($s)) =~ pattern($pattern) "
                } else {
                    log.debug "no match, obj($object) name($name) LABEL($label)::pattern($pattern)"
                }
            } else {
                log.debug "no pattern, label: $label -- pattern($pattern)"
                defaultLabel = label
            }
        }
        if (matchingLabels) {
            log.debug "found matching label, no need for default"
        } else {
            log.info "\t\t----No matching labels found for name($name)"
            if (defaultLabel) {
                object.labels << defaultLabel
                matchingLabels.put(defaultLabel, labelMap.get(defaultLabel))         // todo -- revisit, hackish approach

            }
        }
        return matchingLabels
    }

//    Map<String, Map<String, Object>> applyLabelsByPath(SavableObject object, def pathMap) {
//        def analysisSteps = []
//        String path = object.path
//        pathMap.each { label, mapVal ->
//            Pattern pattern = mapVal.pattern
//            if (path ==~ pattern) {
//                object.labels << label
//                analysisSteps << mapVal.analysis
//            } else {
//                log.debug "no match, obj($object) name($path) pattern($pattern)"
//            }
//        }
//        return analysisSteps
//
//    }

    String toString() {
        String s = this.class.simpleName
    }
}
