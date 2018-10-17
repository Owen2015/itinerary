package com.sinbad.itinerary.logic.scorer;

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.sinbad.itinerary.model.entity.Hotel;
import com.sinbad.itinerary.util.DistanceUtils;
import com.sinbad.itinerary.util.StatisticsUtil;


public class HotelScorer extends ConfigScorer {

	private Logger logger=Logger.getLogger(getClass());
	public HotelScorer(List<Hotel> hotels,Double lat,Double lon){
		initConfig("config.properties");
		init(hotels);
		extract(hotels,lat,lon);
	}
	private void init(List<Hotel> hotels){
		int rowNum=hotels.size();
		int colNum;
		factors=new HashMap<String, Object>();
		factors.put(HOTELRANKFACTOR, 1);
		factors.put(HOTELPRICEFACTOR, 1);
		factors.put(HOTELDISTANCEFACTOR, 1);
		try{
			factors.put(HOTELRANKFACTOR, config.getProperty(HOTELRANKFACTOR));
			factors.put(HOTELPRICEFACTOR, config.getProperty(HOTELPRICEFACTOR));
			factors.put(HOTELDISTANCEFACTOR, config.getProperty(HOTELDISTANCEFACTOR));
		}catch (Exception e){
			logger.error(e);
		}
		colNum=factors.size();
		
		result=new double[rowNum];
		factor=new double[colNum];
		data=new double[rowNum][colNum];
		
		factor[0]=Double.parseDouble((String) factors.get(HOTELRANKFACTOR));
		factor[1]=Double.parseDouble((String) factors.get(HOTELPRICEFACTOR));
		factor[2]=Double.parseDouble((String) factors.get(HOTELDISTANCEFACTOR));
		
	}
	private void extract(List<Hotel> hotels,Double lat,Double lon){
		for(int i=0;i<hotels.size();i++){
			data[i][0]=0;
			//data[i][1]=hotels.get(i).getHotelPrice();
			data[i][1]=0;
			data[i][2]=DistanceUtils.get_distance(hotels.get(i).getLatitude(), hotels.get(i).getLongitude(), lat, lon);
		}
		data=StatisticsUtil.normalize(data);
		
	}
	
	public double[] score(){
		return Scorer.score(data, factor);
	}
	private static final String HOTELRANKFACTOR="hotelrankfactor";	
	private static final String HOTELPRICEFACTOR="hotelpricefactor";
	private static final String HOTELDISTANCEFACTOR="hoteldistancefactor";
}
