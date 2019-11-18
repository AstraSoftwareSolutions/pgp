package mail;

import java.io.IOException;  
import java.util.Properties;  

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Folder;  
import javax.mail.Message;  
import javax.mail.MessagingException;  
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;  
import javax.mail.Part;
import javax.mail.Session;  
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import com.sun.mail.pop3.POP3Store;  

import com.didisoft.pgp.mail.*;

public class ReceiveEncryptedMail {

	private boolean textIsHtml = false;
	
	private String getText(Part p) throws MessagingException, IOException {
		if (p.isMimeType("text/*")) {
			String s = (String) p.getContent();
			textIsHtml = p.isMimeType("text/html");
			return s;
		}

		if (p.isMimeType("multipart/alternative")) {
			// prefer html text over plain text
			Multipart mp = (Multipart) p.getContent();
			String text = null;
			for (int i = 0; i < mp.getCount(); i++) {
				Part bp = mp.getBodyPart(i);
				if (bp.isMimeType("text/plain")) {
					if (text == null)
						text = getText(bp);
					continue;
				} else if (bp.isMimeType("text/html")) {
					String s = getText(bp);
					if (s != null)
						return s;
				} else {
					return getText(bp);
				}
			}
			return text;
		} else if (p.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) p.getContent();
			for (int i = 0; i < mp.getCount(); i++) {
				String s = getText(mp.getBodyPart(i));
				if (s != null)
					return s;
			}
		}

		return null;
	}	
	
	public void receiveEmail(String pop3Host, 
									String user, 
									String password) {

	 try {
	   Properties properties = new Properties();  
	   properties.put("mail.pop3.host", pop3Host);  
	   Session emailSession = Session.getDefaultInstance(properties);  
	     
	   //2) create the POP3 store object and connect with the pop server  
	   POP3Store emailStore = (POP3Store) emailSession.getStore("pop3");  
	   emailStore.connect(user, password);  
	  
	   //3) create the folder object and open it  
	   Folder emailFolder = emailStore.getFolder("INBOX");  
	   emailFolder.open(Folder.READ_ONLY);  
	  
	   //4) retrieve the messages from the folder in an array and print it  
	   Message[] messages = emailFolder.getMessages();  
	   for (int i = 0; i < messages.length; i++) {  
	    Message message = messages[i];  
	    System.out.println("---------------------------------");  
	    System.out.println("Email Number " + (i + 1));  
	    System.out.println("Subject: " + message.getSubject());  
	    System.out.println("From: " + message.getFrom()[0]);  
	    System.out.println("Text: h" + message.getContent().toString());
	    
	    PGPMailLib mailUtil = new PGPMailLib();
	    if (mailUtil.isOpenPGPEncrypted(message)) {	    	
	    	try {
	    		MimeMessage decrypted = mailUtil.decryptMessage(emailSession, (MimeMessage)message, "examples/DataFiles/private.key", "changeit");	    		
	    		Object decryptedRawContent = decrypted.getContent();
	    		// PGP/MIME decrypted should be multipart
	    		if (decryptedRawContent instanceof Multipart) {
	    			Multipart multipart = (Multipart) decryptedRawContent;

	    	         for (int j = 0; j < multipart.getCount(); j++) {
	    	          BodyPart bodyPart = multipart.getBodyPart(j);
	    	          
	    	          String disposition = bodyPart.getDisposition();
	    	          if (disposition != null && (disposition.equalsIgnoreCase("ATTACHMENT"))) { 
	    	              System.out.println("Mail have some attachment");

	    	              DataHandler handler = bodyPart.getDataHandler();
	    	              System.out.println("file name : " + handler.getName());
	    	              // file saving code here
	    	            }
	    	          else {
	    	        	  // print decrypted message
	    	        	  String content = getText(bodyPart);
	    	        	  System.out.println(content);
	    	            }	  
	    	         }
	    		} else {
	    			// Could be PGP inline mail, just print the decrypted text then
	    			String content = message.getContent().toString();
	    			System.out.println(content);
	    		}
	    		
	    	} catch (Exception e) {
	    		System.out.println(e.getMessage());
	    	}
	    } else if (mailUtil.isOpenPGPSigned(message)) {	    	
	    	// pgp signed only message
	    	MimeBodyPart decrypted = mailUtil.getSignedContent((MimeMessage)message);
	        String content = getText(decrypted);
	        System.out.println(content);
	    } else {
	    	System.out.println(message.getContentType());
	    }
	   }  
	  
	   //5) close the store and folder objects  
	   emailFolder.close(false);  
	   emailStore.close();  
	  
	  } catch (NoSuchProviderException e) {e.printStackTrace();}   
	  catch (MessagingException e) {e.printStackTrace();}  
	  catch (IOException e) {e.printStackTrace();}  
	 }
	
	public static void main(String[] a) {
		ReceiveEncryptedMail receiveDemo = new ReceiveEncryptedMail();
		receiveDemo.receiveEmail("mail.mywebsite.com", "me@mywebsite.com", "my POP3 password");
	}	
}