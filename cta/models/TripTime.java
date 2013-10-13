package cta.models;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Entity;

import lombok.Data;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.ebean.annotation.Sql;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Sql
public @Data class TripTime {

	public Timestamp start;
	public Timestamp end;
	
	public TripTime() {
	}

	public TripTime(Timestamp start, Timestamp end) {
		this.start = start;
		this.end = end;
	}
	
	@JsonIgnore
	public int getDuration() {
		return (int)((end.getTime()-start.getTime())/60000);
	}
	
	public static List<TripTime> getTripTimes(String rt, Integer dir, Integer id0, Integer id1) {
		String sql = "select min(tmstmp) as start, max(tmstmp) as end " +
				"from stop_times st join runs r " +
				"on st.run_id = r.id " +
				"join positions p on p.id = st.id and p.pid = r.pid " +
				"join patterns p2 on p.pid = p2.pid " +
				"where p2.rt = :rt and p2.dir_id = :dir " +
				"and (p.stpid = :stpid0 or p.stpid = :stpid1) " +
				"group by st.run_id " +
				"having count(*) = 2 " +
				"order by end desc";
		
		RawSql rawSql = RawSqlBuilder.parse(sql)
				.create();
		
		return Ebean.find(TripTime.class).setRawSql(rawSql)
				.setParameter("rt",rt)
				.setParameter("dir",dir)
				.setParameter("stpid0",id0)
				.setParameter("stpid1",id1).findList();
		
	}
}