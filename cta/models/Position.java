package cta.models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import com.avaje.ebean.annotation.CacheStrategy;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

@Entity
@CacheStrategy(warmingQuery="order by pattern_id,id") //readOnly =false
@Table(name = "position")
@ToString(exclude = { "pattern", "first", "last" })
@EqualsAndHashCode(exclude = { "pattern" })
@JsonIgnoreProperties(ignoreUnknown = true)
public @Data
class Position {
	@Id
	private Long id;

	private Integer index;

	@ManyToOne
	@JoinColumn(name = "pattern_id", referencedColumnName = "id")
	private Pattern pattern;

	@JsonProperty("stpid")
	private Integer stopId;

	@JsonProperty("stpnm")
	private String name;

	// @JsonProperty("pdist")
	private Integer distance;

	@JsonSetter
	public void setPdist(double pdist) {
		setDistance((int) pdist);
	}

	@JsonProperty("lat")
	private Double latitude;
	@JsonProperty("lon")
	private Double longitude;

	public Position() {
	}

	public Position(Integer stopId, String name, Integer distance) {
		setStopId(stopId);
		setName(name);
		setDistance(distance);
	}

	@Transient
	@Getter(lazy = true)
	private final boolean first = first(), last = last();

	private boolean first() {
		return getIndex().equals(1);
	}

	public Boolean last() {
		return getIndex().equals(getPattern().getPositions().size());
	}
}
