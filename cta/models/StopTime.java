package cta.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.persistence.Version;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@EqualsAndHashCode(exclude = { "min", "max", "timestamp", "position" })
@ToString(exclude = { "run", "min", "max", "position" })
// @IdClass(StopTimeId.class)
@JsonIgnoreProperties({ "run", "min", "max", "position" })
public @Data
class StopTime {

	@Id
	private Long id;

	private Integer stopId;

	@ManyToOne
	@JoinColumn(name = "run_id", referencedColumnName = "id")
	private Run run;

	@ManyToOne
	@JoinColumn(name = "min_time_id", referencedColumnName = "id")
	private Time min;

	@ManyToOne
	@JoinColumn(name = "max_time_id", referencedColumnName = "id")
	private Time max;

	private Timestamp timestamp;
	
	// for optimistic locking!
	@Version
	private int version;

	public StopTime() {
	}

	@Transient
	@Getter(lazy=true)
	private final Position position = position();

	
	public Position position() {
		return Ebean.find(Position.class).setUseQueryCache(true).setUseCache(true)
					.where().eq("pattern", getRun().getPattern())
					.eq("stopId", getStopId()).findUnique();
	}

	public StopTime(Run run, Integer id) {
		// setStopTimeId(new StopTimeId(run.getId(), id));
		setRun(run);
		setStopId(id);
		setRun(run);
	}

	public void updateMin(Time t) {
		if (getMin() == null || getMin().getGood() != true) {
			setMin(t);
		} else if (t != null && t.greaterThan(getMin())) {
			setMin(t);
		}
	}

	public void updateMax(Time t) {
		if (getMax() == null || getMax().getGood() != true) {
			setMax(t);
		} else if (t != null && t.lessThan(getMax())) {
			setMax(t);
		}
	}

	public void updateTimes(Time min, Time max) {
		updateMin(min);
		updateMax(max);
		updateTimestamp();
		Ebean.save(this);
	}

	public void setTimes(Time min, Time max) {
		if (min != null)
			setMin(min);
		if (max != null)
			setMax(max);
		updateTimestamp();
		Ebean.save(this);
	}

	private void updateTimestamp() {
		Boolean a = getMin() != null, b = getMax() != null;
		if (getPosition().isFirst()) {
			if (b
					&& max.getDistance() < getRun().getPattern()
							.getBeginDistance()) {
				setTimestamp(max.getTimestamp());
			}
		} else if (getPosition().isLast()) {
			if (a && min.getDistance() > getRun().getPattern().getEndDistance()) {
				setTimestamp(min.getTimestamp());
			}
		} else {
			if (a && b) {
				// if there is a previous stop and a stoptime in this run for it
				Integer pDist0 = getMin().getDistance();
				Integer pDist1 = getMax().getDistance();
				Integer stopDist = getPosition().getDistance();

				if (pDist0.equals(stopDist)) {
					setTimestamp(new Timestamp((getMin().getTimestamp()
							.getTime() + getMax().getTimestamp().getTime()) / 2));
				} else {
					double s = (stopDist - pDist0) / (double) (pDist1 - pDist0);
					long t = (long) ((1 - s)
							* getMin().getTimestamp().getTime() + s
							* getMax().getTimestamp().getTime());
					setTimestamp(new Timestamp(t));
				}
			}
		}

	}

	// TODO: this is simpler in new schema...
	public static List<Timestamp> getStopTimes(String route, Integer dir,
			Integer stopId) {
		List<Timestamp> times = new ArrayList<Timestamp>();

		return times;
	}
}
