package cta.tests;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;

import cta.models.Pattern;
import cta.models.Position;
import cta.models.Run;
import cta.models.Time;
import cta.models.Vehicle;

public class RunTest {
	public static final int N = 4;		// number of stops on test pattern
	public static final int DX = 10000;	// distance between stops on test pattern

	@BeforeClass
	public static void init() {
		ServerConfig c = new ServerConfig();
		c.setName("h2");
		DataSourceConfig h2 = new DataSourceConfig();
		h2.setUsername("test");
		h2.setPassword("test");
		h2.setUrl("jdbc:h2:mem:tests;DB_CLOSE_DELAY=-1");
		h2.setDriver("org.h2.Driver");
		c.setDataSourceConfig(h2);
		c.setDdlGenerate(true);
		c.setDdlRun(true);
		c.setRegister(true);
		c.setDefaultServer(true);
		EbeanServerFactory.create(c);
		
		ArrayList<Position> positions = new ArrayList<Position>();
		for (int i = 0; i < N; i++) {
			Position position = new Position(i, "Stop " + i, i*DX);
			positions.add(position);
		}
		
		Pattern pattern = new Pattern(1, (N-1)*DX, "South Bound", positions);
		Ebean.save(pattern);
		
	}
	
	@Test
	public void testSetGoodTimes() {
		Pattern pattern = Ebean.find(Pattern.class, 1);
		Vehicle[] v = {
				new Vehicle(1, minuteTimestamp(1), pattern, 0, "1", null),
				new Vehicle(1, minuteTimestamp(2), pattern, 123, "1", null),
				new Vehicle(1, minuteTimestamp(3), pattern, 196, "1", null),
				new Vehicle(1, minuteTimestamp(4), pattern, 1200, "1", null),
				new Vehicle(1, minuteTimestamp(5), pattern, 1500, "1", null)
		};
		
		Run run = new Run(1,pattern);
		run.addVehicle(v[0]);
		assertEquals(run.getTimes().size(),1);
		//assertEquals(run.getTimes().get(0), new Time(minuteTimestamp(1),0,true,run));
		run.addVehicle(v[1]);
		run.addVehicle(v[2]);
		run.addVehicle(v[3]);
		run.addVehicle(v[4]);
		//run.processTimes();
		System.out.println(run.getTimes());
		System.out.println(run.getStopTimes());
	}

	/*@Test
	public void testProcessTimes() {
		fail("Not yet implemented");
	}*/
	
	// helper method to 
	public static Timestamp minuteTimestamp(int min) {
		return new Timestamp(min * 60 * 1000);
	}
	
	public static void main(String[] args) throws InterruptedException {
		init();
		new RunTest().testSetGoodTimes();
	}
}
