package cta.models;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public @Data
class Prediction {

	@Id
	private Long id;
	
	@JsonProperty("vid")
	private Integer vehicleId;
	
	@JsonProperty("typ")
	private String type;
	
	@JsonProperty("tmstmp")
	private Timestamp timestamp;
	
	@JsonProperty("stpnm")
	private String stopName;
	
	@JsonProperty("stpid")
	private Integer stopId;

	@JsonProperty("dstp")
	private Integer distance;

	//@ManyToOne
	//@JoinColumn(name = "route_id", referencedColumnName = "rt")
	//private Route route;
	
	@JsonProperty("rt")
	private String routeId;

	@JsonProperty("rtdir")
	private String direction;

	@JsonProperty("dly")
	private Boolean delay;
	
	@JsonProperty("prdtm")
	private Timestamp predictedTime;
	

	public boolean isNull() {
		return (getVehicleId() == null || getTimestamp() == null
				|| getRouteId() == null || getDistance() == null || getPredictedTime() == null );
	}
}
