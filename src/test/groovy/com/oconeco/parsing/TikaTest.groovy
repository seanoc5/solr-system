package com.oconeco.parsing

import com.oconeco.helpers.Constants
import com.oconeco.models.FSFile
import com.oconeco.models.FSFolder
import com.oconeco.models.SavableObject
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.tika.Tika
import org.apache.tika.config.TikaConfig
import org.apache.tika.detect.Detector
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import org.apache.tika.metadata.TikaCoreProperties
import org.apache.tika.mime.MediaType
import org.apache.tika.mime.MimeTypes
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.parser.Parser
import org.apache.tika.parser.RecursiveParserWrapper
import org.apache.tika.sax.BasicContentHandlerFactory
import org.apache.tika.sax.BodyContentHandler
import org.apache.tika.sax.RecursiveParserWrapperHandler
import org.spockframework.compiler.model.ThenBlock
import spock.lang.Specification

import java.util.regex.Pattern

import static org.apache.tika.metadata.Metadata.*

class TikaTest extends Specification {
    Logger log = LogManager.getLogger(this.class.name);
    File srcFile = new File(getClass().getResource('/content/combinedTestContent.tgz').toURI())
//    File srcFile = new File(getClass().getResource('/content/FutureTechPredictions.doc').toURI())

    def "tika should detect doc type"() {
        given:
        log.info "File: $srcFile"
        String path = srcFile.absolutePath
        String name = srcFile.name

        when:
        TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
        Metadata metadata = new Metadata();
        MimeTypes mimeRegistry = tikaConfig.getMimeRepository()
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, srcFile.name)

        //TikaInputStream sets the TikaCoreProperties.RESOURCE_NAME_KEY when initialized with a file or path
        String typeName = tikaConfig.getDetector().detect(null, metadata);
        log.info("Filename mimeType $typeName")

        Metadata mdata = new Metadata()
        //if you know the file name, it is a good idea to set it in the metadata, e.g.
        mdata.set(TikaCoreProperties.RESOURCE_NAME_KEY, srcFile.name)
        TikaInputStream tis = TikaInputStream.get(srcFile.newInputStream())
        MediaType typeStream = tikaConfig.getDetector().detect(tis, mdata);
        log.info("Stream $typeStream")
        log.info("Stream $tis is $typeStream")

//        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, srcFile);
//        MediaType typeRegistry = mimeRegistry.detect(null, metadata)
//        log.info("The MIME type (based on srcFile) is: [${typeRegistry}]")
//
//        InputStream stream = TikaInputStream.get(new File(srcFile));
//        log.info("The MIME type (based on MAGIC) is: [${mimeRegistry.detect(stream, metadata)}]")
//
//        stream = TikaInputStream.get(new File(srcFile));
//        Detector detector = tikaConfig.getDetector();
//        MediaType typeStream = detector.detect(stream, metadata)
//        log.info("The MIME type (based on the Detector interface) is: [${typeStream}]");

        then:
        mimeRegistry != null
        typeName != null
        typeStream != null
    }

    def "get archive files metadata"() {
        given:
        log.info "File: $srcFile"
        String path = srcFile.absolutePath
        String name = srcFile.name

        when:
        TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
        Metadata metadata = new Metadata();

        Parser p = new AutoDetectParser();

        BasicContentHandlerFactory factory = new BasicContentHandlerFactory(BasicContentHandlerFactory.HANDLER_TYPE.IGNORE, -1);
        RecursiveParserWrapper wrapper = new RecursiveParserWrapper(p);
        metadata.set(TikaCoreProperties.ORIGINAL_RESOURCE_NAME, name);
        ParseContext context = new ParseContext();
        RecursiveParserWrapperHandler handler = new RecursiveParserWrapperHandler(factory, -1);
        try (InputStream stream = new FileInputStream(srcFile)) {
            wrapper.parse(stream, handler, metadata, context);
            log.info "Metadata: $metadata"
        }
        log.info handler.getMetadataList()

        then:
//        mimeRegistry != null
        handler != null
    }

    def "basic tika test"() {
        when:
        File srcfile = new File(getClass().getResource("/content/6_Vector_Apr18_2021.pdf").toURI())
        println "File path: ${srcfile.absolutePath}"

        String content = null
        Tika tika = new Tika();
        try (InputStream stream = srcfile.newInputStream()) {
            content = tika.parseToString(stream);
        } catch (Exception e) {
            log.error "Error: $e"
        }

        then:
        content != null
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
