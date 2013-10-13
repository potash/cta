package cta.models;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.annotation.CacheStrategy;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@CacheStrategy(warmingQuery="order by id")
@Table(name="route")
@EqualsAndHashCode(exclude={"patterns"})
public @Data class Route {

	@Id 
	@JsonProperty("rt")
	private String id;
	
	@JsonProperty("rtnm")
	private String name;
	
	@JsonIgnore
	@OneToMany(cascade=CascadeType.ALL, mappedBy="route")
	private List<Pattern> patterns;
	
	@JsonIgnore
	public List<Direction> getDirections() {
		return Ebean.find(Direction.class).setUseQueryCache(true).where()
				.eq("patterns.route",this).findList();
	}
	
	public List<Object> getDirectionIds() {
		return Ebean.find(Direction.class).setUseQueryCache(true).where()
			.eq("patterns.route",this).orderBy("id asc").findIds();
	}
	
	@JsonIgnore
	public List<Stop> getStops(Direction direction) {
		return Ebean.find(Stop.class).setUseQueryCache(true).where()
				.eq("route", this).eq("direction",direction)
				.orderBy("stopName asc").findList();
	}
	
	@JsonIgnore
	public List<Pattern> getPatterns(Direction direction) {
		return Ebean.find(Pattern.class).setUseQueryCache(true).where()
				.eq("route", this).eq("direction", direction).findList();
	}
	
/*	public void processStops() {
		for (Direction d : getDirections()) {
			processStops(d);
		}
	}
	
	public void processStops(Direction direction) {
		System.out.println(getRoute() + ", " + direction);
		List<Pattern> patterns = getPatterns(direction);
		if (patterns.isEmpty())
			return;
		
		PriorityQueue<List<Stop>> stops = 
				new PriorityQueue<List<Stop>>(patterns.size(), new CollectionComparator());
		
		for (Pattern pattern : patterns) {
			List<Stop> s = new ArrayList<Stop>();
			for (Position p : pattern.getPositions())
				s.add(new Stop(p));
			stops.add(s);
		}
		
		System.out.println(stops.peek().size());
		
		List<Stop> sorted = new ArrayList<Stop>();
		Stop stop;
		while ( (stop = getMinimum(stops)) != null) {
			for (List<Stop> l : stops) {
				l.remove(stop);
			}
			sorted.add(stop);
		}
		
		Iterator<List<Stop>> i = stops.iterator();
		System.out.println(sorted.size());
		while (i.hasNext())
			System.out.println(i.next().size());
		
		//Ebean.
		Ebean.save(sorted);
	}
	
	private Stop getMinimum(Collection<List<Stop>> l) {
		Iterator<List<Stop>> i = l.iterator();
		while (i.hasNext()) {
			List<Stop> m = i.next();
			if (!m.isEmpty()) {
				Stop s = m.get(0);
				Iterator<List<Stop>> j = l.iterator();
				boolean min = true;
				while (j.hasNext()) {
					List<Stop> n = j.next();
					if (m != n && n.indexOf(s) > 0) {
						min = false; break;
					}
				}
				if (min)
					return s;
			}
		}
		// if there was no min but the lists are not empty just return the first elt
		i = l.iterator();
		while(i.hasNext()) {
			List<Stop> m = i.next();
			if (!m.isEmpty())
				return m.get(0);
		}
		return null;
	}
	
	private class CollectionComparator implements Comparator<Collection> {
		@Override
		public int compare(Collection c0, Collection c1) {
			return c1.size() - c0.size();
		}
	}*/
}
