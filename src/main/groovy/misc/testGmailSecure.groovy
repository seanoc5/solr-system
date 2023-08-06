//package misc
//
//import java.util.Properties;
//import javax.mail.Authenticator;
//import javax.mail.Message;
//import javax.mail.MessagingException;
//import javax.mail.PasswordAuthentication;
//import javax.mail.Session;
//import javax.mail.Transport;
//import javax.mail.internet.InternetAddress;
//import javax.mail.internet.MimeMessage;
//
//Properties props = new Properties();
//props.put("mail.smtp.auth", "true");
//props.put("mail.smtp.starttls.enable", "true");
//// smtp.gmail.com supports TLSv1.2 and TLSv1.3
//// smtp.office365.com supports only TLSv1.2
//props.put("mail.smtp.ssl.protocols", "TLSv1.2");
//props.put("mail.smtp.host", "smtp.office365.com");
//props.put("mail.smtp.port", "587");
//
//Session session = Session.getInstance(props, new Authenticator() {
//    @Override
//    protected PasswordAuthentication getPasswordAuthentication() {
//        return new PasswordAuthentication("test@example.com", "******");
//    }
//});
//
//try {
//    Message message = new MimeMessage(session);
//    message.setFrom(new InternetAddress("sender@example.com"));
//    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("recipient@example.com"));
//    message.setSubject("You got mail");
//    message.setText("This email was sent with JavaMail.");
//    Transport.send(message);
//    System.out.println("Email sent.");
//} catch (MessagingException e) {
//    throw new RuntimeException(e);
//}
//
