/*package com.sinbad.itinerary.logic.scorer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sinbad.itinerary.model.entity.Spot;


public class TagScorer {

/*	public TagScorer(List<Spot> spots){
		init(spots);
	}
	
	private void init(List<Spot> spots){
		if(spots.size()>=size){
			size=spots.size()-1;
		}
		result=new HashMap<Object,Object>();
		for(int i=size;i>size-100;i--){
			System.out.println(spots.get(i).getNameCn()+" : "+spots.get(i).getTags().toString());
			for(Tag tag:spots.get(i).getTags()){
				if(result.get(tag.getName())==null){
					result.put(tag.getName(), (int)Math.pow(i, 2));
				}else{
					result.put(tag.getName(), (int)Math.pow(i, 2)+(int)result.get(tag.getName()));
				}
			}
		}
	}
	
	private void sort(Map<Object,Object> map){
		
	}
	
	public Map<Object,Object> getScore(){
		return result;
	}
	
	private  int size=100;
	private Map<Object,Object> result;
}
*/