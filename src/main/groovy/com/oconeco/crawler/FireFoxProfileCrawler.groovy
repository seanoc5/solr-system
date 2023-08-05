package com.oconeco.crawler

import com.oconeco.difference.BaseDifferenceChecker
import com.oconeco.difference.SolrDifferenceChecker
import com.oconeco.helpers.Constants
import com.oconeco.models.FFPlace
import com.oconeco.persistence.BaseClient
import com.oconeco.persistence.SolrSystemClient
import groovy.sql.GroovyRowResult
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument

import java.sql.*
import org.sqlite.JDBC
import groovy.sql.Sql


//        String dbURL = "jdbc:sqlite:/home/sean/.mozilla/firefox/mwad0hks.default/places.sqlite";

class FireFoxProfileCrawler extends BaseCrawler {
    static Logger log = LogManager.getLogger(this.class.name);
    File sqliteDirectory = null
    Sql dbInstance = null
    Map<String, String> tableMap = [
            'places'       : 'places.sqlite',
            'bookmarks'    : 'places.sqlite',
            'historyvisits': 'places.sqlite',
            'moz_origins'      : 'places.sqlite',
//            'moz_places'       : 'places.sqlite',
//            'moz_bookmarks'    : 'places.sqlite',
//            'moz_'             : 'places.sqlite',
//            'moz_historyvisits': 'places.sqlite',
//            'moz_origins'      : 'places.sqlite',
//            'moz_places': 'places.sqlite',
//            'moz_': 'places.sqlite',
    ]

    static void main(String[] args) {
        String locationName = "seanoc5@gmail.com"
        String dbPath = "/Users/bentc/AppData/Roaming/Mozilla/Firefox/Profiles/fi02zmh7.seanoc5@gmail.com/places.sqlite";
        SolrSystemClient client = new SolrSystemClient()
        FireFoxProfileCrawler crawler = new FireFoxProfileCrawler(locationName, client, new SolrDifferenceChecker(false), dbPath)
        def results = crawler.crawlSqliteDirectory()
        println "Done??"
    }

    FireFoxProfileCrawler(String locationName, BaseClient persistenceClient, BaseDifferenceChecker diffChecker) {
        super(locationName, persistenceClient, diffChecker)
    }

    FireFoxProfileCrawler(String locationName, BaseClient persistenceClient, BaseDifferenceChecker diffChecker, String sqlitePath) throws IllegalArgumentException {
        this(locationName, persistenceClient, diffChecker)
        sqliteDirectory = new File(sqlitePath)
        if (sqliteDirectory?.exists() && sqliteDirectory.canRead()) {
            Class.forName("org.sqlite.JDBC");
            dbInstance = Sql.newInstance("jdbc:sqlite:${sqlitePath}", "org.sqlite.JDBC")
            // todo -- consider limitations of hardcoding this? seems ok, since FF uses sqlite...
            log.info "\t\tFound SQLite dir: ${sqliteDirectory.absolutePath}"
        } else {
            throw new IllegalArgumentException("Could not find existing/readable SQLite folder: $sqlitePath, bailing out!!!")
        }
    }

    int BATCH_SIZE = 100

    def crawlSqliteDirectory() {
        List<FFPlace> places = []
        tableMap.each { String table, String fileName ->
            log.info "Processing table name: $table (file:$fileName)"
            //def places = sql.dataSet("moz_places")
            int i = 0
            dbInstance.eachRow("select * from ${table}") {
                i++
                if (i == 1) {
                    GroovyRowResult rowResult = it.toRowResult()
                    log.debug "Fields: " + rowResult.keySet()
                }
                log.debug(it)
                FFPlace place = new FFPlace(it, null, locationName, table)
                places << place
                if (i % BATCH_SIZE == 0){
                    UpdateResponse response = persistenceClient.saveObjects(places)
                    log.info "$i) Batch Solr update response: $response"
                    places = []
                }
            }
            if (places) {
                UpdateResponse response = persistenceClient.saveObjects(places)
                log.info "Solr update response: $response"
            } else {
                log.info "No extra places list?? finixhed exactly on a batch??? "
            }
        }

    }
}
