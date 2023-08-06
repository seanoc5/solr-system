# Todo tracking

- rename SolrSys `Analysis` to `Process`
- `Solr System` module (and `solr_system` collection) includes basic processing
  - name & path regex labeling
  - simple `locate` and/or basic (tika) extraction and standard `solr` search capabilities
  - basic vocabulary tools (not much implementation, just the tools & setup)
    - terms: component (vector fields & tvrh placeholders in `system_analysis` collection, main processing in system_analysis module)
      - domain words (??) - explicit list of words that have _special_ meaning
      - skip words - similar to stop words but they are kept, indexed and general available, but skipped for more advanced processing and analysis
  - content fields:
    - name*, 
    - path*, 
    - content*,
    - summary* (consider both **simple** summary, and **generated** summary
- `system_vocabulary` - supporting collection to hold (dynamic) vocabulary terms/shingles
  - `text` - word or shingle/phrase 
  - `lemmas`
  - `word sense` - currently based on Wordnet
- `system_concepts` _(or system_taxonomy ??)_
  - `label` - concept name/label
  - sample concepts (high level):
    - tech: 
      - equipment/hardware
      - tools/software
      - techniques (code/libraries)
    - political: 
      - left/right/center
      - concerns:
        - role of government (benefits, limitations, 
    - personal/productivity
    - fact checking: 
- `system_sources` - information and points for sources of information (sites, tools,...)
  - search engines - google, bing, other....
  - sites (to crawl)
    - tech: stack overflow, reddit,....
      - content analysis
    - political: wsj, wapo, nyt, cnn, google,....
    - personal/productivity
    - fact checking: 
- `system_analysis` is the more advanced collection for 'deeper' analysis
  - mostly placeholder for `System Analysis` module (licensed/non-free)
  - `Solr System` ...?
  - Basic `vocabulary processing`
    - terms component _(? other basic tools?)_
    - CRUD
      - `uncommon` words/shingles - 
      - `domain` words (`domain`, `word/term`, and `word sense identifier`)
      - `skip` words

## Crawling

- ~~process archive files~~
- ~~compare saved vs current directories (name/path/size/date)~~
- `Labeling`  by name/path pattern
  - ~~folders~~
  - ~~files~~
    - ~~extract/index contents~~
      - ~~text files~~
- Basic content analysis
  - `domain words` & `skip words` processing
- Clustering
  - basic solr clustering (term vectors??)
- Bookmarks
  - try Google cache or Wayback machine for 404 and missing content


## Notes and various
- crawlName logic and 'wipe'
  - if crawlname different, does update/wipe work properly??
  - ExtPasswort vs ExtPassport...???


## Adjust spurious logs

### PDFBox / fonts...
// https://stackoverflow.com/questions/58180179/disable-pdfbox-logging-with-springboot-org-apache-commons-logging   ??? todo -- is there a better approach? probably

`
java.util.logging.Logger.getLogger("org.apache.pdfbox").setLevel(java.util.logging.Level.OFF);
List<String> loggers = [
        "org.apache.pdfbox.util.PDFStreamEngine",
        "org.apache.pdfbox.pdmodel.font.PDSimpleFont",
        "httpclient.wire.header",
        "httpclient.wire.content"]
for (String ln : loggers) {
    // Try java.util.logging as backend
    java.util.logging.Logger.getLogger(ln).setLevel(java.util.logging.Level.WARNING);
    // Try Log4J as backend
//    LogManager.getLogger(ln).lev  setLevel(org.apache.log4j.Level.WARN);
    // Try another backend
    Log4JLoggerFactory.getInstance().getLogger(ln).setLevel(java.util.logging.Level.WARNING);
}
`
