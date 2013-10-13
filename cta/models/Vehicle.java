package cta.models;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public @Data
class Vehicle {

	public Vehicle() {
	}

	public Vehicle(Integer vehicleId, Timestamp timestamp, Pattern pattern,
			Integer distance, String routeId, Boolean delay) {
		setVehicleId(vehicleId);
		setTimestamp(timestamp);
		setPattern(pattern);
		setDistance(distance);
		setRouteId(routeId);
		setDelay(delay);
	}

	@Id
	private Long id;
	@JsonProperty("vid")
	private Integer vehicleId;

	@JsonProperty("tmstmp")
	private Timestamp timestamp;

	// private double lat,lon;
	// private Integer hdg;
	@Version
	private int version;

	@ManyToOne
	@JoinColumn(name = "pattern_id", referencedColumnName = "id")
	private Pattern pattern;

	@JsonProperty("pid")
	private Integer patternId;

	@JsonProperty("pdist")
	private Integer distance;

	// @ManyToOne
	// @JoinColumn(name = "route_id", referencedColumnName = "rt")
	// private Route route;

	@JsonProperty("rt")
	private String routeId;

	// private String des;

	@JsonProperty("dly")
	private Boolean delay;

	public boolean isNull() {
		return (getVehicleId() == null || getTimestamp() == null
				|| getPatternId() == null || getRouteId() == null || getDistance() == null);
	}

}
