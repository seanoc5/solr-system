package com.oconeco.models

import com.oconeco.persistence.SolrSystemClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.solr.common.SolrInputDocument
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

//import org.jsoup.nodes.Document
//import org.jsoup.select.Elements
//import org.jsoup.nodes.Element

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

/**
 * util class to encapsulate Firefox bookmark info (starting with exported bookmarks file)
 */
class FFBookmark extends FFObject {
    public static final String ERROR_MESSAGE = "ERROR MESSAGE!"
    Logger log = LogManager.getLogger(this.class.name)
    public static final String TYPE = 'FFBookmark'
    long MIN_CACHED_FILE_SIZE = 100      // todo - revisit to see if this should be larger/smaller

    FFBookmark(def object, SavableObject parent, String locationName, String crawlName) {
        super(object, parent, locationName, crawlName)
        type = TYPE

        String t = object.tags
        if (t) {
            tags = t.split(',')
        }
        this.ffId = object.id
        mimeType = object.type
        path = object.uri

        dedup = buildDedupString()
    }


    SolrInputDocument toPersistenceDocument(File cacheFolder) {
        SolrInputDocument solrInputDocument = super.toPersistenceDocument()
        File cachedFile = getCachedContent(cacheFolder)
        String msg = null

        try {
            URI uri = new URI(path)
            if (uri) {
                boolean abs = uri.isAbsolute()
                String host = uri.host
                String auth = uri.authority
                if (auth) {
                    log.debug "\t\t++++auth:$auth --  URI: $uri -- absolute:$abs) host:$host -- "
                    if (uri) {
                        solrInputDocument.addField(SolrSystemClient.FLD_URI_S, uri)
                        solrInputDocument.addField(SolrSystemClient.FLD_URI_T, uri)
                    } else {
                        log.warn "no URI ($uri) for this object/bookmark:($this)"
                    }
                    if (cachedFile.exists()) {
                        log.info "\t\tusing cached file:(${cachedFile.name}) rather than trying to (re)download it for this:$this"
                        String content = extractCachedInfo(cachedFile, solrInputDocument)
                    } else {
                        String html = extractHtmlInfo(solrInputDocument)
                        cachedFile.text = html
                        log.debug "\t\t\t\twriting Cache ($cachedFile): html size:${html.size()} -- file size:${cachedFile.size()}"
                    }

                } else {
                    log.info "---- No authority/host, skipping content retrieval... this:($this)"
                }

            } else {
                log.warn "No valid uri:($uri) from path:($path) for this:$this"
            }
        } catch (URISyntaxException uriSyntaxException) {
            log.warn "Could not convert path($path) to uri, skipping content extraction for this:($this)"
        } catch (IOException ioe) {
            msg = "IO Problems: exception: $ioe"
            solrInputDocument.addField(SolrSystemClient.FLD_NOTES, msg)
            log.warn msg
        } catch (MalformedURLException mue) {
            msg = "Not a valid uri($uri) -> url:($URL) conversion??? -- exception: $mue"
            solrInputDocument.addField(SolrSystemClient.FLD_NOTES, msg)
            log.warn msg
        } catch (HttpStatusException httpStatusException) {
            msg = "Could not connecto to url:($URL)? Exception: $httpStatusException"
            solrInputDocument.addField(SolrSystemClient.FLD_NOTES, msg)
            log.warn msg
        }
        if (msg) {
            log.info ERROR_MESSAGE + " -- Appending it to the cache file (to streamline future attempts and avoid trying to read non-existent pages. --Msg:[[$msg]]"
            cachedFile << msg
        }

        return solrInputDocument
    }

    public String extractHtmlInfo(SolrInputDocument solrInputDocument) throws IOException {
        Document jsoupDoc = Jsoup.connect(path).get();
        Elements links = jsoupDoc.select("a[href]");
        links.each {
            solrInputDocument.addField('attr_links', it.toString())
            solrInputDocument.addField('attr_link_text', it.text())
            solrInputDocument.addField('attr_hrefs', it.attr('href'))
        }
        String c = jsoupDoc.body().text()
        solrInputDocument.setField(SolrSystemClient.FLD_CONTENT_BODY, c);
        String html = jsoupDoc.html()
        solrInputDocument.setField('html_t', html);
        log.info "\t\t\t\textracted content size(${c?.size()}) -- html size:(${html?.size()})"
        return html
    }

    public String extractCachedInfo(File cachedFile, SolrInputDocument solrInputDocument) throws IOException {
        String cachedContent = cachedFile.text
        if (cachedContent.size() < MIN_CACHED_FILE_SIZE) {
            log.warn "Cached file content too small, putting content in notes, probably need a more advanced crawl, or remove empty/useless cache file: $cachedFile (cached content: $cachedContent)"
            solrInputDocument.addField(SolrSystemClient.FLD_NOTES, "CACHED CONTENT too small:\n\n$cachedContent")
        } else {
            String heading = cachedContent[0..MIN_CACHED_FILE_SIZE]
            if (heading.contains(ERROR_MESSAGE)) {
                log.warn "Cached file starts with ($ERROR_MESSAGE) label, putting content in notes, probably need a more advanced crawl, or remove empty/useless cache file: $cachedFile (cached content: $cachedContent)"
                solrInputDocument.addField(SolrSystemClient.FLD_NOTES, "CACHED CONTENT too small:\n\n$cachedContent")
            } else {
                Document jsoupDoc = Jsoup.parse(cachedContent);
                Elements links = jsoupDoc.select("a[href]");
                links.each {
                    solrInputDocument.addField('attr_links', it.toString())
                    solrInputDocument.addField('attr_link_text', it.text())
                    solrInputDocument.addField('attr_hrefs', it.attr('href'))
                }
                String c = jsoupDoc.body().text()
                solrInputDocument.setField(SolrSystemClient.FLD_CONTENT_BODY, c);
                String html = jsoupDoc.html()
                if (c.size() < html.size()) {
                    log.debug "\t\tHtml size:(${html.size()}) greater than jsoup text output (${c.size()}), things look good for this:$this"
                    solrInputDocument.setField('html_t', html);
                } else {
                    log.warn "Html size(${html.size()}) NOT GREATEER than jsoup text output (${c.size()}), things look questionable for this:$this"
                }
                log.info "\t\t\t\textracted content size(${c?.size()}) -- html size:(${html?.size()})"
            }
        }
        return cachedContent
    }

    @Override
    String buildDedupString() {
        String d = "$type:$locationName:$path"
    }

    @Override
    String getType() {
        FFBookmark.TYPE
    }

    File getCachedContent(File cacheFolder) {
        String filename = this.guid ?: this.ffId
        File file = new File(cacheFolder, filename + '.txt')
    }

}
