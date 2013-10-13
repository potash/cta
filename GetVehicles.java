import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import com.ctc.wstx.exc.WstxParsingException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import cta.api.CTA;
import cta.models.Vehicle;

public class GetVehicles {
	public static void main(String[] args) throws SQLException,
			JsonParseException, JsonMappingException, IOException {

		CTA cta = new CTA(args[0]);
		Connection connection = cta.getDatabaseConnection();
		
		try {
			List<Vehicle> vehicles = cta.getVehicles(new String[] {"22","50","77","76","36","92","81","2","55","6"});//cta.getVehicles(file);
			try {
				cta.insertVehicles(connection, vehicles);
			} catch (BatchUpdateException e) {
				System.err.println(e.getNextException());
			}
		} catch (IOException e) {
				System.err.println(e);
				//System.err.println(readFile(file));
		}
		
		connection.close();
	}
}