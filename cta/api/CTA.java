package cta.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import cta.models.Pattern;
import cta.models.Prediction;
import cta.models.Route;
import cta.models.Vehicle;

public class CTA {
	private Properties properties;

	private ObjectMapper xmlMapper;
	private DateFormat ctaDateFormat, mysqlDateFormat;

	private static final int connectTimeout = 10000, // 10 seconds
			readTimeout = 10000; // 10 seconds

	// initialize parser
	public CTA() {
		JacksonXmlModule module = new JacksonXmlModule();
		module.setDefaultUseWrapper(false); // unwrapped lists
		xmlMapper = new XmlMapper(module);

		ctaDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm");
		mysqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	}

	// load url and key from properties file
	public CTA(String propertiesFile) throws FileNotFoundException, IOException {
		this();
		properties = new Properties();
		properties.load(new FileInputStream(propertiesFile));
	}

	public List<Route> getRoutes()
			throws JsonParseException, JsonMappingException,
			MalformedURLException, IOException {

		xmlMapper.setDateFormat(ctaDateFormat);

		Response response = xmlMapper.readValue(
				getInputStream(new URL(properties.getProperty("cta_url") + "getroutes?key=" + properties.getProperty("cta_key"))), Response.class);
		
		return response.getRoutes();
	}

	
	// get vehicles from online api
	public List<Vehicle> getVehicles(String[] routes)
			throws JsonParseException, JsonMappingException,
			MalformedURLException, IOException {

		xmlMapper.setDateFormat(ctaDateFormat);

		return xmlMapper.readValue(
				getInputStream(new URL(properties.getProperty("cta_url") + "getvehicles?key=" + properties.getProperty("cta_key") + "&rt="
						+ join(routes, ","))), Response.class).getVehicles();
	}

	// legacy method for getting vehicles from file
	public List<Vehicle> getVehicles(File file) throws JsonParseException,
			JsonMappingException, IOException {
		xmlMapper.setDateFormat(mysqlDateFormat);
		return xmlMapper.readValue(file, Response.class).getVehicles();
	}

	// get pattern from online api
	public Pattern getPattern(int id, Route route) throws JsonParseException,
			JsonMappingException, MalformedURLException, IOException {

		Response response = xmlMapper.readValue(getInputStream(new URL(properties.getProperty("cta_url")
				+ "getpatterns?key=" + properties.getProperty("cta_key") + "&pid=" + id)), Response.class);
		response.getPattern().setRoute(route);

		return response.getPattern();
	}

	// batch sql insert a list of vehicles. Faster than loading Ebean.
	public void insertVehicles(Connection connection, List<Vehicle> vehicles)
			throws SQLException {

		if (vehicles != null && vehicles.size() > 0) {
			String sql = "insert into vehicle (vehicle_id,timestamp,pattern_id,route_id,distance,delay) "
					+ "values (?, ?, ?, ?, ?,?)";
			PreparedStatement ps = connection.prepareStatement(sql);

			for (Vehicle vehicle : vehicles) {
				if (!vehicle.isNull()) {
					ps.setInt(1, vehicle.getVehicleId());
					ps.setTimestamp(2, vehicle.getTimestamp());
					ps.setInt(3, vehicle.getPatternId());
					ps.setString(4, vehicle.getRouteId());
					ps.setInt(5, vehicle.getDistance());
					ps.setBoolean(6,
							vehicle.getDelay() != null ? vehicle.getDelay()
									: false);
					ps.addBatch();
				}
			}
			ps.executeBatch();
			ps.close();
		}
	}
	
	public void insertPredictions(Connection connection, List<Prediction> predictions) throws SQLException {
		String sql = "insert into prediction(timestamp, type, stop_id, stop_name, vehicle_id, distance, route_id, direction, predicted_time, delay) "
				+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement ps = connection.prepareStatement(sql);

		for (Prediction prediction : predictions) {
			if (!prediction.isNull()) {
				System.out.println("hi");
				ps.setTimestamp(1, prediction.getTimestamp());
				ps.setString(2, prediction.getType());
				ps.setInt(3, prediction.getStopId());
				ps.setString(4, prediction.getStopName());
				ps.setInt(5, prediction.getVehicleId());
				ps.setInt(6, prediction.getDistance());
				ps.setString(7, prediction.getRouteId());
				ps.setString(8, prediction.getDirection());
				ps.setTimestamp(9, prediction.getPredictedTime());
				ps.setBoolean(10, prediction.getDelay() != null ? prediction.getDelay() : false);
				ps.addBatch();
			}
		}
		try {
		ps.executeBatch();
		} catch (BatchUpdateException e) {
			System.out.println(e.getNextException());
		}
		ps.close();
	}

	// get an input stream with timeout settings from url
	private static InputStream getInputStream(URL u) throws IOException {
		URLConnection con = u.openConnection();
		con.setConnectTimeout(connectTimeout);
		con.setReadTimeout(readTimeout);

		return con.getInputStream();
	}

	// join an array of strings with a delimeter
	private static String join(String[] strings, String delimeter) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < strings.length; i++) {
			buffer.append(strings[i]);

			if (i < strings.length - 1)
				buffer.append(delimeter);
		}

		return buffer.toString();
	}
	
	
	public Connection getDatabaseConnection() throws SQLException {
		return DriverManager.getConnection(
				properties.getProperty("db_url"), properties.getProperty("db_username"), properties.getProperty("db_password"));
		
	}
}
