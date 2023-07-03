package misc.old
//import com.squareup.okhttp.HttpUrl
//import com.squareup.okhttp.OkHttpClient
//import com.squareup.okhttp.Request
//import com.squareup.okhttp.Response
//import groovy.json.JsonSlurper
//import org.apache.logging.log4j.core.Logger
//
//Logger log = LogManager.getLogger(this.class.name);
//
//log.info "Start ${this.class.name}"
//
//// avoid creating several instances, should be singleon
//OkHttpClient client = new OkHttpClient();
//
//HttpUrl.Builder urlBuilder = HttpUrl.parse("http://newmac:8983/solr/lucy/select").newBuilder();
//urlBuilder.addQueryParameter("q", "sean");
//urlBuilder.addQueryParameter("fl", "id");
//String url = urlBuilder.build().toString();
//
//Request request = new Request.Builder()
//                     .url(url)
//                     .build();
//JsonSlurper jsonSlurper = new JsonSlurper()
//
//Response response = client.newCall(request).execute();
//log.info "Response: $response"
//
//String rspBody = response.body().string();
//log.debug "Response body: $rspBody"
//
//Map json = jsonSlurper.parseText(rspBody)
//log.debug "Json: $json"
//int numFound = json.response.numFound
//int start = json.response.start
//List docs = json.response.docs
//log.info "Solr docs: $docs"
//
//log.info "done!?"
