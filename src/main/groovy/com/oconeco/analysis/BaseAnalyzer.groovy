package com.oconeco.analysis

import com.oconeco.helpers.Constants
import com.oconeco.models.FSFile
import com.oconeco.models.FSObject
import com.oconeco.models.SavableObject
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.tika.exception.TikaException
import org.apache.tika.metadata.Metadata

//import org.apache.logging.log4j.core.Logger

import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.Parser
import org.apache.tika.sax.BodyContentHandler

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

class BaseAnalyzer {
    static final Logger log = LogManager.getLogger(this.class.name);

    public static final String HEAVYPARSE = 'heavyParse'
    //'OCR (not immplemented yet) and other more advanced content extraction'

    public static final String SEGMENT_SECTIONS = 'segmentSections' // break documents into sections/chapters
    public static final String SEGMENT_PARAGRAPHS = 'segmentParagraphs' // break documents into paragraphs
    public static final String SEGMENT_SENTENCES = 'segmentSentence' // break documents into sentences

    public static final String NLP_POS = 'nlpPOS'           // tag sentences with parts of speech
    public static final String NLP_NER = 'nlpNER'           // tag sentences with parts of speech
    /** tag sentences with parts of speech */
    public static final List NLP_BUNDLE = [Constants.TRACK, Constants.PARSE, SEGMENT_SECTIONS, SEGMENT_PARAGRAPHS, SEGMENT_SENTENCES, NLP_POS, NLP_NER]

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

    Parser tikaParser = null
    BodyContentHandler tikaBodyHandler = null


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
    BaseAnalyzer(Map<String, Map<String, Object>> groupNameMap, Map<String, Map<String, Object>> itemNameMap,
                 Map<String, Map<String, Object>> groupPathMap = null, Map<String, Map<String, Object>> itemPathMap = null,
                 AutoDetectParser tikaParser = null, BodyContentHandler tikaBodyHandler = null) {
        log.info "BaseAnalyzer constructor(groupNameMap, itemNameMap, groupPathMap?, itemPathMap?) -> groupNameMap keys(${groupNameMap.keySet()}), itemNameMap.keys(${itemNameMap.keySet()}) "

        Map<String, Object> gnm = groupNameMap[Constants.IGNORE]
        if (gnm) {
            ignoreGroup = gnm.pattern
            this.groupNameMap = groupNameMap.findAll { it.key != Constants.IGNORE }
            log.info "\t\tFound special ignoreGroup entry ($ignoreGroup), setting to Analyzer prop: 'ignoreGroup' and removing from groupNameMap ($groupNameMap) "
        } else {
            this.groupNameMap = groupNameMap
            log.warn "\t\tNo Group Ignore entry found!! $groupNameMap"
        }

        Map<String, Object> inm = itemNameMap[Constants.IGNORE]
        if (inm) {
            ignoreItem = inm.pattern
            this.itemNameMap = itemNameMap.findAll { it.key != Constants.IGNORE }
            log.info "\t\tFound special ignoreItem entry ($ignoreItem), setting to Analyzer prop: 'ignoreItem' and removing from itemNameMap ($itemNameMap) "
        } else {
            this.itemNameMap = itemNameMap
            log.warn "\t\tNo Ignore ITEM entry found!! $itemNameMap"
        }

        if (groupPathMap) {
            log.info "\t\tFound groupPathMap: $groupPathMap ($this)"
            this.groupPathMap = groupPathMap
        }
        if (itemPathMap) {
            log.info "\t\tFound itemPathMap: $itemPathMap ($this)"
            this.itemPathMap = itemPathMap
        }

        if (tikaParser) {
            this.tikaParser = tikaParser
        }
        if (tikaBodyHandler) {
            this.tikaBodyHandler = tikaBodyHandler
        }

    }


//    BaseAnalyzer(Map<String, Map<String, Object>> groupNameMap, Map<String, Map<String, Object>> itemNameMap, Map<String, Map<String, Object>> groupPathMap = null, Map<String, Map<String, Object>> itemPathMap = null,
//                 AutoDetectParser parser = null, BodyContentHandler handler = null) {
//    }


    def analyze(List<SavableObject> objectList) {
        List<Map<String, Map<String, Object>>> results = []
        objectList.each { SavableObject object ->
            log.info "Analyze list (size: ${objectList.size()}) objects: $objectList"
//            List<Map<String, Map<String, Object>>> analysisResults = analyze(object)
            Map<String, Map<String, Object>> analysisResults = analyze(object)
            results << analysisResults
        }
        return results
    }


    /**
     * check the object against the Analyzer and see if we should ignore.
     * If ignore and it's a group object: still save the 'easy' metadata, but mark it as ignore, and skip any futher processing of this group (i.e. Folder)
     * If ignore for for item, probably don't save anything, and ignore the item altogehter (i.e. File)
     * @param object
     * @return true if we should ignore the object
     */
    boolean shouldIgnore(SavableObject object) {
        if (object.ignore == null) {
            if (object.groupObject) {
                if (this.ignoreGroup) {
                    Matcher matcher = (object.name =~ this.ignoreGroup)
                    object.ignore = matcher.matches()
                    if (object.ignore) {
                        String match = matcher.group(1)
                        object.labels << Constants.IGNORE
                        Map ml = [(Constants.IGNORE): [pattern: this.ignoreGroup, (Constants.LABEL_MATCH): match]]
                        object.matchedLabels = ml
                        log.info "\t\tIGNORE: processing a groupObject ($object), this matches group name ignore (${this.ignoreGroup} "
                    } else {
                        log.debug "\t\tprocess: processing a groupObject ($object), this does NOT match group name ignore (${this.ignoreGroup} "
                    }
                } else {
                    object.ignore = false
                    log.warn "\t\t no ignoreGroup property for this Analyzer, so setting object.ignore==false analyzer:($this) -- object:($object) (bug??)"
                }
            } else {
                if (this.ignoreItem) {
                    Matcher matcher = (object.name =~ this.ignoreItem)
                    object.ignore = matcher.matches()
                    if (object.ignore) {
                        String match = matcher.group(1)
                        log.info "\t\tIGNORE: processing an Item Object ($object), this matches item name ignore (${this.ignoreItem} -- match:$match "
                        object.labels << Constants.IGNORE
                        Map ml = [(Constants.IGNORE): [pattern: this.ignoreItem, (Constants.LABEL_MATCH): match]]
                        object.matchedLabels = ml
                    } else {
                        log.debug "\t\tprocessing an Item Object ($object),  this does NOT match item name ignore (${this.ignoreItem} -- ignore? ${object.ignore} "
                    }
                } else {
                    object.ignore = false
                    log.warn "\t\t no ignoreItem property for this Analyzer($this), so setting object.ignore==false -- object:($object) (bug??)"
                }
            }
            if (object.ignore == null) {
                log.warn "Object($object) has 'ignore' propert still null"
            }
            return object.ignore
        } else {
            log.warn "\t\tFound existing ignore flag (${object.ignore}) on onbject: $object (remove this, just a code/processing check during development"
            return object.ignore
        }
    }


    /**
     * check "short-circuit" ignore patterns to see if we should ignore this item. If yes, skip to the end
     * If no: apply map labels, with configured processing pipelines (analysis key in sub map)
     * @param object SavableObject to analyze
     * @return matching analysis map entries (pattern driven label, plus matching pattern, plus list of analysis 'steps')
     */
    Map<String, Map<String, Object>> analyze(SavableObject object) {
        log.debug "analyze object: $object"
        if (object) {
//        def results = []
            if (shouldIgnore(object)) {
                log.info "\t\tIgnore object ($object) (skipping majority of `analyze(object)` method"
                Map ignoreMap = [(Constants.IGNORE): [pattern: '', analysis: [], (Constants.LABEL_MATCH): Constants.NO_MATCH]]
                object.matchedLabels = ignoreMap
            } else {
                object.matchedLabels = applyLabelMaps(object)
//            results.addAll(matchedEntries)
                object.matchedLabels.each { String label, Map labelMatchMap ->
                    def analysis = labelMatchMap.analysis
                    if (analysis) {
                        log.debug "\t\t++++ -> Apply analysis: $label->$analysis -- object (${object}) -- matched on: ${labelMatchMap.get(Constants.LABEL_MATCH)}"
                        // todo - move me to after "shouldUpdate'
                        def rc = this.doAnalysis(label, object)
                        log.debug "\t\tdoAnalysis($label) results: $rc"
                    } else {
                        log.warn "\t\tNothing to process? $label - $analysis"
                    }
                    // todo - more code here
                }
            }
        } else {
            log.warn "Null SavableObject($object) -- bug??? nothing to analyze here"
        }

        return object?.matchedLabels
    }


    /**
     * upper level call to iterate through matchedLabels and cal specific analysis processes based on value in matched map
     * @param object
     * @return
     */
    def doAnalysis(SavableObject object) {
        def results = []
        if (object) {
            log.info "iterate through each matched label entry, and call doAnalysis(label, object)"
            object.matchedLabels.each {
                def foo = doAnalysis(it.key, object)
                results << foo
            }
        } else {
            log.warn "No valid object in doAnalysis(object:$object) -- nothing to do here... bug??"
        }
        return results
    }


    /**
     * Peform specific analysis basd on analysisName and use that to update SavableObject(object)
     * @param analysisName
     * @param object
     * @return
     */
    def doAnalysis(String analysisName, SavableObject object) {
        def results = null
        log.info "Analysis ($analysisName) on object:($object)"
        switch (analysisName.toLowerCase()) {
            case 'track':
                log.info "Do analysis named '$analysisName' on object $object"
                results = this.track(object)
                break
            case 'parse':
                log.info "Do analysis named '$analysisName' on object $object"
                results = this.parse(object)
                String mime = results?.metadata?.get('Content-Type')
                if(mime){
                    object.mimeType = mime
                }
                break
//            case 'default':
//                log.info "Do analysis named 'default' on object $object"
//                break
            case 'ignore':
                log.warn "Do analysis named '$analysisName' on object $object -- ATTENTION: should probably never get to doAnalysis() on an 'ignored' object...."
                break
            default:
                log.warn "$Constants.NO_MATCH in swtich, falling back to  default analysis on object $object"
                results = this.track(object)
                break
        }
        log.debug "\t\tResults $results (for analysis:$analysisName on object:$object)"
        return results
    }


/**
 * Based on if SavableObject is a group/wrapper (ie. folder) or an individual item (i.e. File) check the relevant label maps and add all those labels (keys) that match (based on name regex, and optionally path regex)
 * @param object SavableObject to check (name, optional path)
 * @return all matching map entries, focused on entry keys which become item labels in solr (or other persistence???)
 */
    Map<String, Map<String, Object>> applyLabelMaps(SavableObject object) {
        Map<String, Map<String, Object>> results = [:]
        log.debug "\t\tapplyLabelMaps: $object"
        if (object.groupObject) {
            Map<String, Map<String, Object>> nameMap = groupNameMap
            if (nameMap) {
                results << applyLabels(object, object.name, nameMap)
            }

            Map<String, Map<String, Object>> pathMap = groupPathMap
            if (pathMap) {
                results << applyLabels(object, object.path, pathMap)
            }

        } else if (object.type == FSFile.TYPE) {
            Map<String, Map<String, Object>> nameMap = this.itemNameMap
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


/**
 * Based on if SavableObject is a group/wrapper (ie. folder) or an individual item (i.e. File) check the relevant label maps and add all those labels (keys) that match (based on name regex, and optionally path regex)
 * @param object SavableObject to check (name, optional path)
 * @param name explicit label to be added to object (in solr...)
 * @param labelMap the child map which should have 'pattern' and 'analysis' values
 * @return all matching map entries, focused on entry keys which become item labels in solr (or other persistence???)
 */
    Map<String, Map<String, Object>> applyLabels(SavableObject object, String name, Map<String, Map<String, Object>> labelMap) {
        Map<String, Map<String, Object>> matchingLabels = [:]
        log.debug "\t\tapplyLabels: name($name) -> object($object)"
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
                    mapVal.put(Constants.LABEL_MATCH, s)
                    matchingLabels.put(label, mapVal)
                    log.info "\t\tMatch: $label name($name) -- matches str($s)) =~ pattern($pattern) "
                } else {
                    log.debug "${Constants.NO_MATCH}, obj($object) name($name) LABEL($label)::pattern($pattern)"
                }
            } else {
                log.debug "no pattern, label: $label -- pattern($pattern)"
                defaultLabel = label
            }
        }
        if (matchingLabels.size() > 0) {
            log.debug "found matching label, no need for default"
        } else {
            log.info "\t\t----No matching labels found for name($name)"
            if (defaultLabel) {
                object.labels << defaultLabel
                Map map = labelMap.get(defaultLabel)
                map.put(Constants.LABEL_MATCH, Constants.NO_MATCH)
                matchingLabels.put(defaultLabel, map)
                // todo -- revisit, hackish approach

            }
        }
        return matchingLabels
    }

    def track(SavableObject object) {
        def results = null
        if (object instanceof FSObject) {
            FSObject fso = object
            results = fso.addFileDetails()
        }
        return results
    }


    def parse(SavableObject object) {
        String content
        Metadata metadata
        if(tikaBodyHandler && tikaParser) {
            try (InputStream inputStream = object.thing.newInputStream()) {
                if (inputStream) {
                    metadata = new Metadata();
                    tikaParser.parse(inputStream, tikaBodyHandler, metadata);

                    if (metadata) {
                        log.info "metadata: $metadata"
                        object.metadata = metadata
                    } else {
                        log.warn "No metadata? $srcfile -- meta: $metadata"
                    }
                    content = tikaBodyHandler.toString()
                    if (content) {
                        object.content = content
                        log.info "Content, size(${content.size()}"
                    } else {
                        log.info "No content....!!!"
                    }
                } else {
                    log.info "No input Stream?)"
                }
            } catch (TikaException tikaException) {
                log.error "Tika exception: $tikaException"
            }
            return [content: content, metadata: metadata]
        } else {
            log.warn "No tikaParser($tikaParser) or tikaBodyHandler:($tikaBodyHandler), no parsing possible"
        }
    }

    @Override
    public String toString() {
        return "BaseAnalyzer{" +
                "ignoreItem=" + ignoreItem +
                ", ignoreGroup=" + ignoreGroup +
                ", groupNameMap.keyset=" + groupNameMap.keySet() +
                ", itemNameMap.keyset=" + itemNameMap.keySet() +
                ", groupPathMap.keyset=" + groupPathMap?.keySet() +
                ", itemPathMap.keyset=" + itemPathMap?.keySet() +
                '}';
    }

}
