package cta.models;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import com.avaje.ebean.annotation.CacheStrategy;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@CacheStrategy(warmingQuery = "order by id")
@EqualsAndHashCode(exclude = { "patterns" })
@ToString(exclude = { "patterns" })
public @Data
class Direction {
	@JsonIgnore
	@Id
	private Integer id;

	private String name;

	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "direction")
	private List<Pattern> patterns;
}
