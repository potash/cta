import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;

import cta.models.Run;
import cta.models.Run.RunKey;
import cta.models.Vehicle;

public class VehiclesProcessor {
	private Map<RunKey, Run> runs;

	public VehiclesProcessor() {
		runs = new HashMap<RunKey, Run>();
	}

	// Match vehicle to existing run, or create a new run
	private Run getRun(Vehicle v) {
		// first check runs cache
		RunKey runKey = new RunKey(v.getVehicleId(), v.getPatternId());
		Run run = runs.get(runKey);
		if (run != null && run.match(v)) {
			return run;
		}
		// then check database
		run = Run.byVehicle(v);
		
		// finally create new run
		if (run == null) {
			run = new Run(v);
			Ebean.save(run);
		}
		runs.put(runKey, run);
		
		return run;
	}

	private int processVehicles() {
		String sql = "select distinct pattern_id from vehicle "
				+ "where pattern_id in (select distinct id from pattern)";
		SqlQuery sqlQuery = Ebean.createSqlQuery(sql);
		List<SqlRow> pids = sqlQuery.findList();
		
		int count = 0;
		for (SqlRow pid : pids) {
			List<Vehicle> vehicles = Ebean.find(Vehicle.class).where()
					.eq("patternId", pid.getInteger("pattern_id"))
					.orderBy("vehicleId asc, timestamp asc").findList();
			for (Vehicle v : vehicles) {
				Run run = getRun(v);
				try {
					run.addVehicle(v);
				} catch (PersistenceException e) {
					System.err.println(e.getMessage());
				}
				count++;
				try {
					Ebean.delete(v);
				} catch (OptimisticLockException e) {
					System.out.println(e.getMessage());
					// no problem, it'll just get reprocessed next time around
				}
			}
		}
		return count;
	}

	public static void main(String[] args) throws Exception {
		VehiclesProcessor cta = new VehiclesProcessor();
		Ebean.runCacheWarming();
		int i = 0;
		while (true) {
			long t = System.currentTimeMillis();
			int count = cta.processVehicles();
			if (count != 0) { // if it processed, sleep for a minute
				i++;
				// every 60 mins get rid of runs that are more than 30 mins old
				if (i % 60 == 0) {
					System.out.println(cta.runs.size() + " runs in cache. Culling.");
					List<RunKey> keys = new ArrayList<RunKey>();
					for (Entry<RunKey, Run> entry : cta.runs.entrySet()) {
						if (Math.abs(System.currentTimeMillis() - entry.getValue().getLastTime().getTimestamp().getTime()) > 30*60*1000) {
							keys.add(entry.getKey());
						}
					}
					
					for (RunKey key : keys) {
						cta.runs.remove(key);
					}
					
					System.out.println(cta.runs.size() + " runs in cache");
					i = 0;
				}
				
				long dt = System.currentTimeMillis() - t;
				System.out.println("Processed " + count + " vehicles in " + dt
						/ 1000d + " seconds.");
				Thread.sleep(Math.max(60000 - dt, 0));
			} else {
				Thread.sleep(3000); // else wait 3 seconds for vehicles
			}
		}
	}
}
