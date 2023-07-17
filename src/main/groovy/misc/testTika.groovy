package misc

import groovy.io.FileType

import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.LogManager
import org.apache.tika.config.TikaConfig
import org.apache.tika.exception.TikaException
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MediaType
import org.apache.tika.mime.MediaTypeRegistry
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.CompositeParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.parser.Parser
import org.apache.tika.sax.BodyContentHandler

Logger log = LogManager.getLogger(this.class.name);


// Get your Tika Config, eg
TikaConfig config = TikaConfig.getDefaultConfig();

// Get the registry
MediaTypeRegistry registry = config.getMediaTypeRegistry();
// List
for (MediaType type : registry.getTypes()) {
   String typeStr = type.toString();
}

//// Get the root parser
//CompositeParser cparser = (CompositeParser)parser.getParser();
//// Fetch the types it supports
//for (MediaType type : cparser.getSupportedTypes(new ParseContext())) {
//   String typeStr = type.toString();
//}
//// Fetch the parsers that make it up (note - may need to recurse if any are a CompositeParser too)
//for (Parser p : cparser.getAllComponentParsers()) {
//   String parserName = p.getClass().getName();
//   if (p instanceof CompositeParser) {
//       log.info "Parser: ($parserName) -- $p"
//   }
//}

File srcDir = new File("C:/Users/bentc/Documents/")
//File srcDir = new File(getClass().getResource('/content').toURI())
//File srcDir = new File("/home/sean/work/OconEco/data/munis/2012_Individual_Unit_file")
//File srcDir = new File("/home/sean/work/OconEco/data/munis")

srcDir.eachFileRecurse(FileType.FILES) { File srcfile ->
    log.info "File: $srcfile"
    String path = srcfile.absolutePath
    String name = srcfile.name

    try {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        parser.parse(srcfile.newInputStream(), handler, metadata);

        if (metadata) {
            log.debug "metadata: $metadata"
        } else {
            log.warn "No metadata? $srcfile -- meta: $metadata"
        }
        String content = handler.toString()
        if(content) {
            log.debug "Content, size(${content.size()})"
        } else {
            log.info "No content....!!!"
        }
    } catch (TikaException tikaException) {
        log.error "Tika exception: $tikaException"
    }
}
