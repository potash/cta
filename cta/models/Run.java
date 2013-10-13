package cta.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import com.avaje.ebean.Ebean;

@Entity
// @CacheStrategy(warmingQuery = "order by id")
@ToString(exclude = { "times", "stopTimes" })
@EqualsAndHashCode(of = { "id" })
public @Data
class Run {

	@Id
	private Long id;
	private Integer vehicleId;

	@ManyToOne
	@JoinColumn(name = "pattern_id", referencedColumnName = "id")
	private Pattern pattern;

	@OneToMany(/* cascade = CascadeType.ALL, */mappedBy = "run")
	@OrderBy("timestamp asc, distance asc")
	private List<Time> times;

	@OneToMany(/* cascade = CascadeType.ALL, */mappedBy = "run")
	@MapKey(name = "stopId")
	private Map<Integer, StopTime> stopTimes;

	// slower than this at endpoints means stopped
	// 100 ft/min in ft/ms
	private static final double TIME_MIN_V = 100d / (60 * 1000);
	// bus stopped longer than this in middle of route means bus was off route
	// 10 min in ms
	private static final long TIME_MAX_DT = 10 * 60 * 1000;
	// if two vehicles have timestamps within this many minutes they are the
	// same run - 15 minutes in ms
	public static final long RUN_MAX_DT = 15 * 60 * 1000;

	public Run() {
	}

	public Run(Integer vid, Pattern pattern) {
		setVehicleId(vid);
		setPattern(pattern);
	}

	public Run(Vehicle v) {
		this(v.getVehicleId(), v.getPattern());
	}

	// if processed = false, something went wrong
	// and run should be reprocessed
	private Boolean processed;
	private static final Set<String> processedProp = new HashSet<String>();
	{
		processedProp.add("processed");
	}

	public void updateProcessed(boolean processed) {
		setProcessed(processed);
		Ebean.update(this, processedProp);
	}

	// TODO: if dt > RUN_MAX_DT but dx/dt is reasonable...
	public static Run byVehicle(Vehicle v) {
		Run run = null;

		long t = v.getTimestamp().getTime();
		Timestamp t0 = new Timestamp(t - RUN_MAX_DT);
		Timestamp t1 = new Timestamp(t + RUN_MAX_DT);
		Time time = Ebean.find(Time.class).where().between("timestamp", t0, t1)
				.eq("run.vehicleId", v.getVehicleId())
				.eq("run.pattern.id", v.getPattern().getId())
				.setMaxRows(1).findUnique();
		if (time != null) {
			run = time.getRun();
		}
		return run;
	}

	// does the vehicle belong to this run?
	public boolean match(Vehicle v) {
		if (!(v.getPattern().equals(getPattern()) && v.getVehicleId().equals(
				getVehicleId()))) {
			return false;
		}
		// Ebean.refreshMany(this, "times");
		if (getTimes() == null || getTimes().size() == 0) {
			return true;
		}

		Time t0 = getTimes().get(0);
		Time t1 = getLastTime();

		return (v.getTimestamp().getTime() < t1.getTimestamp().getTime()
				+ RUN_MAX_DT && v.getTimestamp().getTime() > t0.getTimestamp()
				.getTime() - RUN_MAX_DT);
	}

	public void addVehicle(Vehicle v) {
		addVehicle(v, true);
	}

	// add a vehicle, set good
	// and, if process=true, set good and process stop times
	public void addVehicle(Vehicle v, boolean process) {
		Time t = new Time(v, this);

		List<Time> times = getTimes();
		if (times == null) {
			setTimes(new ArrayList<Time>());
			times = getTimes();
		}
		List<Time> good = null; // the tail good times to be processed

		// max index such that times[i].tmstmp <= t.tmstmp
		int minTimeIndex = times.size();
		// max index such that times[i].pdist < t.pdist
		int minDistIndex = times.size();

		for (int i = times.size() - 1; i >= 0; i--) {
			Time s = times.get(i);
			int c = t.getTimestamp().compareTo(s.getTimestamp());
			int d = t.getDistance().compareTo(s.getDistance());

			if (c < 0)
				minTimeIndex = i;

			if (d < 0) {
				minDistIndex = i;
				if (c == 0)
					minTimeIndex = i;
			} else if (c > 0 && d >= 0) {
				break;
			} else if (c == 0 && d == 0) {
				System.out.println("Duplicate " + t);
				return;
			}
		}

		times.add(minTimeIndex, t);
		Ebean.save(t);

		if (process) {
			// run setGoodTimes
			int min = Math.min(minTimeIndex, minDistIndex);
			List<Time> seq = new ArrayList<Time>(times.subList(min,
					times.size()));
			good = setGoodTimes(seq);

			// get previous good time for stop time processing
			Time t1 = good.get(0);
			boolean done = false; // flag for breaking from loop
			while ((min = getPreviousGood(times, min)) >= 0 && !done) {
				Time t0 = times.get(min);
				switch (areGood(t0, t1)) {
				case 2:
					good.add(0, t0);
					done = true;
					break;
				case 1:
					t0.updateGood(false);
					break;
				case 0:
					t1.updateGood(false);
					good.set(0, t0);
					break;
				}
			}

			updateStopTimes(good);
		}
	}

	// return the last good time index before index
	private int getPreviousGood(List<Time> times, int index) {
		// TODOhandle bad input
		if (times.size() > 0) {
			for (int i = index - 1; i >= 0; i--) {
				try {
				if (times.get(i).getGood()) {
					return i;
				}
				} catch (NullPointerException e) {
					System.out.println(getId());
					System.out.println(times.size());
					System.out.println(index);
					System.out.println(i);
					throw e;
				}
			}
		}
		return -1;
	}

	// get a stop time or create a new one
	public StopTime getStopTime(Integer stopId) {
		if (getStopTimes() == null) {
			setStopTimes(new HashMap<Integer, StopTime>());
		}
		StopTime st = getStopTimes().get(stopId);

		if (st == null) {
			st = new StopTime(this, stopId);
			getStopTimes().put(stopId, st);
			// Ebean.save(st);
		}
		return st;
	}

	// return an increasing subsequence (breaks ties with later timestamps)
	// also spots bad times using isGood
	public List<Time> setGoodTimes(List<Time> times) {
		if (times.isEmpty()) {
			return new ArrayList<Time>();
		} else if (times.size() == 1) {
			Time t = times.get(0);
			t.updateGood(true);
			return new ArrayList<Time>(times);
		} else {
			// q[i] length max subseq ending on times[i]
			int[] q = new int[times.size()];
			// y[i] = penultimate index of max subseq ending on times[i]
			int[] y = new int[times.size()];
			int max = 0; // the winner so far

			for (int i = 0; i < q.length; i++) {
				Time t = times.get(i);
				// t.updateGood(false);
				y[i] = -1;
				for (int j = /* 0; j < i; j++) { */i - 1; j >= 0; j--) {
					if (times.get(j).lessThan(t) && q[j] >= q[i] - 1) {
						q[i] = q[j] + 1;
						y[i] = j;
						if (q[j] == q[max]) // if this is the absolute max no
							break; // need to go farther
					}
				}
				if (y[i] == -1)
					q[i] = 0;
				if (q[i] >= q[max]) // >
					max = i;
			}

			// build non-decreasing subseq
			List<Time> x = new ArrayList<Time>(q[max]);

			Time lastTime = null;
			int nextIndex = max; // next element of good
			for (int i = times.size() - 1; i >= 0; i--) {
				Time t = times.get(i);
				if (i == nextIndex) {
					if (lastTime == null) {
						t.updateGood(true);
						x.add(t);
						lastTime = t;
					} else {
						switch (areGood(t, lastTime)) {
						case 2:
							t.updateGood(true); // both good
							x.add(t);
							lastTime = t;
							break;
						case 1:
							t.updateGood(false); // t bad
							break;
						case 0:
							t.updateGood(true); // t good, last bad
							lastTime.updateGood(false);
							x.set(x.size() - 1, t);
							lastTime = t;
							break;
						}
					}

					nextIndex = y[i];

				} else {
					t.updateGood(false);
				}
			}

			Collections.reverse(x);
			return x;
		}
	}

	/**
	 * @param Times
	 *            t0 < t1
	 * @return 0 if only t0 is good, 1 if only t1 is good, 2 if both are good
	 */
	private int areGood(Time t0, Time t1) {
		if (t1 == null) {
			return 0;
		}
		int problem = 0; // 0 = good, 1 = off route, 2 = slow
		int x0 = t0.getDistance(), x1 = t1.getDistance();
		int dx = x1 - x0;
		double dt = t1.getTimestamp().getTime() - t0.getTimestamp().getTime();

		if (dx == 0 && dt > TIME_MAX_DT) {
			problem = 1;
		} else if (dt == 0 || dx / dt < TIME_MIN_V) {
			problem = 2;
		}

		if (x1 < getPattern().getBeginDistance()) {
			if (problem > 0)
				return 1;
		} else if (x0 > getPattern().getEndDistance()) {
			if (problem > 0)
				return 0;
		} else {
			if (problem == 1)
				return 0;
		}

		return 2;
	}

	// process times: mark the good ones and compute stop times
	public void processTimes() {
		updateStopTimes(setGoodTimes(getTimes()));
	}

	// compute stop times corresponding to the (sorted) times
	private void updateStopTimes(List<Time> times) {
		if (times.isEmpty())
			return;

		int from = times.get(0).getPosition().getIndex();
		int to = getLast(times).getPosition().getIndex();
		if (getLast(times).getDistance() > getPattern().getEndDistance()) {
			// if the max pdist is greater than finalPdist process the final
			// stopTime as well TODO this sould be part of getPosition()?
			to = getPattern().getPositions().size();
		}

		int j = 0; // current time index
		Time time = times.get(j); // current time

		for (int i = from; i < to + 1; i++) {
			Position p = getPattern().getPositions().get(i - 1);
			StopTime st = getStopTime(p.getStopId());

			if (p.isLast()) {
				st.setTimes(getLast(times), null);
			} else if (p.isFirst()) {
				st.updateTimes(null, time);
			} else {
				Time min = null, max = null; // first time with geq pdist
				int c;

				while (true) {
					c = time.getDistance().compareTo(p.getDistance());
					if (c == 0 && min == null)
						min = time;
					else if (c > 0)
						break;

					if (j < times.size() - 1)
						time = times.get(++j);
					else
						break;
				}

				// now time is either first time after pdist or last of times
				// and min is either null or the first time with equal pdist

				if (min == null) { // did not see vehicle at exact location
					if (c > 0) { // after
						if (j > 0) // and before
							min = times.get(j - 1);
						max = time;
					} else { // only before
						min = time;
					}
				} else { // saw at exact location
					if (c > 0) { // and overshot
						max = times.get(j - 1);
					} else {
						max = time;
					}
				}

				if (i == from) { // if its the first position
					st.updateTimes(min, max);// update it
				} else {
					st.setTimes(min, max); // if its the last, set it
				}
			}
		}

	}

	// utility method
	private <T> T getLast(List<T> c) {
		if (c != null && c.size() != 0) {
			return c.get(c.size() - 1);
		} else {
			return null;
		}
	}

	public Time getLastTime() {
		return getLast(getTimes());
	}

	public static @Data
	class RunKey {
		private Integer vehicleId;
		private Integer patternId;

		public RunKey(Integer vehicleId, Integer patternId) {
			setVehicleId(vehicleId);
			setPatternId(patternId);
		}
	}
}
