
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import cta.models.Run;
import cta.models.Time;
import cta.models.Vehicle;


public class ProcessVehicles {

	static class PatternThread extends Thread {
		private Queue<Integer> pids;
		private int n = 10000;

		public PatternThread(Queue<Integer> pids) {
			this.pids = pids;
		}

		public void run() {
			while (!pids.isEmpty()) {
				Integer pid = pids.poll();
				System.out.println("Processing pattern " + pid);
				
				long t = System.currentTimeMillis();
				List<Vehicle> vehicles;
				while (/*(Ebean.beginTransaction()) != null &&*/ (vehicles = getVehicles(pid, n)).size() > 0) {
					Time time = null;
					Run run = Run.byVehicle(vehicles.get(0));
					for (Vehicle v : vehicles) {
						if (run == null
								|| !(v.getVehicleId().equals(run.getVehicleId()))
								|| (time != null && v.getTimestamp().getTime() >= time
										.getTimestamp().getTime()
										+ Run.RUN_MAX_DT)) {
							if (run != null) {
								Ebean.refreshMany(run, "times");
								//Ebean.refreshMany(run, "stopTimes");
								run.processTimes();
								run.updateProcessed(true);
							}
							run = Run.byVehicle(v);
							if (run == null) {
								run = new Run(v);
								run.setProcessed(false);
								Ebean.save(run);
							} else {
								run.updateProcessed(false);
							}
							System.out.println(run.getId());
						}
						try {
							//run.addVehicle(v, false);
							time = new Time(v, run);
							Ebean.save(time);
						} catch (PersistenceException e) {
							System.err.println(e.getMessage());
						}
						try {
							Ebean.delete(v);
						} catch (OptimisticLockException e) {
							System.out.println(e.getMessage());
							// no problem, will get reprocessed next time around
						}
					}

					// process last one
					if (run != null && !run.getProcessed()) {
						Ebean.refreshMany(run, "times");
						//Ebean.refreshMany(run, "stopTimes");
						run.processTimes();
						run.updateProcessed(true);
					}
					long t2 = System.currentTimeMillis();
					double dt = (t2 - t) / 1000d;
					System.out.println(vehicles.size() + " vehicles in " + dt
							+ " seconds");
					t = t2;
				}
			}
		}
	}

	public static List<Vehicle> getVehicles(int pid, int n) {
		return Ebean.find(Vehicle.class).where().eq("patternId", pid)
				.orderBy("vehicleId asc, timestamp asc").setMaxRows(n)
				.findList();
	}

	private static void processVehicles() {
		String sql = "select id from pattern where id in (select distinct pattern_id from vehicle)";
		SqlQuery sqlQuery = Ebean.createSqlQuery(sql);
		Queue<Integer> pids = new ConcurrentLinkedQueue<Integer>();
		for (SqlRow row : sqlQuery.findList()) {
			pids.add(row.getInteger("id"));
		}
		
		for (int i = 0; i < 6; i++) {
			PatternThread thread = new PatternThread(pids);
			thread.start();
		}
	}

	public static void main(String[] args) throws Exception {
		processVehicles();
	}
}
