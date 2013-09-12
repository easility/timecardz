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
import models.Token;
import models.UserDbo;
import models.TimeCardDbo;
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

@With(Secure.class)
public class OtherStuff extends Controller {

	private static final Logger log = LoggerFactory.getLogger(OtherStuff.class);

	public static void company() {
		UserDbo user = Utility.fetchUser();
		if(user!=null && !user.isAdmin()) {
			validation.addError("Access", "Oops, you do not have access to this page");
			dashboard();
		}
		CompanyDbo company = user.getCompany();
		log.info("User = " + user +" and Company = " + company);
		List<UserDbo> employees = user.getEmployees();
		List<TimeCardDbo> timeCards = user.getTimecards();
		render(user, company, employees, timeCards);
	}


	public static void addCompany() {
		render();
	}

	public static void companyDetails() {
		UserDbo user = Utility.fetchUser();
		CompanyDbo company = user.getCompany();
		log.info("User = " + user.getEmail() +" and Company = " + company);
		List<UserDbo> users = null;
		if (company != null)
			users = company.getUsers();
		render(user, company, users);
	}

	public static void postAddition(String name, String address, String phone, String detail) throws Throwable {
		validation.required(name);
		UserDbo user = Utility.fetchUser();
	
		if(validation.hasErrors()) {
			params.flash(); // add http parameters to the flash scope
	        validation.keep(); // keep the errors for the next request
	        addCompany();
		}
		CompanyDbo company = new CompanyDbo();
		company.setName(name);
		company.setAddress(address);
		company.setPhoneNumber(phone);
		company.setDescription(detail);
		company.addUser(user);
		
		user.setCompany(company);
		
		JPA.em().persist(company);
		JPA.em().persist(user);
		
		JPA.em().flush();
		company();
	}

	public static void dashboard() {
		UserDbo user = Utility.fetchUser();
		if (user!=null && user.isAdmin())
			company();
		else
			employee();
	}
	
	public static void addUser() {
		UserDbo admin = Utility.fetchUser();
		CompanyDbo company = admin.getCompany();
		log.info("Adding users by Admin = " + admin.getEmail()
				+ " and Company = " + company.getName());
		List<UserDbo> users = company.getUsers();

		render(admin, company, users);
	}

	public static void postUserAddition(String useremail, String manager)
			throws Throwable {
		validation.required(useremail);

		if (!useremail.contains("@"))
			validation.addError("useremail", "This is not a valid email");

		EmailToUserDbo existing = JPA.em().find(EmailToUserDbo.class,
				useremail);
		if (existing != null) {
			validation.addError("useremail", "This email already exists");
		}

		if (validation.hasErrors()) {
			params.flash(); // add http parameters to the flash scope
			validation.keep(); // keep the errors for the next request
			addUser();
		}
		UserDbo admin = Utility.fetchUser();
		CompanyDbo company = admin.getCompany();

		UserDbo user = new UserDbo();
		user.setEmail(useremail);
		user.setCompany(company);
		if (manager == null) {
			// If there is no manager, add the current user as Manager
			user.setManager(admin);
		} else {
			EmailToUserDbo ref = JPA.em().find(EmailToUserDbo.class, manager);
			UserDbo adminDbo = JPA.em().find(UserDbo.class, ref.getValue());
			user.setManager(adminDbo);
		}

		JPA.em().persist(user);

		EmailToUserDbo emailToUser = new EmailToUserDbo();
		emailToUser.setId(useremail);
		emailToUser.setValue(user.getId());
		JPA.em().persist(emailToUser);

		company.addUser(user);
		JPA.em().persist(company);

		JPA.em().flush();

		String key = Utility.generateKey();
		Token token = new Token();
		long timestamp = System.currentTimeMillis();
		token.setTime(timestamp);
		token.setToken(key);
		token.setEmail(useremail);
		JPA.em().persist(token);
		JPA.em().flush();
		Utility.sendEmail(useremail, company.getName(), key);
		companyDetails();
	}

	public static void rename(String useremail,String firstmanager,String manager){

		EmailToUserDbo oldManagerRef = JPA.em().find(EmailToUserDbo.class, firstmanager);
		UserDbo oldManager = JPA.em().find(UserDbo.class, oldManagerRef.getValue());

		EmailToUserDbo newManagerRef = JPA.em().find(EmailToUserDbo.class, manager);
		UserDbo newManager = JPA.em().find(UserDbo.class, newManagerRef.getValue());

		EmailToUserDbo empRef = JPA.em().find(EmailToUserDbo.class, useremail);
		UserDbo emp = JPA.em().find(UserDbo.class, empRef.getValue());

		emp.setManager(newManager);
		JPA.em().persist(emp);

		newManager.addEmployee(emp);
		JPA.em().persist(newManager);

		oldManager.deleteEmployee(emp);
		JPA.em().persist(oldManager);

		JPA.em().flush();

		dashboard();
	}

	public static void employee() {
		UserDbo employee = Utility.fetchUser();
		List<UserDbo> employees = employee.getEmployees();
		String email=employee.getEmail();
		if (employees != null && employees.size() == 0) {
			// Employee is either only employee or a manager with no employee under him
			// so render his timecards only
			LocalDate beginOfWeek = Utility.calculateBeginningOfTheWeek();
			List<TimeCardDbo> timeCards = employee.getTimecards();
			render(timeCards,beginOfWeek,email);
		} else {
			manager();
		}
	}

	public static void detailEmployee(String id) {
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
		render(timeCard, dayCardDbo, noofhours, details, readOnly, status);
	}

	public static void manager() {
		//Manager can have his own timecards to submit to admin
		UserDbo manager = Utility.fetchUser();
		List<UserDbo> employees = manager.getEmployees();
		List<TimeCardDbo> timeCards = manager.getTimecards();
		render(employees, timeCards);
	}
	

	public static void addTime() {
		UserDbo employee = Utility.fetchUser();
		LocalDate beginOfWeek = Utility.calculateBeginningOfTheWeek();
		DateTimeFormatter fmt = DateTimeFormat.forPattern("MMM dd");
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
		render(currentWeek, employee, beginOfWeek, dayCards, noofhours, details);
	}

	public static void postTimeAddition(int totaltime, String detail)
			throws Throwable {
		validation.required(totaltime);

		if (validation.hasErrors()) {
			params.flash(); // add http parameters to the flash scope
			validation.keep(); // keep the errors for the next request
			addTime();
		}

		UserDbo user = Utility.fetchUser();
		CompanyDbo company = user.getCompany();
		UserDbo manager = user.getManager();

		TimeCardDbo timeCardDbo = new TimeCardDbo();
		timeCardDbo.setBeginOfWeek(Utility.calculateBeginningOfTheWeek());
		timeCardDbo.setNumberOfHours(totaltime);
		timeCardDbo.setDetail(detail);
		timeCardDbo.setApproved(false);
		timeCardDbo.setStatus(StatusEnum.SUBMIT);
		user.addTimecards(timeCardDbo);
		JPA.em().persist(timeCardDbo);
		JPA.em().persist(user);

		JPA.em().flush();

		Utility.sendEmailForApproval(manager.getEmail(), company.getName(), user.getEmail());
		employee();
	}

	public static void postTimeAddition2(int[] noofhours, String[] details) throws Throwable {
		UserDbo user = Utility.fetchUser();
		CompanyDbo company = user.getCompany();
		UserDbo manager = user.getManager();

		TimeCardDbo timeCardDbo = new TimeCardDbo();
		timeCardDbo.setBeginOfWeek(Utility.calculateBeginningOfTheWeek());
		LocalDate beginOfWeek = Utility.calculateBeginningOfTheWeek();
		int totalhours = 0;
		for (int i = 0; i < 7; i++) {
			DayCardDbo dayC = new DayCardDbo();
			dayC.setDate(beginOfWeek.plusDays(i));
		//validation.required(noofhours[i]);
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
			addTime();
		}
		JPA.em().flush();

		timeCardDbo.setNumberOfHours(totalhours);
		// timeCardDbo.setDetail(detail);
		timeCardDbo.setApproved(false);
		timeCardDbo.setStatus(StatusEnum.SUBMIT);
		user.addTimecards(timeCardDbo);
		JPA.em().persist(timeCardDbo);
		JPA.em().persist(user);
		JPA.em().flush();
		Utility.sendEmailForApproval(manager.getEmail(), company.getName(), user.getEmail());
		employee();
	}
	
	
	public static void updateTimeAddition(String timeCardId,
			String[] dayCardsid, int[] noofhours, String[] details) {
		TimeCardDbo timeCard = JPA.em().find(TimeCardDbo.class, timeCardId);
		int sum = 0;
		for (int i = 0; i < 7; i++) {
			DayCardDbo dayC = JPA.em().find(DayCardDbo.class, dayCardsid[i]);
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
				params.flash(); // add http parameters to the flash scope
				validation.keep(); // keep the errors for the next request
				addTime();
			}

		}
		timeCard.setNumberOfHours(sum);
		timeCard.setStatus(StatusEnum.SUBMIT);
		JPA.em().persist(timeCard);
		JPA.em().flush();
		employee();

	}

	public static void detail(String id) {
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

	public static void cardsAction(String timeCardId, int status) {

		TimeCardDbo ref = JPA.em().find(TimeCardDbo.class, timeCardId);
		if (ref != null) {
			if (status == 1) {
				ref.setStatus(StatusEnum.APPROVED);
				ref.setApproved(true);
			} else {
				ref.setStatus(StatusEnum.CANCELLED);
				ref.setApproved(false);
			}

		}
		JPA.em().persist(ref);
		JPA.em().flush();
		manager();
	}

	public static void success() {
		render();
	}

	public static void cancel() {
		render();
	}
}
