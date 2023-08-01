package com.oconeco.difference


import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
/**
 * Small class to record differences
 */
class BaseDifferenceStatus {
    Logger log = LogManager.getLogger(this.class.name);
    /** track crawled/new object for debugging/explanation */
    Object crawledObject
    /** track saved/old object for debugging/explanation */
    Object savedDocument

    Boolean differentIds
    Boolean differentLastModifieds
    Boolean differentSizes
    Boolean differentDedups
    Boolean differentPaths
    Boolean differentLocations
    Boolean sourceIsNewer

    /** short-circuit flag for first-time crawls, or crawls after previous crawls have been delete/wiped */
    Boolean noMatchingSavedDoc

    /** track messages/descriptions of differences */
    List<String> differences = []
    /** track messages/descriptions of similarities (mostly debugging/understanding) */
    List<String> similarities = []
    /** property to check if the new/crawled object has significant updates that need to be saved */
    boolean significantlyDifferent = false


    /**
     * Basic status object that a difference checker will update. This is basically a no-action record keeper
     * @param crawledObject -- new/crawled information
     * @param savedDoc -- older/saved information to check for updates/new information
     */
    BaseDifferenceStatus(Object crawledObject, Object savedDoc) {
        this.crawledObject = crawledObject
        if(savedDoc) {
            this.savedDocument = savedDoc
            noMatchingSavedDoc = false
        } else {
            log.debug "Invalid savedDoc object:($savedDoc) -- not setting this.savedDocument nor updating `noMatchingSavedDoc` boolean (leaving as: $noMatchingSavedDoc)"
        }
    }

    String toString() {
        String s = "Diff Status:($crawledObject) "
        if (similarities) {
            s = s + " Similarities count:${similarities.size()} "
        }
        if (differences) {
            s = s + " Differences: ${differences} "
        }
        return s
    }
}
