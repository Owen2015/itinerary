package com.sinbad.itinerary.logic.planner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sinbad.itinerary.logic.ga.GeneticTSP;
import com.sinbad.itinerary.logic.ga.ModifiedGeneticTSP;
import com.sinbad.itinerary.model.geo.Locatable;
import com.sinbad.itinerary.util.ArrayUtil;
import com.sinbad.itinerary.util.DistanceUtils;
import com.sinbad.itinerary.util.ListUtils;

public class TspLocationPlanner implements Planned{

	public List<?> plan(List<? extends Locatable> locations, Locatable start, int days){
		return tspPlan(locations,start,days);
	}
	
	@SuppressWarnings("unchecked")
	private List<Locatable> tspPlan(List<? extends Locatable> locations, Locatable start, int days){
		int[] result=tsp((List<Locatable>) locations);
		int startIndex=ListUtils.getIndex ((List<Locatable>)locations,start);
		int resultIndex=getIndex(result,startIndex);
		if(resultIndex==-1)
		System.out.println("start spot did not be found in spot lists");
		return 	(List<Locatable>) getTspLocations((List<Locatable>)locations,result,resultIndex);
	}
	
	@SuppressWarnings("unchecked")
	private List<Locatable> tspPlan(List<? extends Locatable> locations, Locatable start){
		int[] result=tsp((List<Locatable>) locations);
		int startIndex=ListUtils.getIndex ((List<Locatable>)locations,start);
		int resultIndex=getIndex(result,startIndex);
		if(resultIndex==-1)
		System.out.println("start spot did not be found in spot lists");
		return (List<Locatable>) getTspLocations((List<Locatable>)locations,result,resultIndex);
	}
	private int [] tspPlan2(List<? extends Locatable> locations, Locatable start){
		return tsp((List<Locatable>) locations);
	}
	
	private int[] tspPlan(List<? extends Locatable> locations, Locatable start, Locatable end){
		int startIndex=ListUtils.getIndex2(locations, start);
		int endIndex=ListUtils.getIndex2(locations, end);
		if(locations.size()==2){
			Collections.swap(locations, 0, startIndex);			
		}else{
			Collections.swap(locations, 0, startIndex);
			Collections.swap(locations, locations.size()-1, endIndex);			
		}
		int[] result=tsp2((List<Locatable>) locations);
		//ArrayUtil.swap(result, 0, startIndex);
/*		int newStartIndex=ArrayUtil.getIndex(result, startIndex);
		int newEndIndex=ArrayUtil.getIndex(result, endIndex);
		ArrayUtil.swap(result, locations.size()-1, newEndIndex);
		ArrayUtil.swap(result, 0, newStartIndex);*/
		return result;
/*		List<Locatable> tspLocations=new ArrayList<Locatable>();
		for(int i=0;i<result.length;i++){
			tspLocations.add(locations.get(result[i]));
		}
		return tspLocations;*/
	}
	
	public int[] tsp(List<Locatable> locations){
		GeneticTSP tsp=new GeneticTSP();
		tsp.setMaxGeneration(1000);
		tsp.setAutoNextGeneration(true);
		return tsp.tsp(getAdjMatrix(locations));
	}
	
	public int [] tsp2(List<Locatable> locations){
		ModifiedGeneticTSP tsp=new ModifiedGeneticTSP();
		tsp.setMaxGeneration(1000);
		tsp.setAutoNextGeneration(true);
		return tsp.tsp(getAdjMatrix(locations));
	}
	private double[][]  getAdjMatrix (List<Locatable> locations){
		int size=locations.size();
		double[][] matrix=new double[size][size];
		for(int i=0;i<size;i++){
			for(int j=0;j<size;j++){
				matrix[i][j]=DistanceUtils.get_distance(locations.get(i).getLatitude(), locations.get(i).getLongitude(), locations.get(j).getLatitude(), locations.get(j).getLongitude());
			}
		}
		return matrix;
	}

	private List<?> getTspLocations(List<? extends Locatable> in, int[] result,int index){
		List<Locatable> out=new ArrayList<Locatable>();
		for(int i=index;i<result.length;i++){
			out.add(in.get(result[i]));
		}
		for(int i=0;i<index;i++){
			out.add(in.get(result[i]));
		}
		return out;
	}

	private  int getIndex(int[] array,int ele ){
		for(int i=0;i<array.length;i++){
			if(array[i]==ele){
				return i;
			}
		}
		return -1;
	}

	@Override
	public int[] plan(List<? extends Locatable> locations, Locatable start) {
		// TODO Auto-generated method stub
		return tspPlan2(locations,start);
	}
	
	public int[] plan(List<? extends Locatable> locations, Locatable start, Locatable end){
		return tspPlan(locations,start,end);
	}
}
