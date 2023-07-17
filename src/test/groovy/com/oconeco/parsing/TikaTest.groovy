package com.oconeco.parsing

import com.oconeco.helpers.Constants
import com.oconeco.models.FSFile
import com.oconeco.models.FSFolder
import com.oconeco.models.SavableObject
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.tika.Tika
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler
import org.spockframework.compiler.model.ThenBlock
import spock.lang.Specification

import java.util.regex.Pattern

class TikaTest extends Specification {
    Logger log = LogManager.getLogger(this.class.name);

    def "basic tika test"() {
        when:
        File srcfile = new File(getClass().getResource("/content/6_Vector_Apr18_2021.pdf").toURI())
        println "File path: ${srcfile.absolutePath}"

        String content = null
        Tika tika = new Tika();
        try (InputStream stream = srcfile.newInputStream()) {
            content = tika.parseToString(stream);
        } catch (Exception e){
            log.error "Error: $e"
        }

        then:
        content !=null
        content.size() > 0

    }



    def "should create basic FSFile"() {
        when:
        File srcfile = new File(getClass().getResource("/content/6_Vector_Apr18_2021.pdf").toURI())

        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
//        String s = parser.par
        parser.parse(srcfile.newInputStream(), handler, metadata);

        if (metadata) {
            log.debug "metadata: $metadata"
        } else {
            log.warn "No metadata? $srcfile -- meta: $metadata"
        }
        String content = handler.toString()
        long size = content.size()

        if (content) {
            log.debug "Content, size(${content.size()})"
        } else {
            log.info "No content....!!!"
        }

        then:
        content != null
        size > 0

    }


}
