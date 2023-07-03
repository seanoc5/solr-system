package com.oconeco.content.structure

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger

/**
 * Class to split content
 *
 * goal is: chapters, sections, paragraphs, sentences, phrases
 * just focusing on paragraphs and sentences to start with
 *
 */
class ContentSplitter {
    static Logger log = LogManager.getLogger("ContentSplitter");

    /**
     * split big text into paragraphs by regex
     * @param content
     * @return
     */
    static List<String> parseParagraphs(String content) {
        List<String> paragraphs = content.split("\\n\\s*\\n+")
    }


    /**
     * Naive section parser,
     * get lines from text and then look for something that is a section heading
     * @param body
     * @return
     */
    static List<List<String>> parseSections(String body) {
        List<List<String>> sections = []

        List<String> lines = body.split(/\n/)
        List<String> currSectionLines = []
        lines.each { String line ->
            if (isSectionHeadingLine(line)) {
                log.info "Found section heading: [[$line]]"
                if(currSectionLines) {
                    log.info "\t\tfound section (size:${currSectionLines.size()} split: $line"
                    sections << currSectionLines
                    currSectionLines = [line]
                } else {
                    log.debug "no current section lines, not adding empty list..."
                    currSectionLines << line
                }
            } else {
                log.debug "\t\tadding line to section(size: ${currSectionLines.size()}): $line "
                currSectionLines << line
            }
        }
        if(currSectionLines){
            sections << currSectionLines
        }

        log.info "Sections size: ${sections.size()}"

        return sections
    }


    /**
     * Assess heading-ness of line
     * @param line
     * @return
     */
    static boolean isSectionHeadingLine(String line) {
        if (line ==~ /\s*[A-Z0-9_ :!.,;-]+/) {
            return true
        }
        return false
    }

}
