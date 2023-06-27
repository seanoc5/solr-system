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
    public static final String DEFAULT = 'default'           //catch all for anything not matched elsewhere (pattern typically ignored, just a placeholder)

    public static final String TRACK = 'track'              //'like locate, file name, path, date, size'
    public static final String PARSE = 'parse'              //'simple TIKA-like content extraction'
    public static final List BASIC_LIST = [TRACK, PARSE]              //typical content extraction
    public static final String ARCHIVE = 'archive'              //typical content extraction
    public static final List ARCHIVE_LIST = [ARCHIVE, TRACK]              //unarchive, then track, don't assume parsing (yet)

    public static final String SOURCE_CODE = 'sourceCode'              //beginning of source code specific analysis (more to do)
    public static final List SOURCE_LIST = [TRACK, SOURCE_CODE]              //beginning of source code specific analysis (more to do)

    public static final String LOGS = 'logs'              //beginning of source code specific analysis (more to do)
    public static final List LOGS_LIST = [TRACK, LOGS]              //beginning of source code specific analysis (more to do)

    public static final String HEAVYPARSE = 'heavyParse'    //'OCR (not immplemented yet) and other more advanced content extraction'

    public static final String SEGMENT_SECTIONS = 'segmentSections' // break documents into sections/chapters
    public static final String SEGMENT_PARAGRAPHS = 'segmentParagraphs' // break documents into paragraphs
    public static final String SEGMENT_SENTENCES = 'segmentSentence' // break documents into sentences

    public static final String NLP_POS = 'nlpPOS'           // tag sentences with parts of speech
    public static final String NLP_NER = 'nlpNER'           // tag sentences with parts of speech
    public static final List NLP_LIST = [TRACK, PARSE, SEGMENT_SECTIONS, SEGMENT_PARAGRAPHS, SEGMENT_SENTENCES, NLP_POS, NLP_NER]
    // tag sentences with parts of speech

    Pattern ignoreItem
    Pattern ignoreGroup

    // ciritical mapping for labels and analysis chain for folder and file names
    def groupNameMap
    def itemNameMap

    // optional additional mapping for folder & file paths
    def folderPathMap
    def filePathMap

    BaseAnalyzer() {
        groupNameMap = Constants.DEFAULT_FOLDERNAME_LOCATE
        def igpEntry = extractIgnoreGroupPattern(groupNameMap)

        this.itemNameMap = Constants.DEFAULT_FILENAME_LOCATE
        def iipEntry = extractIgnoreItemPattern(Constants.DEFAULT_FILENAME_LOCATE)
        log.info "No arg constructor BaseAnalyzer: $this  (consider passing in basic folder & file pattern maps??)"
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
        ignoreItem = itemNameMap[IGNORE]
        def rc
        if (ignoreItem) {
            log.info "Found special ignoreGroup entry ($ignoreItem), setting to Analyzer prop: 'ignoreGroup' and removing from itemNameMap ($itemNameMap) "
            rc = itemNameMap.remove(IGNORE)
        }
        return rc
    }


/**
     * Base constructor focusing on folder and file name mapping. Key is the label to assign based on pattern match, and then analysis is the chain of analyzers to use
     * @param groupNameMap
     * @param itemNameMap
     * @param folderPathMap
     * @param filePathMap
     */
    BaseAnalyzer(groupNameMap, itemNameMap, folderPathMap = null, filePathMap = null) {
        this.groupNameMap = groupNameMap
        def igp = extractIgnoreGroupPattern(groupNameMap)
        this.itemNameMap = itemNameMap
        def iip = extractIgnoreItemPattern(itemNameMap)
        this.folderPathMap = folderPathMap
        this.filePathMap = filePathMap
    }


    BaseAnalyzer(ConfigObject config) {
        this.groupNameMap = config?.namePatterns?.folders
        this.itemNameMap = config?.namePatterns?.files
        this.folderPathMap = config?.pathPatterns?.folders
        this.filePathMap = config?.pathPatterns?.files
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
            def nameMap = groupNameMap
            if (nameMap) {
                results << applyLabels(object, object.name, nameMap)
            }

            def pathMap = folderPathMap
            if (pathMap) {
                results << applyLabels(object, object.path, pathMap)
            }

        } else if (object.type == FSFile.TYPE) {
            def nameMap = this.itemNameMap
            if (nameMap) {
                results << applyLabels(object, object.name, nameMap)
            }

            def pathMap = filePathMap
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
