package cta.api;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class Error {
	@JsonProperty("rt")
	private String route;
	@JsonProperty("msg")
	private String message;
}
