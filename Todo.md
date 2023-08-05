# Todo tracking

## Crawling

- process archive files
- compare saved vs current directories (name/path/size/date)
- Categorization _(aka analysis)_ by name/path pattern
  - folders
  - files
    - extract/index contents
      - text files
      - deeper content analysis
- Clustering
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
