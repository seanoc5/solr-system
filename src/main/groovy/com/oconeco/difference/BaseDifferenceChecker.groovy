package com.oconeco.difference

import com.oconeco.models.FSObject
import com.oconeco.persistence.SolrSystemClient

//import org.apache.commons.lang3.
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.solr.common.SolrDocument

import java.text.ParseException

/**
 * codify what qualifies as a difference worthy of 're-indexing'
 */
class BaseDifferenceChecker {
    Logger log = LogManager.getLogger(this.class.name)
    public static final String[] DEFAULT_DATEFORMATS = new String[]{"EEE MMM d HH:mm:ss z yyyy", "EEE, d MMM yyyy HH:mm:ss Z", "yyyy/MM/dd", "dd/MM/yyyy", "yyyy-MM-dd"}
    /** calculating size of group objects (i.e. folders) can be expensive, quick and dirty approach is just lastModifiedDate */
    boolean checkGroupSizes = true      // todo -- refactor to more elegant approach

    /**
     * default to checking sizes (especially for folders) == this can be notably longer, but more accurate, otherwise folders (and files?) will rely on lastModifiedDate
     * @param checkSizes
     */
    BaseDifferenceChecker(boolean checkSizes = true) {
        this.checkGroupSizes = checkSizes
    }

    /**
     * check DifferenceStatus to determine if any existing differences are significant enough to require an update
     * @param status predefined record of differences (status) to check to determine if there are compelling differences
     * @return true if the caller should update the saved record (solr, ....)
     */
    boolean shouldUpdate(BaseDifferenceStatus status) {
        boolean shouldUpdatePersistence = true
        String msg
        if (status) {
            if (status.noMatchingSavedDoc) {
                shouldUpdatePersistence = true
                msg = "no matching saved/solr doc, flagging for update"
//                status.differences << msg
                log.debug "\t\t--$msg"
            } else {
                if (status.differentIds) {
                    shouldUpdatePersistence = true
                    msg = "different ids, flagging for update (CAUTION: why would we ever compare different IDs???)"
                    log.warn "\t--$msg: $status"

                } else if (status.differentDedups) {
                    shouldUpdatePersistence = true
                    msg = "different dedups, flagging for update ($status.differences)"
                    log.debug "\t\t--$msg: $status"

                } else if (status.differentSizes) {
                    shouldUpdatePersistence = true
                    msg = "different sizes, flagging for update"
                    log.debug "\t\t----$msg: $status"

                } else if (status.differentPaths) {
                    shouldUpdatePersistence = true
                    msg = "different paths, flagging for update (how do we get different paths...?) "
                    log.warn "\t\t--: $status"

                } else if (status.differentLastModifieds) {
                    // todo -- check logic and assumptions
                    shouldUpdatePersistence = true
                    msg = "different lastModifiedDate, FLAGGING for update"
                    log.info "\t\t--$msg: $status"

                } else if (status.differentLocations) {
                    // todo -- check logic and assumptions
                    shouldUpdatePersistence = true
//                    String objectLocation = status.object.locationName
//                    String solrLocation = status.savedDocument.getFirstValue(SolrSystemClient.FLD_LOCATION_NAME)
//                    msg = "different locations, BUT NOT CONSIDERED WORTHY of reindexing, NOT flagging for update (fsfolder location: ${objectLocation}) != (solr doc location: ${solrLocation})}"
                    msg = "different locations, CONSIDERED WORTHY of reindexing (double check this logic), status differences:(${status.differences})"
                    log.warn "\t----$msg: $status"

                } else {
                    msg = "no differences found, flagging as NO update"
                    shouldUpdatePersistence = false
                }
            }
            if (!msg) {
                log.warn "\t\t....No message for status: $status???"
            }
        } else {
            log.warn "No valid DifferenceStatus, coding bug??!?!"
            shouldUpdatePersistence = true
        }

        return shouldUpdatePersistence
    }

    BaseDifferenceStatus compareCrawledGroupToSavedGroup(def crawledGroup, def savedGroup) {
        BaseDifferenceStatus status = new BaseDifferenceStatus(crawledGroup, savedGroup)
        BaseDifferenceStatus updatedStatus = this.compareCrawledGroupToSavedGroup(crawledGroup, savedGroup, status)
        return updatedStatus
    }


    /**
     * check groups to see if the is a change (and hence avoid checking each child object)
     * @param crawledGroup (i.e. folder/dir)
     * @param savedGroup - saved info from persistence later (i.e. solr document)
     * @return status of differences
     * todo -- refactor to checkGroupSizes as a param, or something better than the hacked prop of BaseDifferenceStatus
     */
    BaseDifferenceStatus compareCrawledGroupToSavedGroup(def crawledGroup, def savedGroup, BaseDifferenceStatus status) {
        String msg = null

        if (!crawledGroup) {
            msg = "GRoup Object ($crawledGroup) is null -- this almost certainly will be a problem!!! [[savedGroup:($savedGroup)??]]"
            log.warn "$msg: folder:$crawledGroup - savedGroup: $savedGroup"
            similarities << msg
        } else {
            if (!savedGroup) {
                msg = "savedGroup is null ($savedGroup)"
                log.debug "$msg: folder:$crawledGroup"
                status.noMatchingSavedDoc = true
                status.significantlyDifferent = true
            } else {
                String crawledID = crawledGroup.id
                String savedId = getSavedId(savedGroup)
                if (crawledID.equals(savedId)) {
                    status.differentIds = false
                    msg = "Crawled id ($crawledID) matches saved id($savedId)"
                    status.similarities << msg
                    log.debug "\t\t>>>>$msg"
                } else {
                    msg = "crawledGroup id ($crawledID) does not match solr id($savedId)"
                    status.differences << msg
                    log.warn "\t\t>>>>$msg"
                    status.differentIds = true
                    status.significantlyDifferent = true
                }

                Date crawledLastModified = getCrawledLastModified(crawledGroup)
                Date savedLastModified = getSavedLastModified(savedGroup)
                if (crawledLastModified == savedLastModified) {
                    msg = "lastModifiedDate same: $savedLastModified"
                    status.similarities << msg
                    status.differentLastModifieds = false
                    status.sourceIsNewer = false
                    log.debug "\t\t$msg"
                } else if (crawledLastModified > savedLastModified) {
                    msg = "lastModifiedDate crawledGroup (${crawledGroup.path}) is NEWER ($crawledLastModified) than solr folder ($savedLastModified)"
                    status.differences << msg
                    log.debug "\t\t>>>>${msg}"
                    status.differentLastModifieds = true
                    status.sourceIsNewer = true
                    status.significantlyDifferent = true
                } else {
                    msg = "crawledGroup (${crawledGroup.path}) lastModified ($crawledLastModified) is NOT the same date as saved folder ($savedLastModified), SaveObject is newer???"
                    status.differences << msg
                    log.warn "\t\t>>>> $msg (what logic is approapriate? currently we overwrite 'newer' saved/saved object with 'older' source object...???)"
                    status.differentLastModifieds = true
                    status.sourceIsNewer = false
                    status.significantlyDifferent = true    // is this true? should we overwrite a newer saved file with an older source object?
                }

                if (checkGroupSizes) {
                    Long savedSize = (Long) getSavedSize(savedGroup)
                    if (crawledGroup.size == savedSize) {
                        msg = "Same sizes, fsfolder: ${crawledGroup.size} == saved: $savedSize"
                        log.debug "\t\t${msg}"
                        status.similarities << msg
                        status.differentSizes = false
                    } else {
                        status.differentSizes = true
                        msg = "Different sizes, fsfolder(${crawledGroup.path}): ${crawledGroup.size} != saved: $savedSize (${crawledGroup.size - savedSize})"
                        log.debug "\t\t>>>>$msg"
                        status.differences << msg
                        status.significantlyDifferent = true
                    }

                } else {
                    log.debug "\t\t DifferencesChecker check sizes set to false, skipping size check...."
                }

                if (checkGroupSizes) {
                    String savedDedup = getSavedDedup(savedGroup)
                    if (crawledGroup.dedup == savedDedup) {
                        msg = "Same dedup: ${crawledGroup.dedup}"
                        log.debug "\t\t${msg}"
                        status.similarities << msg
                        status.differentDedups = false
                    } else {
                        status.differentDedups = true
                        msg = "Different dedups (${crawledGroup.path}), fsfolder: ${crawledGroup.dedup} != saved: $savedDedup"
                        log.info "\t\t>>>>$msg"
                        status.differences << msg
                        status.significantlyDifferent = true
                    }
                } else {
                    log.debug "\t\tskipping dedup  based on checker prop(checkGroupSizes:$checkGroupSizes)"
                }

                String savedPath = getSavedPath(savedGroup)
                if (crawledGroup.path == savedPath) {
                    msg = "Paths are same: ${savedPath}"
                    status.similarities << msg
                    log.debug "\t\t$msg"
                    status.differentPaths = false
                } else {
                    status.differentPaths = true
                    msg = "Different paths, Crawled path: ${crawledGroup.path} != saved: $savedPath"
                    log.warn "\t\t>>>>$msg (change this to info level once we are confident in logic)"
                    status.differences << msg
                    status.significantlyDifferent = true
                }

                String solrLocation = getSavedLocation(savedGroup)
                if (crawledGroup.locationName.equals(solrLocation)) {
                    msg = "locationName same: $solrLocation"
                    status.similarities << msg
                    log.debug "\t\t$msg"
                    status.differentLocations = false
                } else {
                    status.differentLocations = true
                    msg = "Different locations, fsfolder(${crawledGroup.path}): ${crawledGroup.locationName} != solr: $solrLocation (why do we have different locations!?!?)"
                    log.warn "\t\t$msg"
                    status.differences << msg
                    status.significantlyDifferent = true
                }
            }
        }
        return status
    }

    String getSavedId(Object savedObj) {
        String id = savedObj.id
        return id
    }

    String getSavedPath(Object savedObj) {
        String sp = savedObj.path ?: savedObj.path_s
        return sp
    }

    String getSavedDedup(Object savedObj) {
        String dd = savedObj.dedup ?: savedObj.dedup_s
    }

    String getSavedLocation(Object savedObj) {
        return savedObj.location
    }

    Date getCrawledLastModified(Object crawledObject) {
        def lm = null
        Date lmd = null
        if (crawledObject instanceof FSObject) {
            lm = ((FSObject) crawledObject).lastModifiedDate
        } else if (crawledObject instanceof Map) {
            lm = crawledObject.lastModified ?: crawledObject.lastModifiedDate
        } else {
            log.warn "Unknown type of crawledObject:($crawledObject.class.simpleName) -- this method getCrawledLastModified(object) should probably be overriden and customized, attempting blind grab..."
            lm = crawledObject.lastModified ?: crawledObject.lastModifiedDate
        }
        if (lm) {
            if (lm instanceof Date) {
                lmd = lm
            } else {
                lmd = parseDateThing(lm)
            }
        } else {
            log.info "No lastModifiedDate property identified for crawled object:($crawledObject), nothing to get... failed!"
        }
        return lmd
    }

    Date getSavedLastModified(Object savedObj) {
        def lm = null
        Date lmd = null
        if (savedObj instanceof SolrDocument) {
            lm = ((SolrDocument) savedObj).getFirstValue(SolrSystemClient.FLD_LAST_MODIFIED)
        } else if (savedObj instanceof Map) {
            lm = savedObj.lastModified ?: savedObj.lastModifiedDate
        } else {
            log.warn "Unknown type of savedObj:($savedObj.class.simpleName) -- this method getSavedLastModified(object) should probably be overriden and customized, attempting blind grab..."
        }
        if (lm) {
            if (lm instanceof Date) {
                lmd = lm
            } else {
                lmd = parseDateThing(lm)
            }
        } else {
            log.info "No lastModifiedDate property identified for crawled object:($savedObj), nothing to get... failed!"
        }
        return lmd
    }

    Long getSavedSize(Object savedObj) {
        def size = savedObj.size ?: savedObj.size_l
        if (size instanceof Number) {
            return size
        } else {
            String s = savedObj.toString()
            if (s.isNumber()) {
                try {
                    Long l = Long.valueOf(s)
                    return l
                } catch (NumberFormatException nfe) {
                    log.warn "Casting exception: for obj($savedObj) getting 'Long' value from $size -- exception:$nfe"
                }
            } else {
                log.warn "\t\tSize of savedObj(${size}) is not a number (${savedObj.class.name})"
            }
        }
        return null
    }

    Date parseDateThing(Object dateThing, String[] dateFormats = DEFAULT_DATEFORMATS) {
        String dateString = dateThing.toString()
        Date date = null
        try {
            date = DateUtils.parseDate(dateString, dateFormats)
        } catch (ParseException parseException) {
            log.warn "Exception: $parseException"
        }
        return date
    }
//        SimpleDateFormat sdf = new SimpleDateFormat('EEE MMM d HH:mm:ss z yyyy')
//        Date fooDate = sdf.parse(dateString)
//        log.debug "\t\tattempting to parse date string: $dateString"

    @Override
    public String toString() {
        return "BaseDifferenceChecker{}";
    }
}
