package com.oconeco.analysis

import com.oconeco.helpers.Constants
import com.oconeco.models.FSObject
import com.oconeco.models.SavableObject
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.tika.exception.TikaException
import org.apache.tika.exception.WriteLimitReachedException
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext

//import org.apache.logging.log4j.core.Logger

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
//    BodyContentHandler tikaBodyHandler = null
    int MAX_BODY_CHARS = 10 * 1024 * 1024       // todo -- revisit :: 10 mb of text...??
//    int MAX_BODY_CHARS = 1000 * 1000 * 1000


    /**
     * Create a basic analyzer with default group and item maps (ignore and default entries only)
     * todo -- is this valid? least surprise issues? blank constructor intitializing default values...??
     */
    BaseAnalyzer() {
        this(Constants.DEFAULT_FOLDERNAME_LOCATE, Constants.DEFAULT_FILENAME_LOCATE, null, null, null, null)
        log.info "No arg constructor BaseAnalyzer: $this  (consider passing in basic folder & file pattern maps??)"
    }

    BaseAnalyzer(ConfigObject cfg) {
        this(cfg.namePatterns.folders, cfg.namePatterns.files, cfg.pathPatterns.folders, cfg.pathPatterns.files)
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
            // todo -- consider just a map-remove op??
            log.debug "\t\tFound special ignoreGroup entry ($ignoreGroup), setting to Analyzer prop: 'ignoreGroup' and removing from groupNameMap ($groupNameMap) "
        } else {
            this.groupNameMap = groupNameMap
            log.warn "\t\tNo Group Ignore entry found!! $groupNameMap"
        }

        Map<String, Object> inm = itemNameMap[Constants.IGNORE]
        if (inm) {
            ignoreItem = inm.pattern
            this.itemNameMap = itemNameMap.findAll { it.key != Constants.IGNORE }
            log.debug "\t\tFound special ignoreItem entry ($ignoreItem), setting to Analyzer prop: 'ignoreItem' and removing from itemNameMap ($itemNameMap) "
        } else {
            this.itemNameMap = itemNameMap
            log.warn "\t\tNo Ignore ITEM entry found!! $itemNameMap"
        }

        if (groupPathMap) {
            log.debug "\t\tFound groupPathMap: $groupPathMap ($this)"
            this.groupPathMap = groupPathMap
        }
        if (itemPathMap) {
            log.debug "\t\tFound itemPathMap: $itemPathMap ($this)"
            this.itemPathMap = itemPathMap
        }

        if (tikaParser) {
            this.tikaParser = tikaParser
        } else {
            this.tikaParser = new AutoDetectParser();
        }
    }


    /**
     * convenience method to iterate over a list of savable objects (i.e. a folder and it's children)
     * @param objectList
     * @return status results (not intended for any further analysis) -- todo revisit this return value, does it make sense? confusiing???
     */
    def analyze(List<SavableObject> objectList) {
//        log.debug "\t\tAnalyze list (size: ${objectList.size()}) -- first object: ${objectList[0]}"
        List<Map<String, Map<String, Object>>> results = []
        log.debug "\t\tAnalyze children SavableObjects list (size: ${objectList?.size()})"
        objectList.each { SavableObject object ->
            if (object.groupObject) {
                log.debug "\t\tskip analyzing child group object:($object) -- it will be handled in a parent processing call..."
            } else {
                Map<String, Map<String, Object>> analysisResults = analyze(object)
                log.debug "\t\t analyzed object:($object) -- results:($analysisResults)"
                results << analysisResults
            }
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
                        log.info "\t\tIGNORE: processing a groupObject ($object), this MATCHES group name ignore (${this.ignoreGroup} "
                    } else {
                        log.debug "\t\tprocess: processing a groupObject ($object), this does NOT match group name ignore (${this.ignoreGroup}, so we should PROCESS it"
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
                        log.debug "\t\tIGNORE: processing an Item Object ($object), this matches item name ignore (${this.ignoreItem} -- match:$match "
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
            log.debug "\t\tFound existing ignore flag (${object.ignore}) on onbject: $object (remove this, just a code/processing check during development"
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
            def lm = applyLabelMaps(object)
            log.debug "\t\tMatched label: $lm"
//                object.matchedLabels << lm
            object.matchedLabels.each { String label, Map labelMatchMap ->
                def analysis = labelMatchMap.analysis
                if (analysis instanceof List) {
                    analysis.each { String analysisName ->
                        log.debug "\t\t....Apply analysis: $label->$analysisName -- object (${object}) -- matched on: ${labelMatchMap.get(Constants.LABEL_MATCH)}"

                        def rc = this.doAnalysis(analysisName, object)
                        log.debug "\t\tdoAnalysis($label) results: $rc"
                    }
                } else {
                    log.info "\t\tNothing to process? $label - $analysis"
                }
                // todo - more code here
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
    List doAnalysis(SavableObject object) {
        List results = []
        if (object) {
            if (object.childGroups) {
                def childResults = doAnalysisOnChildGroups(object.childGroups)
                results.addAll(childResults)
            }
            if (object.childItems) {
                def childResults = doAnalysisOnChildItems(object.childItems)
                results.addAll(childResults)
            }
            log.debug "iterate through each matched label entry, and call doAnalysis(label, object)"
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
        String lowerName = analysisName.toLowerCase()
        switch (lowerName) {
            case 'track':
                log.debug "\t\t....Do analysis named '$analysisName' on object $object"
                results = this.track(object)
                break
            case 'parse':
                if (object?.thing) {
                    if (object.groupObject) {
                        log.info "\t\tNo parse method created for group item (yet): $object -- path:${object.path}"
                    } else {
                        log.debug "\t\t....Do analysis named '$analysisName' on object $object (size:${object.size})"
                        results = this.parse(object)
                        //String mime = results?.metadata?.get('Content-Type')
                    }
                } else {
                    log.warn "Object thing:(${object.thing}) is not valid: $object"
                }

                break

            case Constants.SOURCE_CODE:
                log.info "\t\tdo source code parsing here..."
                break

            case 'ignore':
                log.debug "\t\t....ignore object $object"
                break
            default:
                log.info "....${Constants.NO_MATCH} on ($lowerName) in swtich, falling back to  default analysis on object $object"
                results = this.track(object)
                break
        }
        log.debug "\t\tResults $results (for analysis:$analysisName on object:$object)"
        return results
    }


    /**
     * simple wrapper, but can allow for more advanced overriding if needed for analyzing childGroup objects (i.e. folders/directories)
     * @param children
     * @return
     */
    List doAnalysisOnChildGroups(List<SavableObject> children) {
        List results = children.collect { doAnalysis(it) }
    }

    /**
     * simple wrapper, but can allow for more advanced overriding if needed for analyzing childItem objects (i.e. files/messages/...)
     * @param children
     * @return
     */
    List doAnalysisOnChildItems(List<SavableObject> children) {
        List results = children.collect { doAnalysis(it) }
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
            if (groupNameMap) {
                Map<String, Map<String, Object>> r = applyLabels(object, object.name, groupNameMap, "groupName")
                r.each { String key, Map map ->
                    results.put(key, map)
                }
            } else {
                log.warn "\t\tno groupNameMap to analyze with, skipping--object: $object"
            }

            if (groupPathMap) {
                Map<String, Map<String, Object>> r = applyLabels(object, object.path, groupPathMap, "groupPath")
                r.each { String key, Map map ->
                    results.put(key, map)
                }
            }

        } else {
            // if not a group, assume this is an item
            // todo -- is there any other option beyond group or item???
            if (itemNameMap) {
                Map<String, Map<String, Object>> r = applyLabels(object, object.name, itemNameMap, "itemName")
                r.each { String key, Map map ->
                    results.put(key, map)
                }
            }

            if (itemPathMap) {
                Map<String, Map<String, Object>> r = applyLabels(object, object.path, itemPathMap, "itemPath")
                r.each { String key, Map map ->
                    results.put(key, map)
                }
            }
        }
        return results
    }


    /**
     * Based on if SavableObject is a group/wrapper (ie. folder) or an individual item (i.e. File) check the relevant label maps and add all those labels (keys) that match (based on name regex, and optionally path regex)
     * @param object SavableObject to check (name, optional path)
     * @param nameOrPath explicit label to be added to object (in solr...)
     * @param labelMap the child map which should have 'pattern' and 'analysis' values
     * @return all matching map entries, focused on entry keys which become item labels in solr (or other persistence???)
     */
    Map<String, Map<String, Object>> applyLabels(SavableObject object, String nameOrPath, Map<String, Map<String, Object>> labelMap, String labelType) {
        Map<String, Map<String, Object>> matchingLabels = [:]
        log.debug "\t\tapplyLabels: name($nameOrPath) -> object(${object.path}) -- labelMap:$labelMap"
        def defaultLabel = null
        labelMap.each { label, mapVal ->
            if (mapVal.pattern) {
                log.debug "\t\tLabel($label) - map($mapVal)"
                Pattern pattern = mapVal.pattern
                Matcher matcher = pattern.matcher(nameOrPath)
//                if (name ==~ pattern) {
                if (matcher.matches()) {
                    if (matcher.groupCount() > 0) {
                        String s = matcher.group(1)
                        mapVal.put(Constants.LABEL_MATCH, s)
                    } else {
                        mapVal.put(Constants.LABEL_MATCH, pattern.pattern() + " => (default matching?? '.*'??")
                    }
                    String s = labelType + ':' + label
                    object.labels << s
                    object.matchedLabels.put(s, mapVal)
                    log.debug "\t\tMatch:$s name($nameOrPath) -- $mapVal "
                } else {
                    log.debug "${Constants.NO_MATCH}, obj($object) name($nameOrPath) LABEL($label)::pattern($pattern)"
                }
            } else {
                log.debug "no pattern, label: $label -- mapVal($mapVal) -- setting as default matching"
                defaultLabel = label
            }
        }
        if (object.labels.size() > 0) {
            log.debug "found matching label, no need for default"
        } else {
            log.debug "\t\t----No matching labels found for name($object)"
            if (defaultLabel) {
                object.labels << labelType + ' ' + defaultLabel
                Map map = labelMap.get(defaultLabel)
                map.put(Constants.LABEL_MATCH, Constants.NO_MATCH)
                object.matchedLabels.put(defaultLabel, map)
            }
        }
        return object.matchedLabels
    }

    def track(SavableObject object) {
        def results = null
        if (object instanceof FSObject) {
            FSObject fso = object
            results = fso.addFileDetails()
        } else {
            log.warn "Object:($object) is not instance of SavableObject, how did this happen"
        }
        return results
    }

    /**
     * call to extract content from object. Rely on tika for majority of extaction
     * @param object
     * @return
     */
    Map parse(SavableObject object) {
        Map results = [:]
        if (object.size > 0) {
            log.debug "\t\tparse(object:$object) starting..."
            if (object.groupObject) {
                log.warn "No parsing implmented for group objects yet:($object) -- path: ${object.path}"
            } else {
                results = parseAndUpdateItem(object)
            }

        } else {
            log.info "\t\t\t\tno size for object.thing (${object.thing} -- size:${object.size}) to analyze (zero-len object/file??), skipping 'parse' analysis "
        }
        return results
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


    LinkedHashMap<Object, Object> parseAndUpdateItem(SavableObject object) {
        Map results = [:]
        long start = System.currentTimeMillis()
        String content
        Metadata metadata = new Metadata();

        if (tikaParser) {
            try (InputStream inputStream = object.thing.newInputStream()) {
                if (inputStream) {
                    BodyContentHandler handler = new BodyContentHandler(MAX_BODY_CHARS)
                    ParseContext context = new ParseContext();

                    // todo -- check on thread safety (and gains/value of reusing this if multithreaded
                    // note: https://tika.apache.org/1.23/api/org/apache/tika/parser/RecursiveParserWrapper.html -- this is NOT thread safe..
                    tikaParser.parse(inputStream, handler, metadata, context);

                    if (metadata) {
                        log.debug "metadata: $metadata"
                        object.metadata = metadata
                        String ctype = metadata.get('Content-Type')
                        if (ctype) {
                            if (!object.mimeType) {
                                object.mimeType = ctype
                                log.debug "\t\tsetting object ($object) mimetype to tika Content type:($ctype)"

                            } else {
                                log.debug "\t\tobject ($object) mimetype already set:(${object.mimeType}) -- same as tika Content type?($ctype)??"
                            }
                        }
                    } else {
                        log.warn "No metadata? $srcfile -- meta: $metadata"
                    }
                    content = handler.toString()
                    if (content) {
                        object.content = content
                        object.contentSize = content.size()
                        log.debug "\t\t$object) Content size:(${content.size()})"
                    } else {
                        if (object.mimeType?.containsIgnoreCase('image')) {
                            log.debug "\t\tno content for object:($object) which is mimetype:(${object.mimeType}) - no content really expected..."
                        } else {
                            log.info "\t\tno content found for SavableObject:(${object.path}) in parse(object) method -- mimetype:(${object.mimeType})"
                        }
                    }
                    long end = System.currentTimeMillis()
                    long elapsed = end - start
                    metadata.parseTimeMS = elapsed
                    results = [content: content, metadata: metadata]
                    log.info "\t\t....parse(object:$object) -- (content size:${content?.size()}) -- Elapsed time: ${elapsed}ms (${elapsed / 1000} sec) "

                } else {
                    log.warn "No input Stream? Object:(${object}) path:(${object.path})"
                }

            } catch (WriteLimitReachedException writeLimitReachedException) {
                log.error "Tika write limit reached exception: $writeLimitReachedException -- object:${object}"
            } catch (LinkageError linkageError) {
                log.error "Tika linkage error exception: $linkageError -- object:${object}"
            } catch (FileNotFoundException fne) {
                log.error "File not found exception: $fne -- object:${object}"
            } catch (NoSuchFieldError nsfe) {
                log.error "Tika exception: $nsfe -- object:${object}"
            } catch (TikaException tikaException) {
                log.error "Tika exception: $tikaException -- object:${object}"
            }
        } else {
            log.warn "No tikaParser($tikaParser) no parsing possible"
        }

        results
    }

}
