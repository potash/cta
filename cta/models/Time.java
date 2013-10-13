package cta.models;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import com.avaje.ebean.Ebean;

@Entity
@ToString(exclude={"run", "position"})
@EqualsAndHashCode(of={"id"})
public @Data class Time {

	@Id
	private Long id;
	private Timestamp timestamp;
	private Integer distance;
	private Boolean good;

	@ManyToOne
	@JoinColumn(name="run_id", referencedColumnName="id")
	private Run run;
	
	@Transient
	Position position;
	
	public Position getPosition() {
		if (position == null) {
			position = getRun().getPattern().getPosition(getDistance());
		}
		return position;
	}

	public Time() {
	}

	public Time(Vehicle v, Run run) {
		setTimestamp(v.getTimestamp());
		setDistance(v.getDistance());
		setRun(run);
	}
	

	public Time(Timestamp timestamp, Integer distance, Boolean good, Run run) {
		setTimestamp(timestamp);
		setDistance(distance);
		setGood(good);
		setRun(run);
	}
	
	/*public Time(Timestamp tmstmp, Integer pdist) {
		setTimestamp(tmstmp);
		setDistance(pdist);
	}*/

	// return true if before, or same time and earlier distance, or after with equal distance 
	/*public boolean before(Time time) {
		return getPdist() < time.getPdist();
	}

	// return true if farther or same distance and earlier
	public boolean after(Time time) {
		return getTimestamp().after(time.getTimestamp());
	}*/

	private static final Set<String> goodProp = new HashSet<String>();
	{
		goodProp.add("good");
	}
	
	public void updateGood(Boolean good) {
		setGood(good);
		Ebean.update(this, goodProp);
	}
	
	public boolean lessThan(Time time) {
		return (getDistance() <= time.getDistance() && 
				getTimestamp().compareTo(time.getTimestamp()) <= 0);
	}
	
	public boolean greaterThan(Time time) {
		return time.lessThan(this);
	}
}
