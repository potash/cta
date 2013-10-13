package cta.models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Transient;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.annotation.CacheStrategy;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@CacheStrategy(warmingQuery = "order by id")
@ToString(exclude = {"runs"})
@EqualsAndHashCode(of={"id"})
@JsonIgnoreProperties(ignoreUnknown = true)
public @Data
class Pattern {

	@Id
	@JsonProperty("pid")
	private Integer id;

	@ManyToOne
	@JoinColumn(name = "direction_id", referencedColumnName = "id")
	private Direction direction;

	private Integer length;

	@ManyToOne
	@JoinColumn(name = "route_id", referencedColumnName = "id")
	private Route route;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "pattern")
	@OrderBy("id asc")
	@JsonProperty("pt")
	private List<Position> positions;

	public Pattern() {
	}

	@JsonCreator
	public Pattern(@JsonProperty("pid") int pid, @JsonProperty("ln") double ln,
			@JsonProperty("rtdir") String rtdir,
			@JsonProperty("pt") List<Position> positions) {

		setId(pid);
		setLength((int) ln);
		setDirection(Ebean.find(Direction.class).where().eq("name", rtdir)
				.findUnique());

		// remove waypoints and set position ids
		for (int p = 0; p < positions.size();) {
			Position position = positions.get(p);
			if (position.getStopId() == null) {
				positions.remove(p);
			} else {
				position.setIndex(++p);
			}
		}
		setPositions(positions);
	}

	@JsonProperty
	private List<Integer> getStopIds() {
		List<Integer> stopIds = new ArrayList<Integer>(getPositions().size());
		for (Position p : getPositions())
			stopIds.add(p.getStopId());
		return stopIds;
	}

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "pattern")
	private List<Run> runs;

	public Position getPositionById(Integer stpid) {
		return Ebean.find(Position.class).setUseQueryCache(true).where()
				.eq("pattern", this).where().eq("stopId", stpid).findUnique();
	}

	// get the stop for this pdist
	public Position getPosition(Integer pdist) {
		return getPosition(pdist, 1);
	}

	// get the stop for this pdist
	// starting the search from id
	// if the stop is before id, this will id
	private Position getPosition(Integer pdist, Integer id) {
		Position lastP = getPositions().get(id - 1);
		for (; id <= getPositions().size(); id++) {
			Position p = getPositions().get(id - 1);
			if (pdist < p.getDistance()) {
				break;
			}
			lastP = p;
		}
		return lastP;
	}

	// distance cutoffs for processing specific to beginning and end of pattern
	@Transient
	private Integer beginDistance, endDistance;
	private static final double lambda = .75;

	public Integer getEndDistance() {
		if (endDistance == null) {
			List<Position> positions = getPositions();
			int size = positions.size();
			if (positions.size() > 1) {
				endDistance = (int) (lambda
						* positions.get(size - 1).getDistance() + (1 - lambda)
						* positions.get(size - 2).getDistance());
			} else {
				endDistance = 0;
			}
		}
		return endDistance;
	}

	public Integer getBeginDistance() {
		if (beginDistance == null) {
			List<Position> positions = getPositions();
			if (positions.size() > 1) {
				beginDistance = (int) (lambda * positions.get(0).getDistance() + (1 - lambda)
						* positions.get(1).getDistance());
			} else {
				beginDistance = 0;
			}
		}
		return beginDistance;
	}

}
