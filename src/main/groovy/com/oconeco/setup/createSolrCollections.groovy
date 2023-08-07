package com.oconeco.setup

import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSystemClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition
import org.apache.solr.client.solrj.request.schema.SchemaRequest
import org.apache.solr.client.solrj.response.CollectionAdminResponse
import org.apache.solr.client.solrj.response.schema.SchemaResponse
import org.apache.solr.common.params.ModifiableSolrParams
import org.apache.solr.common.util.NamedList

/**
 * quick hack/pass at setting up core collections
 * todo -- move this to more rigorous (or flexible??) approach
 */

Logger log = LogManager.getLogger(this.class.name);
log.info "Starting script: ${this.class.name}..."

ArrayList<String> collsNeeded = [
        Constants.DEFAULT_COLL_NAME,
        'system_analysis',
        'system_vocabulary',
        'system_concepts',
        'system_xlogs',
//        'system_',
]

SolrSystemClient client = new SolrSystemClient()

CollectionAdminResponse collectionsResponse = client.getCollections()
List existingCollections = collectionsResponse.getResponse().collections

collsNeeded.each { String neededColl ->
    log.debug "Need Collection: $neededColl:"
//    def foundColl = existingCollections.find {neededColl}
    def found = existingCollections.contains(neededColl)
    if (found) {
        log.info "\t\t Needed coll:($neededColl) found in existing collections, nothing further to do!"
    } else {
        log.info "\t\t Needed coll:($neededColl) NOT FOUND in existing collections, create it now..."
    }
}
FieldTypeDefinition ftDef = new FieldTypeDefinition()
Map<String, Object> fieldTypeAttributes = [:]
String fieldTypeName = "shingle_basic";
fieldTypeAttributes.put("name", fieldTypeName);
fieldTypeAttributes.put("class", "solr.TextField");
fieldTypeAttributes.put("stored", false);
fieldTypeAttributes.put("positionIncrementGap", 100);

FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
fieldTypeDefinition.setAttributes(fieldTypeAttributes);

SchemaRequest.AddFieldType addFieldTypeRequest = new SchemaRequest.AddFieldType(fieldTypeDefinition);
SchemaResponse.UpdateResponse addFieldTypeResponse = addFieldTypeRequest.process(solrClient)


// bad Bito AI suggestionm??
//SchemaRequest sr = SchemaRequest.AddFieldType
//String fieldType = "text_custom"; // Replace with your field type name
//String fieldClass = "solr.TextField"; // Replace with your field class
//List<String> fieldArgs =  ["indexed" , "stored" , "multiValued" ]

//SolrClient solrClient = new HttpSolrClient.Builder(solrUrl).build();
//ModifiableSolrParams params = new ModifiableSolrParams();
//params.set("updateTimeoutSecs", 10);
//
//SchemaRequest.AddField fieldTypeAddRequest = new SchemaRequest.AddField(fieldType, fieldClass, fieldArgs);
//SchemaRequest.AddFieldType fieldTypeUpdateRequest = new SchemaRequest.AddFieldType(fieldTypeAddRequest);
//
//NamedList<Object> response = solrClient.request(fieldTypeUpdateRequest);


log.info "Done...?"
