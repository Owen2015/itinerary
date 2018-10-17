package com.sinbad.itinerary.logic.planner;

import java.util.List;

import com.sinbad.itinerary.model.geo.Locatable;

public interface Planned {

	public List<?> plan(List<? extends Locatable> locations,Locatable start,int num);
	//public List<?> plan(List<? extends Locatable> locations,Locatable start);
	public int[] plan(List<? extends Locatable> locations,Locatable start);
	public int[] plan(List<? extends Locatable> locations,Locatable start, Locatable end);
	
}
