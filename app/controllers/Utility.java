package controllers;

import java.util.UUID;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import play.Play;
import play.db.jpa.JPA;
import play.libs.Mail;
import play.mvc.Scope.Session;
import play.mvc.results.Unauthorized;
import models.EmailToUserDbo;
import models.UserDbo;

public class Utility {
	private static final Logger log = LoggerFactory.getLogger(Utility.class);
	
	public static UserDbo fetchUser() {
		String userName = Session.current().get("username");
		EmailToUserDbo ref = JPA.em().find(EmailToUserDbo.class, userName);
		if(ref == null) {
			throw new Unauthorized("bug, user not found, corrupt session");
		}
		
		UserDbo user = JPA.em().find(UserDbo.class, ref.getValue());
		return user;
	}

	public static String generateKey(){
		UUID token = UUID.randomUUID();
		return(token.toString());
	}

	public static void sendEmail(String emailId, String company,String key) {
		SimpleEmail email = new SimpleEmail();
		String mode = Play.configuration.getProperty("application.mode");
		// TBD to change the URl
		String signupUrl = "null";
		if ("dev".equals(mode)) {
			signupUrl = Play.configuration.getProperty("dev.signupUrl");

		} else {
			signupUrl = Play.configuration.getProperty("prod.signupUrl");

		}
		try {
			email.setFrom("no-reply@tbd.com");
			email.addTo(emailId);
			email.setSubject("You are registered for " + company);
			email.setMsg(" Hi,\n You have been added on www.tbd.com for "+ company + 
					" to submit your time cards. \n Please go to "+ signupUrl +"usersignup2/"+key+"  and complete the registration. \n Best Regards");

			Mail.send(email); 
		} catch (EmailException e) {
			log.error("ERROR in sending mail to " + emailId);
			e.printStackTrace();
		}
		

/*		//An HTML e-mail code snippet to be used later if required:
 
		HtmlEmail email = new HtmlEmail();
		email.addTo("info@lunatech.com");
		email.setFrom(sender@lunatech.com", "Nicolas");
		email.setSubject("Test email with inline image");
		// embed the image and get the content id
		URL url = new URL("http://www.zenexity.fr/wp-content/themes/images/logo.png");
		String cid = email.embed(url, "Zenexity logo");
		// set the html message
		email.setHtmlMsg("<html>Zenexity logo - <img src=\"cid:"+cid+"\"></html>");
		// set the alternative message
		email.setTextMsg("Your email client does not support HTML, too bad :(");
		*/
		
	}

	public static void sendEmailForApproval(String emailId, String company, String employee) {
		SimpleEmail email = new SimpleEmail();
		try {
			email.setFrom("no-reply@tbd.com");
			email.addTo(emailId);
			email.setSubject("New time card submitted");
			email.setMsg("The user " + employee + " who is registerdd for " + company + 
					" has submitted his time card.\n Please log on to www.tbd.com and approve/reject the same. \n Best Regards");

			Mail.send(email); 
		} catch (EmailException e) {
			log.error("ERROR in sending mail to " + emailId);
			e.printStackTrace();
		}
	}

	public static LocalDate calculateBeginningOfTheWeek() {
		LocalDate time = LocalDate.now();
		LocalDate beginOfWeek = time.dayOfWeek().withMinimumValue();
		return beginOfWeek;
	}
	
}
