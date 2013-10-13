package cta.models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.annotation.CacheStrategy;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//@Entity
//@CacheStrategy(warmingQuery = "order by rt,dir_id")
@EqualsAndHashCode(exclude = { "route","direction","stopName","positions" })
@JsonIgnoreProperties({"id","route","direction","positions"})
@IdClass(StopId.class)
public @Data class Stop {
	
	//@EmbeddedId
	//private StopId id;
	
	//@MapsId("stop")
	@Id
	private Integer stopId;
	
	@Id
	private String routeId;
	
	@Id
	private Integer directionId;
	
	@MapsId("route")
	@ManyToOne
	@JoinColumn(name = "route_id", referencedColumnName = "id")
	private Route route;
	
	private String name;

	@MapsId("direction")
	@ManyToOne
	@JoinColumn(name = "direction_id", referencedColumnName = "id")
	private Direction direction;

	public Stop() {

	}

	public Stop(Position p) {
		setStopId(p.getStopId());
		setRoute(p.getPattern().getRoute());
		setName(p.getName());
		setDirection(p.getPattern().getDirection());
	}
	
	@Transient
	private List<Position> positions;
	
	public List<Position> getPositions() {
		if (positions == null) {
			setPositions(Ebean.find(Position.class).setUseQueryCache(true).where()
					.eq("pattern.route", getRoute())
					.eq("pattern.direction", getDirection())
					.eq("stopId", getStopId()).findList());
		}
		
		return positions;
	}

	/*public List<Stop> getDestinations() {
		String sql = "select distinct q.stpid from positions p join positions q "
				+ "on p.pid = q.pid and p.id < q.id "
				+ "join patterns r on p.pid = r.pid "
				+ "where p.stpid=:stpid and r.rt = :rt and r.dir_id = :dir";
		SqlQuery sqlQuery = Ebean.createSqlQuery(sql);
		sqlQuery.setParameter("stpid", getStopId());
		sqlQuery.setParameter("rt", getRoute().getRoute());
		sqlQuery.setParameter("dir", getDirection().getId());

		// execute the query returning a List of MapBean objects
		List<SqlRow> rows = sqlQuery.findList();
		List<Stop> stops = new ArrayList<Stop>(rows.size());
		for (SqlRow row : rows) {
			stops.add(Ebean.find(Stop.class).setUseQueryCache(true).where().eq("route",getRoute())
					.eq("direction", getDirection())
					.eq("stopId", row.getInteger("stpid")).findUnique());
		}
		return stops;
	}*/
}
