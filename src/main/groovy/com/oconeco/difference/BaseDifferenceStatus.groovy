package com.oconeco.difference

import com.oconeco.models.SavableObject
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger

/**
 * Small class to record differences
 */
class BaseDifferenceStatus {
    Logger log = LogManager.getLogger(this.class.name);
    SavableObject object
    def savedDocument

    Boolean differentIds
    Boolean differentLastModifieds
    Boolean differentSizes
    Boolean differentDedups
    Boolean differentPaths
    Boolean differentLocations

    Boolean sourceIsNewer
    Boolean noMatchingSavedDoc
//    List<String> messages = []
    List<String> differences = []
    List<String> similarities = []
    boolean significantlyDifferent = false


    BaseDifferenceStatus(SavableObject object, Object savedDoc) {
        this.object = object
        this.savedDocument = savedDoc
    }

    String toString() {
        String s = "Diff Status:($object) "
        if (similarities) {
            s = s + " Similarities:${similarities} "
        }
        if (differences) {
            s = s + " Differences: ${differences} "
        }
        return s
    }
}
