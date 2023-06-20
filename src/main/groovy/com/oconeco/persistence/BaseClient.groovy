package com.oconeco.persistence

import com.oconeco.models.SavableObject
import org.apache.log4j.Logger

class BaseClient {
    Logger log = Logger.getLogger(this.class.name);

    public static String FLD_ID = 'id'
    public static String FLD_LAST_MODIFIED = "lastModified_dt"        // pdate dynamic field
    public static String FLD_SIZE = "size_l"
    public static String FLD_DEDUP = 'dedup_s'
    public static String FLD_TYPE = 'type_s'
    public static String FLD_PATH_S = 'path_s'
    public static String FLD_CRAWL_NAME = "crawlName_s"
    public static String FLD_LOCATION_NAME = "locationName_s"
    public static String FLD_INDEX_DATETIME = "indexedTime_dt"        // pdate dynamic field

    public static final List<String> DEFAULT_FIELDS_TO_CHECK = [
            FLD_ID,
            FLD_LAST_MODIFIED,
            FLD_SIZE,
            FLD_DEDUP,
            FLD_PATH_S,
            FLD_LOCATION_NAME,
            FLD_INDEX_DATETIME,
//            SolrSystemClient.FLD_,
    ]

    BaseClient() {
    }

    def deleteDocuments(){

    }

    def saveObjects(List<SavableObject> objects){
        log.warn "todo -- complete me"
    }
}
