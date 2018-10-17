package com.sinbad.itinerary.logic.scorer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sinbad.itinerary.model.bean.Tag;
import com.sinbad.itinerary.model.entity.Spot;
import com.sinbad.itinerary.util.DistanceUtils;
import com.sinbad.itinerary.util.StatisticsUtil;


public class SpotScorer extends ConfigScorer{

	private Logger logger=Logger.getLogger(getClass());
	//public static final Map<String,Integer> tagFrenquency=new HashMap<String,Integer>();
	
	public SpotScorer(List<Spot> allcitysights,List<Spot> requiredsights, List<Tag> tags,
			Double lat, Double lon){
	//super();
	initConfig("config.properties");
	init(allcitysights);
	extract(allcitysights,requiredsights,tags,lat,lon);
	}

	private void init(List<Spot> allcitysights){
		int row_num=allcitysights.size();
		int col_num;
		factors=new HashMap<String,Object>();
		// init factor
		factors.put(COMMENTFACTOR, 1);
		factors.put(STARFACTOR, 1);
		factors.put(RANKFACTOR, 1);
		factors.put(TAGFACTOR, 1);
		factors.put(REQUIREFACTOR, 1);
		factors.put(DISTANCEFACTOR, 1);
		// get factor from config
		factors.put(COMMENTFACTOR, config.getProperty(COMMENTFACTOR));
		factors.put(STARFACTOR, config.getProperty(STARFACTOR));
		factors.put(RANKFACTOR, config.getProperty(RANKFACTOR));
		factors.put(TAGFACTOR, config.getProperty(TAGFACTOR));
		factors.put(REQUIREFACTOR, config.getProperty(REQUIREFACTOR));
		factors.put(DISTANCEFACTOR, config.getProperty(DISTANCEFACTOR));
		
		col_num=factors.size();
		
		factor=new double[col_num];
		data=new double[row_num][col_num];
		result=new double[col_num];
		
		factor[0]=Double.parseDouble((String) factors.get(COMMENTFACTOR));
		factor[1]=Double.parseDouble((String) factors.get(STARFACTOR));
		factor[2]=Double.parseDouble((String) factors.get(RANKFACTOR));
		factor[3]=Double.parseDouble((String) factors.get(TAGFACTOR));
		factor[4]=Double.parseDouble((String) factors.get(REQUIREFACTOR));
		factor[5]=Double.parseDouble((String) factors.get(DISTANCEFACTOR));
		
	}
	private void extract(List<Spot> allcitysights,List<Spot> requiredsights,List<Tag> tags,Double lat,Double lon){
		List<Tag> thistags;
		for(int i=0;i<allcitysights.size();i++){
			// init comment score
			//data[i][0]=(double)allcitysights.get(i).getCommentCount();
			//data[i][1]=(double)allcitysights.get(i).getStar_Score();
			data[i][0]=0;
			data[i][1]=0;
			data[i][2]=0;
			//data[i][2]=(double)allcitysights.get(i).getRank();
			data[i][5]=DistanceUtils.get_distance(lat, lon, allcitysights.get(i).getLatitude(), allcitysights.get(i).getLongitude());
			// init tag 
			thistags= allcitysights.get(i).getTags();
			for(int j=0;j<tags.size();j++){
				if(thistags.contains(tags.get(j))){
					data[i][3]=data[i][3]+1;
				}
			}
			// init require sights
			if(requiredsights.contains(allcitysights.get(i)))
				data[i][4]=1D;
			else
				data[i][4]=0D;

			//logger.debug("after extract: sight: "+allcitysights.get(i).getNameCn()+", comment: "+data[i][0]+", star: "+data[i][1]+", rank: "+data[i][2]+", tag: "+data[i][3]+", require sight: "+data[i][4]);
		}
		data=StatisticsUtil.normalize(data);
		for(int i=0;i<allcitysights.size();i++){
			data[i][2]=1-data[i][2];
			data[i][5]=1-data[i][5];
			//logger.debug("after norm: sight: "+allcitysights.get(i).getNameCn()+", comment: "+data[i][0]+", star: "+data[i][1]+", rank: "+data[i][2]+", tag: "+data[i][3]+", require sight: "+data[i][4]+", distance "+data[i][5]);
		}
	}

	public double[] score(List<Spot> allcitysights){
		result=Scorer.score(data, factor);
		for(int i=0;i<allcitysights.size();i++){
			allcitysights.get(i).setScore(result[i]);
			//logger.debug("after score: sight: "+allcitysights.get(i).getNameCn()+", score: "+allcitysights.get(i).getScore());
		}
		return result;
	}
	
	private static String COMMENTFACTOR="commentfactor";
	private static String STARFACTOR="starfactor";
	private static String RANKFACTOR="rankfactor";
	private static String TAGFACTOR="tagfactor";
	private static String REQUIREFACTOR="requirefactor";
	private static String DISTANCEFACTOR="distancefactor";
}
