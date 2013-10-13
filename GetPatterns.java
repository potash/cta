import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import cta.api.CTA;
import cta.models.Pattern;
import cta.models.Route;

// Executable for fetching and storing unknown patterns referenced in vehicles
// Takes one argument, the location of a cta api properties file
public class GetPatterns {
	public static void main(String[] args) throws JsonParseException,
			JsonMappingException, IOException, SQLException,
			ClassNotFoundException {
		
		CTA cta = new CTA(args[0]);
		String sql = "select distinct route_id,pattern_id from vehicle "
				+ "where pattern_id not in (select id from pattern)";
		SqlQuery query = Ebean.createSqlQuery(sql);
		List<SqlRow> rows = query.findList();
		for (SqlRow row : rows) {
			System.out.println(row.getInteger("route_id") + ", "
					+ row.getInteger("pattern_id"));
			try {
				Route route = Ebean.find(Route.class, row.getString("route_id"));
				Pattern pattern = cta.getPattern(row.getInteger("pattern_id"), route);
				
				Ebean.save(pattern);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
