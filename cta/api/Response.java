package cta.api;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import cta.models.Pattern;
import cta.models.Prediction;
import cta.models.Route;
import cta.models.Vehicle;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "bustime-response")
public @Data class Response {
	@JsonProperty("vehicle")
	private List<Vehicle> vehicles;
	
	@JsonProperty("error")
	private List<Error> errors;
	
	@JsonProperty("route")
	private List<Route> routes;	
	
	@JsonProperty("prd")
	private List<Prediction> predictions;
	
	@JsonProperty("ptr")
	private Pattern pattern;
}
