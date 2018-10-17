package com.sinbad.itinerary.logic.planner;

import java.util.List;

import com.sinbad.itinerary.model.geo.Locatable;

public class LocationPlanner {

	@SuppressWarnings("unchecked")
	public LocationPlanner(List<? extends Locatable> locations,Locatable start){
		this.locations=(List<Locatable>) locations;
		this.start=start;
	}
	public LocationPlanner(){
		
	}
	@SuppressWarnings("unchecked")
	public List<?> performPlan(int days){
		return planner.plan((List<Locatable>)locations,start,days);
	}

	public int[] performPlan(){
		return planner.plan(locations, start);
	}
	public int[] performPlan(List<? extends Locatable> locations,Locatable start){
		return planner.plan(locations, start);
	}
	public int[] performPlan(List<? extends Locatable> locations,Locatable start,Locatable end){
		return planner.plan(locations, start,end);
	}
	public void setPlanner(Planned planner){
		this.planner=planner;
	}
	
	protected List<Locatable> locations;
	protected Locatable start;
	protected Planned planner;

}
