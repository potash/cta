package cta.models;

import java.io.Serializable;

import lombok.Data;

//@Embeddable
public @Data class StopId implements Serializable {
	//@Column(name="rt")
	private String routeId;
	//@Column(name="stpid")
	private Integer stopId;
	//@Column(name="dir_id")
	private Integer directionId;
	
	public StopId() {
	
	}
	
	public StopId(String rt, Integer stpid, Integer dirid) {
		setRouteId(rt);
		setStopId(stpid);
		setDirectionId(dirid);
	}
}
