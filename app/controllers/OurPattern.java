package controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import models.DayCardDbo;
import models.EmailToUserDbo;
import models.StatusEnum;
import models.TimeCardDbo;
import models.Token;

import models.UserDbo;

import models.CompanyDbo;

import controllers.auth.Check;
import controllers.auth.Secure;
import controllers.auth.Secure.Security;
import play.Play;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.libs.Crypto;
import play.libs.Time;
import play.mvc.Controller;
import play.mvc.With;
import play.mvc.Scope.Session;

public class OurPattern extends Controller {

	private static final Logger log = LoggerFactory.getLogger(OurPattern.class);
	public static String useremail = "sampleuser@email.com"; 
	public static int createSampleData() {
        UserDbo user = new UserDbo();  
        user.setEmail(useremail);
        user.setPassword("password");
        user.setFirstName("firstName");
        user.setLastName("lastName");
        user.setPhone("1111");
        user.setAdmin(false);
        
        EmailToUserDbo user1 = JPA.em().find(EmailToUserDbo.class, useremail);
        if (user1 == null) {
            JPA.em().persist(user);

            EmailToUserDbo emailToUser = new EmailToUserDbo();
            emailToUser.setEmail(useremail);
            emailToUser.setValue(user.getId());
            JPA.em().persist(emailToUser);

            JPA.em().flush();
            
            TimeCardDbo timeCardDbo = new TimeCardDbo();
            timeCardDbo.setBeginOfWeek(Utility.calculateBeginningOfTheWeek());
            LocalDate beginOfWeek = Utility.calculateBeginningOfTheWeek();
            int totalhours = 0;
            for (int i = 0; i < 7; i++) {
                  DayCardDbo dayC = new DayCardDbo();
                  dayC.setDate(beginOfWeek.plusDays(i));
                  dayC.setNumberOfHours(5);
                  totalhours = totalhours + 5;
                  dayC.setDetail("some details");
                  timeCardDbo.addDayCard(dayC);
                  JPA.em().persist(dayC);
                }
                JPA.em().flush();
                timeCardDbo.setNumberOfHours(totalhours);
                timeCardDbo.setApproved(false);
                timeCardDbo.setStatus(StatusEnum.SUBMIT);
                user.addTimecards(timeCardDbo);
                JPA.em().persist(timeCardDbo);
                return user.getId();  
        } else 
            return user1.getValue();
        
	}
	
	public static void pattern(Integer id) {
	    int userId = createSampleData();
	    UserDbo user= JPA.em().find(UserDbo.class, userId);
	    List<TimeCardDbo> timeCards = user.getTimecards();
		LocalDate beginOfWeek = Utility.calculateBeginningOfTheWeek();
		DateTimeFormatter fmt = DateTimeFormat.forPattern("MMM dd");
		if (id == null) {
			String currentWeek = fmt.print(beginOfWeek);
			DayCardDbo[] dayCards = new DayCardDbo[7];
			int[] noofhours = new int[7];
			String[] details = new String[7];
			for (int i = 0; i < 7; i++) {
				noofhours[i] = 0;
				details[i] = "";
				dayCards[i] = new DayCardDbo();
				dayCards[i].setDate(beginOfWeek.plusDays(i));
			}
			render(timeCards, beginOfWeek, currentWeek, dayCards, noofhours, details);
		
		} else {
			String view = "view";
			TimeCardDbo timeCard = JPA.em().find(TimeCardDbo.class, id);
			StatusEnum status = timeCard.getStatus();
			boolean readOnly;
			if (status == StatusEnum.APPROVED)
				readOnly = true;
			else
				readOnly = false;
			List<DayCardDbo> dayCardDbo = timeCard.getDaycards();
			int[] noofhours = new int[7];
			String[] details = new String[7];
			int i = 0;
			for (DayCardDbo dayCard : dayCardDbo) {
				noofhours[i] = dayCard.getNumberOfHours();
				details[i] = dayCard.getDetail();
				i++;
			}
			render(view, timeCard, dayCardDbo,timeCards, noofhours, details,
					readOnly, status);
		}
	}

	public static void postTimeAddition2(Integer timeCardId,Integer[] dayCardsid, int[] noofhours, String[] details) throws Throwable {
		Integer id = null;
		EmailToUserDbo user1 = JPA.em().find(EmailToUserDbo.class, useremail);
		UserDbo user = JPA.em().find(UserDbo.class, user1.getValue());
		if (timeCardId == null || dayCardsid == null) {
			TimeCardDbo timeCardDbo = new TimeCardDbo();
			timeCardDbo.setBeginOfWeek(Utility.calculateBeginningOfTheWeek());
			LocalDate beginOfWeek = Utility.calculateBeginningOfTheWeek();
			int totalhours = 0;
			for (int i = 0; i < 7; i++) {
				DayCardDbo dayC = new DayCardDbo();
				dayC.setDate(beginOfWeek.plusDays(i));
				if (noofhours[i] > 12) {
					validation.addError("noofhours[i]",
							"hours should be less than 12");
				} else {
					dayC.setNumberOfHours(noofhours[i]);
					totalhours = totalhours + noofhours[i];
					dayC.setDetail(details[i]);
					timeCardDbo.addDayCard(dayC);
					JPA.em().persist(dayC);
				}
			}
			if (validation.hasErrors()) {
				params.flash(); // add http parameters to the flash scope
				validation.keep(); // keep the errors for the next request
				pattern(id);
			}
			JPA.em().flush();

			timeCardDbo.setNumberOfHours(totalhours);
			timeCardDbo.setApproved(false);
			timeCardDbo.setStatus(StatusEnum.SUBMIT);
			user.addTimecards(timeCardDbo);
            JPA.em().persist(timeCardDbo);          
			JPA.em().persist(user);
			JPA.em().flush();
			pattern(id);
		} else {
			TimeCardDbo timeCard = JPA.em().find(TimeCardDbo.class, timeCardId);
			int sum = 0;
			for (int i = 0; i < 7; i++) {
				DayCardDbo dayC = JPA.em()
						.find(DayCardDbo.class, dayCardsid[i]);
				if (noofhours[i] > 12) {
					validation.addError("noofhours[i]",
							"hours should be less than 12");
				} else {
					dayC.setNumberOfHours(noofhours[i]);
					dayC.setDetail(details[i]);
					JPA.em().persist(dayC);

					sum += noofhours[i];
				}
				if (validation.hasErrors()) {
					params.flash();
					validation.keep();
					pattern(id);
				}

			}
			timeCard.setNumberOfHours(sum);
			timeCard.setStatus(StatusEnum.SUBMIT);
            JPA.em().persist(timeCard);          
			JPA.em().flush();
			pattern(id);
		}
	}

	public static void detail(Integer id) {
		TimeCardDbo timeCard = JPA.em().find(TimeCardDbo.class, id);
		List<DayCardDbo> dayCardDbo = timeCard.getDaycards();
		StatusEnum status = timeCard.getStatus();
		render(dayCardDbo, timeCard, status);
	}

	public static void userCards(String email) {
		EmailToUserDbo ref = JPA.em().find(EmailToUserDbo.class, email);
		UserDbo user = JPA.em().find(UserDbo.class, ref.getValue());
		List<TimeCardDbo> timeCards = user.getTimecards();
		render(email, timeCards);
	}

	public static void success() {
		render();
	}

	public static void cancel() {
		render();
	}
	
}
