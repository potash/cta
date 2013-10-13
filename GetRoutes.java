import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import javax.persistence.PersistenceException;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import cta.api.CTA;
import cta.models.Pattern;
import cta.models.Route;

// Executable for fetching and saving all routes
// Takes one argument, the location of a cta api properties file
public class GetRoutes {
	public static void main(String[] args) throws FileNotFoundException, IOException {

		CTA cta = new CTA(args[0]);
		
		try {
			List<Route> routes = cta.getRoutes();
			System.out.println(routes.size());
			for (Route route : routes) {
				try {
					Ebean.save(routes);
				} catch (PersistenceException e) { 
					// the route is already there, do nothing
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
