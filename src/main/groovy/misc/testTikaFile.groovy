package misc

import groovy.io.FileType
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.tika.config.TikaConfig
import org.apache.tika.detect.Detector
import org.apache.tika.exception.TikaException
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MimeTypes
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler

Logger log = LogManager.getLogger(this.class.name);


/*
AutoDetectParser parser = new AutoDetectParser();
BodyContentHandler handler = new BodyContentHandler();

parser.parse(srcfile.newInputStream(), handler, metadata);

if (metadata) {
    log.debug "metadata: $metadata"
} else {
    log.warn "No metadata? $srcfile -- meta: $metadata"
}
String content = handler.toString()
if (content) {
    log.debug "Content, size(${content.size()})"
} else {
    log.info "No content....!!!"
}
*/
