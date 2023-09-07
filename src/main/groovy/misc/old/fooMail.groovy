package misc.old
/**
 * Created by sean on 12/3/14.
 */
// http://www.technipelago.se/blog/show/groovy-imap
// http://dmitrijs.artjomenko.com/2012/03/how-to-read-imap-email-with-groovy.html


import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPMessage
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument

import javax.mail.*
import javax.mail.search.AndTerm
import javax.mail.search.ComparisonTerm
import javax.mail.search.ReceivedDateTerm
import javax.mail.search.SearchTerm

Logger log = LogManager.getLogger(this.class.name)
// TODO -- revisit -- BROKEN????

Properties props = new Properties()
props.setProperty("mail.store.protocol", "imap")
props.setProperty("mail.imap.host", 'mail.oconeco.com')

Properties defaultProps = new Properties(["mail.store.protocol": "imaps", "mail.imaps.host": "imap.gmail.com", "mail.imaps.port": "993"])

int batchSize = 50
Map accountsMap = [
        'imap.gmail.com'  : [username: 'sean.oconnor@foo.org', password: 'mypass', props: defaultProps, folders: ['Inbox', 'Sent']]
]

Date endDate = new Date()     // now
Date startDate = new Date(2023,1,1)

SearchTerm olderThan = new ReceivedDateTerm(ComparisonTerm.LT, endDate)
SearchTerm newerThan = new ReceivedDateTerm(ComparisonTerm.GT, startDate)
SearchTerm filterSearchTerm = new AndTerm(olderThan, newerThan)

Session session = Session.getDefaultInstance(defaultProps, null)
Store store = session.getStore("imaps")

String solrUrl = 'http://localhost:8080/solr5/communications/'
//SolrServer server = new HttpSolrServer(solrUrl)
String host = 'localhost'
int port = 8983
String collection = 'solr_system'
String baseUrl = "http://$host:$port/solr/$collection"
SolrClient client = new Http2SolrClient.Builder(baseUrl).build()

List<SolrInputDocument> solrDocs = []

FetchProfile fp = new FetchProfile()
fp.add(FetchProfile.Item.ENVELOPE)
fp.add(IMAPFolder.FetchProfileItem.FLAGS)
fp.add(IMAPFolder.FetchProfileItem.CONTENT_INFO)
fp.add(IMAPFolder.FetchProfileItem.HEADERS)
fp.add(IMAPFolder.FetchProfileItem.SIZE)
fp.add("X-mailer")

accountsMap.each { String accountName, Map accountInfoMap ->

    store.connect(accountName, accountInfoMap.username, accountInfoMap.password)
    Folder[] folders = store.getDefaultFolder().list()
    log.info "Account: $accountName -- folders: $folders"

    accountInfoMap.folders.each { String folderName ->
        IMAPFolder mailboxFolder = store.getFolder(folderName)
        log.info "\t\tFolder: $folderName -> ${mailboxFolder.getNewMessageCount()} new messages of ${mailboxFolder.getMessageCount()} total messages"
        mailboxFolder.open(Folder.READ_ONLY)
        Message[] messages = mailboxFolder.search(filterSearchTerm)

        log.info "\t found ${messages.size()} november messages..."
        messages.each { IMAPMessage msg ->
            Date msgDate = msg.getSentDate()
            Address from = msg.getSender()
            Address[] tos = msg.getRecipients(Message.RecipientType.TO)
            Address[] ccs = msg.getRecipients(Message.RecipientType.CC)
            int size = msg.getSize()
            def inreplyTo = msg.getHeader('In-Reply-To')
            Flags flags = msg.getFlags()
            log.info "${msgDate}: ${from} -> to:$tos ==> subject:${msg.getSubject()}"
            if (inreplyTo || flags.userFlags) {
                log.debug "\t\t interesting meta data, flags:${flags.userFlags} ${inreplyTo ? "-- inreplyTo:" + inreplyTo : ''}"
            }
            SolrInputDocument sid = new SolrInputDocument()
            sid.setField('id', msg.messageID)
            sid.setField('messageId', msg.messageID)
            sid.setField('account', accountName)
            sid.setField('folder', folderName)
            sid.setField('number', msg.getMessageNumber())
            sid.setField('flags', flags.userFlags)

            sid.setField('to', tos)
            sid.setField('from', from)
            sid.setField('cc', ccs)
            sid.setField('subject', msg.getSubject())
            sid.setField('textSize', msg.getSize())

            String content = getText(msg)
            sid.setField('content', content)

            solrDocs << sid
            if(solrDocs.size() >= batchSize){
                log.info "commiting batch: ${solrDocs.size()}"
                server.add(solrDocs)
                solrDocs = []
            }
        }

        mailboxFolder.close(false)
        UpdateResponse ur = server.add(solrDocs)
        log.info "update response: $ur"
    }
    server.commit()
    store.close()
}


textIsHtml = false

/**
 * Return the primary text content of the message.
 */
private String getText(Part p) throws MessagingException, IOException {
    if (p.isMimeType("text/*")) {
        String s = (String)p.getContent()
        textIsHtml = p.isMimeType("text/html")
        return s
    }

    if (p.isMimeType("multipart/alternative")) {
        // prefer html text over plain text
        Multipart mp = (Multipart)p.getContent()
        String text = null
        for (int i = 0; i < mp.getCount(); i++) {
            Part bp = mp.getBodyPart(i)
            if (bp.isMimeType("text/plain")) {
                if (text == null)
                    text = getText(bp)
                continue
            } else if (bp.isMimeType("text/html")) {
                String s = getText(bp)
                if (s != null)
                    return s
            } else {
                return getText(bp)
            }
        }
        return text
    } else if (p.isMimeType("multipart/*")) {
        Multipart mp = (Multipart)p.getContent()
        for (int i = 0; i < mp.getCount(); i++) {
            String s = getText(mp.getBodyPart(i))
            if (s != null)
                return s
        }
    }

    return null
}
