package misc

import com.oconeco.persistence.SolrSystemClient
import javax.mail.Folder;
import com.sun.mail.imap.IMAPFolder
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.SolrInputDocument

import javax.mail.*
//import javax.mail.Flags.Flag
//import javax.mail.internet.InternetAddress

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger

Logger log = LogManager.getLogger(this.class.name)

String collection = 'mail'
String urlString = "http://localhost:8983/solr/${collection}"

log.info "Start: collection: $collection -- urlString: $urlString"
SolrSystemClient client = new SolrSystemClient()

//)
//HttpSolrClient solr = new HttpSolrClient.Builder(urlString).build();
//solr.setParser(new XMLResponseParser());

boolean clearCollection = false
if(clearCollection){
    log.warn "Clearing collection: $collection"
    UpdateResponse ursp = client.deleteDocuments("*:*")
    log.info "Delete response: $ursp"
    SolrQuery query = new SolrQuery()
    query.set("q", "*:*")
    query.setFields("id, *")
    query.setRows(2)        // arbitrary limit to return a couple, but not full 10
    QueryResponse response = client.query(query)
    SolrDocumentList docList = response.getResults()
    log.info "Test query after clear: $docList"
}

IMAPFolder folder = null
Store store = null
String subject = null
//Flag flag = null;


try {
    Properties props = System.getProperties()
    props.setProperty("mail.store.protocol", "imaps")

    Session session = Session.getDefaultInstance(props, null)

    store = session.getStore("imaps")
    String user = 'seanoc5@gmail.com'
//    user = 'sean.oconnor@lucidworks.com'
    user = 'sean.oconnor@stvfd.org'
//    user = 'sean@oconeco.com'
    String account = "imap.googlemail.com"
//    account = 'imap.mail.us-east-1.awsapps.com'
    String password = 'Jax@2023'
//    password = 'obxFam!8'
    store.connect(account, user, password)

//    folder = (IMAPFolder) store.getFolder("[Gmail]/Accounts");    // This doesn't work for other email account
//    folder = (IMAPFolder) store.getFolder("[Gmail]/All Mail");    // This doesn't work for other email account
    String folderName = 'INBOX'
    folder = (IMAPFolder) store.getFolder(folderName)          // This works for both email account

    if (!folder.isOpen()) {
        folder.open(Folder.READ_ONLY)
    }

    Message[] messages = folder.getMessages()
    log.info("No of Messages : " + folder.getMessageCount() + " -- No of Unread Messages : " + folder.getUnreadMessageCount() + " msgs lengthz; ${messages.length}")

    List<SolrInputDocument> sidList = []
    int batchSize = 20
    int batchCount = 0

    boolean cancelCrawl = false
//    for (int i = 0; i < 100; i++) {
    for (int i = 0; i < messages.length && !cancelCrawl; i++) {

        Message msg = messages[i]
        int msgNumber = msg.getMessageNumber()
        subject = msg.getSubject()
        long uid = folder.getUID(msg)
        log.info("$msgNumber:$uid) $subject")

        SolrInputDocument sid = new SolrInputDocument()
        sid.setField('id', "${user}.${uid}")
        sid.setField('message_number_i', msgNumber)
        sid.setField('uid_l', uid)
        sid.setField('username_s', user)

        if (subject) {
            sid.setField('subject_txt_en', subject)
            sid.addField("subject_size_i", subject.size())
        } else {
            log.warn "No subject?? Solr Input doc: ($sid)"
        }

        Address[] froms = msg.getFrom()
        if(froms.size()==1){
            sid.setField('from_t', froms[0])
            sid.setField('from_s', froms[0])
        } else if(froms.size() > 1) {
            log.warn "\t\tMore than one from??? $froms"
            sid.setField('from', froms[0])
            sid.addField('attr_notes', "More than one from address??\n${froms}")
        } else {
            log.warn "\t\tNo from??? $msg"
        }

        [Message.RecipientType.TO, Message.RecipientType.CC, Message.RecipientType.BCC]. each { Message.RecipientType rtype ->
            Address[] addys = msg.getRecipients(rtype)
            if(addys) {
//                if (rtype?.toString()?.contains('c')) {
//                    log.debug "\t\tcopy recipient: ${rtype.toString()}"
//                }
                addys.each { Address addr ->
                    def addrPersonal = addr.getPersonal()
                    if(addrPersonal) {
                        sid.addField("${rtype.toString().toLowerCase()}", addr.getAddress())
                    } else {
                        sid.addField("${rtype.toString().toLowerCase()}", addr.getAddress())
                    }
                }
            }
        }

        sid.addField('to_count_i', sid.getFieldValues('to')?.size() ?: 0)
        sid.addField('cc_count_i', sid.getFieldValues('cc')?.size() ?: 0)

        Enumeration<Header> headers = msg.getAllHeaders()
        int headerCount = 0
        headers.each {Header header ->
            sid.addField("headers", "${header.name}::${header.value}")
            sid.addField("header_name_ss", header.name )
            headerCount++
        }
        sid.addField("header_count_i", headerCount )
        sid.setField('date', msg.getReceivedDate())

        String content = msg.getContent()
        sid.setField('content', content)
        sid.setField('contentType_s', msg.getContentType())
        sid.setField('textSize', content?.size())

        List systemFlags =  msg.getFlags()?.getSystemFlags()
        String flagsString =  msg.getFlags().collect{it.toString()}
//        if(systemFlags.size()>1){
//            log.info "\t\tTodo?"
//        }
        sid.setField('flags', flagsString)

        String[] userFlags = msg.getFlags().getUserFlags()
        sid.setField('flags_user_ss', userFlags)
        if(userFlags.size()>1){
            log.debug "\t\tMore than one user flag???"
        }
        sid.setField('flags_user_count_i',userFlags?.size())

        sidList << sid
        if (sidList.size() % batchSize == 0) {
            batchCount++
            log.info "\t\t$msgNumber) Add bactch: $batchCount"
            def rsp = solr.add(sidList, 2000)
            log.info "Add `response`: $rsp"
            sidList = []
        }

    }

    if(sidList.size() > 0){
        log.info "Save last batch in crawl: ${sidList.size()}"
        solr.add(sidList, 2000)
    }
} catch (Exception e ){
    log.error "Exception: $e"

} finally {
    if (folder != null && folder.isOpen()) {
        log.info "Close folder: $folder"
        folder.close(false)
    }
    if (store != null) {
        log.info "Close store: $store"
        store.close()
    }
}

log.info "Done"
