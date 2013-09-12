package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import play.db.jpa.GenericModel;

@Entity
public class TimeCardDbo extends GenericModel{

	private static DateTimeFormatter fmt = DateTimeFormat
			.forPattern("MMM dd, yyyy");

	@Id
	private String id;

	private LocalDate beginOfWeek;

	private int numberOfHours;
	
	private String detail;
	
	private boolean approved;
	
	@OneToMany
	private List<DayCardDbo> daycards = new ArrayList<DayCardDbo>();

	private String status;

	public String getId() {
		return id;
	}

	public void setId(LocalDate beginOfWeek) {
		this.beginOfWeek = beginOfWeek;
	}

	public String getRange() {
		LocalDate end = beginOfWeek.plusWeeks(1);
		end = end.minusDays(1);
		return fmt.print(beginOfWeek) + " to " + fmt.print(end);
	}

	public LocalDate getBeginOfWeek() {
		return beginOfWeek;
	}

	public void setBeginOfWeek(LocalDate beginOfWeek) {
		this.beginOfWeek = beginOfWeek;
	}

	public int getNumberOfHours() {
		return numberOfHours;
	}

	public void setNumberOfHours(int numberOfHours) {
		this.numberOfHours = numberOfHours;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public boolean isApproved() {
		return approved;
	}

	public void setApproved(boolean approved) {
		this.approved = approved;
	}

	public StatusEnum getStatus() {

		return StatusEnum.mapForConversion.get(status);
	}

	public void setStatus(StatusEnum status) {
		this.status = status.getDbValue();
	}

	public List<DayCardDbo> getDaycards() {
		return daycards;
	}

	public void setDaycards(List<DayCardDbo> daycards) {
		this.daycards = daycards;
	}

	public void addDayCard(DayCardDbo dayCard) {
		this.daycards.add(dayCard);
	}
}
