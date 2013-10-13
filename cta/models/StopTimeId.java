package cta.models;

import java.io.Serializable;

import lombok.Data;

//@Embeddable
public @Data class StopTimeId implements Serializable {
	//@Column(name="run_id")
	private Long runId;
	//@Column(name="id")
	private Integer id;
	
	public StopTimeId() {
	
	}
	
	public StopTimeId(Run run, Integer id) {
		setRunId(run.getId());
		setId(id);
	}
}
