import groovy.io.FileType
import org.apache.log4j.Logger
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument
import org.apache.tika.exception.TikaException
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler

Logger log = Logger.getLogger(this.class.name);

AutoDetectParser parser = new AutoDetectParser();
BodyContentHandler handler = new BodyContentHandler(-1);
File srcDir = new File("/home/sean/work/oconeco/wealth-view/bea/emails")
String host = 'localhost'
int port = 8983
String collection = 'research'
String baseUrl = "http://$host:$port/solr/$collection"
SolrClient solrClient = new HttpSolrClient(baseUrl)

srcDir.eachFileRecurse(FileType.FILES) { File srcfile ->
    log.info "File: $srcfile"
    String path = srcfile.absolutePath
    String name = srcfile.name


    SolrInputDocument sid = new SolrInputDocument()
    sid.setField('id', path)
    sid.setField('path_t', path)

    List<String> parts = path.split('/')  // todo - switch to OS relevant splitter
    if (parts) {
        String parentFolder = parts[-1]
        sid.setField('parentFolder', parentFolder)
        sid.setField('parentFolder_s', parentFolder)
    } else {
        log.warn "No parent info: $path"
    }
    parts = name.split('\\.')
    if (parts) {
        String ext = parts[-1]
        sid.setField('ext', ext)
    } else {
        log.warn "No extension? $path"
    }

    sid.setField('fileSize_l', srcfile.size())
    Date lmdate = new Date(srcfile.lastModified())
    sid.setField('lastModified_tdt', lmdate)

    try {
        Metadata metadata = new Metadata();
        parser.parse(srcfile.newInputStream(), handler, metadata);

        if (metadata) {
            sid.setField('metadata_txt', metadata)
        } else {
            log.warn "No metadata? $srcfile -- meta: $metadata"
        }
        String content = handler.toString()
        sid.addField("content_txt", content)
    } catch (TikaException tikaException) {
        log.error "Tika exception: $tikaException"
    }
    UpdateResponse ursp = solrClient.add(sid, 3000)
    log.info "\t\tResponse: $ursp"
}
