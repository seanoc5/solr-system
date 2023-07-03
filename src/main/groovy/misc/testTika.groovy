package misc

import groovy.io.FileType

//import org.apache.log4j.Logger

import org.apache.logging.log4j.core.Logger
import org.apache.tika.exception.TikaException
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler

Logger log = Logger.getLogger(this.class.name);

AutoDetectParser parser = new AutoDetectParser();
BodyContentHandler handler = new BodyContentHandler(-1);
File srcDir = new File("/home/sean/work/OconEco/data/munis/2012_Individual_Unit_file")
//File srcDir = new File("/home/sean/work/OconEco/data/munis")

srcDir.eachFileRecurse(FileType.FILES) { File srcfile ->
    log.info "File: $srcfile"
    String path = srcfile.absolutePath
    String name = srcfile.name

    try {
        Metadata metadata = new Metadata();
        parser.parse(srcfile.newInputStream(), handler, metadata);

        if (metadata) {
            log.debug "metadata: $metadata"
        } else {
            log.warn "No metadata? $srcfile -- meta: $metadata"
        }
        String content = handler.toString()
        if(content) {
            log.info "Content, size(${content.size()}"
        } else {
            log.info "No content....!!!"
        }
    } catch (TikaException tikaException) {
        log.error "Tika exception: $tikaException"
    }
}
