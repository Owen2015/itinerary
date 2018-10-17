package com.sinbad.itinerary.logic.scorer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.sinbad.itinerary.util.StatisticsUtil;

public class FlightScorer extends ConfigScorer {

	private Logger logger = Logger.getLogger(this.getClass());

	public FlightScorer(String configName) throws ParseException {

		initConfig(configName);
		//init(routings);
		//extract(routings);
	}
	public FlightScorer(Properties config){
		this.config=config;
	}
	// read config properties, outter factor
	public void init(List<HashMap<String, Object>> routings) throws ParseException {
		int row_num = routings.size();
		int col_num = 6;
		factors = new HashMap<String, Object>();
		initconfig();
		factor = new double[col_num];
		data = new double[row_num][col_num];
		result = new double[row_num];
		initfactor();
		extract(routings);
	}

	private void extract(List<HashMap<String, Object>> routings) throws ParseException {
		String format = "yyyy-MM-dd HH:mm";
		Date departuredate;
		Date arrivaldate;
		Calendar ca = Calendar.getInstance();
		;
		for (int i = 0; i < routings.size(); i++) {

			HashMap<String, Object> routing = routings.get(i);
			@SuppressWarnings("unchecked")
			List<Object> legs=(List<Object>) routing.get("leg");
			Map<String,Object> outward=(Map<String, Object>) legs.get(0);
			Map<String,Object> flight=(Map<String, Object>) outward.get("flight");
			List<HashMap<String,Object>> segments = (List<HashMap<String, Object>>) flight.get("segments");

			//Map<String, Object> segment = (Map<String, Object>) segments.get(0);
			//List<HashMap<String, Object>> segments = (List<HashMap<String, Object>>) segment.get("Flights");
			if (segments.size() > 1) {

				departuredate = str2date((String) segments.get(0).get("dtime"), format);
				arrivaldate = str2date((String) segments.get(segments.size() - 1).get("atime"), format);
				data[i][3] = arrivaldate.getTime() - departuredate.getTime();
				ca.setTime(departuredate);
				data[i][4] = getDepartureNorm(ca.get(Calendar.HOUR_OF_DAY));
				ca.setTime(arrivaldate);
				data[i][5] = getArrivalNorm(ca.get(Calendar.HOUR_OF_DAY));
			} else {
				departuredate = str2date((String) segments.get(0).get("dtime"), format);
				arrivaldate = str2date((String) segments.get(0).get("atime"), format);
				data[i][3] = arrivaldate.getTime() - departuredate.getTime();
				ca.setTime(departuredate);
				data[i][4] = getDepartureNorm(ca.get(Calendar.HOUR_OF_DAY));
				ca.setTime(arrivaldate);
				data[i][5] = getArrivalNorm(ca.get(Calendar.HOUR_OF_DAY));
			}

			List<HashMap<String,Object>> price=(List<HashMap<String, Object>>) routing.get("price");
			Map<String,Object> adtPrice=price.get(0);
			data[i][0] = (Double) adtPrice.get("ticket")+(Double) adtPrice.get("taxes");
			data[i][1] = (double) routing.get("startdistance");
			data[i][2] = (double) routing.get("enddistance");

			logger.debug("ith: " + i + ", price: " + data[i][0] + ", startdistance: " + data[i][1] + ", enddistance: "
					+ data[i][2] + ", totaltime: " + data[i][3] + ", departuredate: " + data[i][4] + ", arrivaldate: "
					+ data[i][5]);
		}

		data = StatisticsUtil.normalize(data);
		logger.debug("----------------------");
		for(int i=0;i<data.length;i++){
			logger.debug("ith: " + i + ", price: " + data[i][0] + ", startdistance: " + data[i][1] + ", enddistance: "
					+ data[i][2] + ", totaltime: " + data[i][3] + ", departuredate: " + data[i][4] + ", arrivaldate: "
					+ data[i][5]);
		}
	}
	/*
	 * public double[] score() throws ParseException {
	 * 
	 * result=Scorer.score(data, factor); //for(int i=0;i<result.length;i++) //
	 * logger.debug("ith: "+i+", score: "+result[i]+", price: "+data[i][0]
	 * +", startdistance: "+data[i][1]+", enddistance: "+data[i][2]
	 * +", totaltime: "+data[i][3]+", departuredate: "+data[i][4]
	 * +", arrivaldate: "+data[i][5]);
	 * 
	 * return result; }
	 */

	private Date str2date(String str, String format) throws ParseException {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		return simpleDateFormat.parse(str);
	}

	// input from config inner factor
	private double getDepartureNorm(int hour) {

		if (Integer.parseInt((String) factors.get("0-0-0")) <= hour
				&& hour < Integer.parseInt((String) factors.get("0-0-1"))) {
			return Double.parseDouble((String) factors.get("0-0-2"));
		} else if (Integer.parseInt((String) factors.get("0-1-0")) <= hour
				&& hour < Integer.parseInt((String) factors.get("0-1-1"))) {
			return Double.parseDouble((String) factors.get("0-1-2"));
		} else if (Integer.parseInt((String) factors.get("0-2-0")) <= hour
				&& hour < Integer.parseInt((String) factors.get("0-2-1"))) {
			return Double.parseDouble((String) factors.get("0-2-2"));
		} else if (Integer.parseInt((String) factors.get("0-3-0")) <= hour
				&& hour < Integer.parseInt((String) factors.get("0-3-1"))) {
			return Double.parseDouble((String) factors.get("0-3-2"));
		} else if (Integer.parseInt((String) factors.get("0-4-0")) <= hour
				&& hour < Integer.parseInt((String) factors.get("0-4-1"))) {
			return Double.parseDouble((String) factors.get("0-4-2"));
		} else {
			return Double.parseDouble((String) factors.get("0-5-2"));
		}
	}

	private double getArrivalNorm(int hour) {

		if (Integer.parseInt((String) factors.get("1-0-0")) <= hour
				&& hour < Integer.parseInt((String) factors.get("1-0-1"))) {
			return Double.parseDouble((String) factors.get("1-0-2"));
		} else if (Integer.parseInt((String) factors.get("1-1-0")) <= hour
				&& hour < Integer.parseInt((String) factors.get("1-1-1"))) {
			return Double.parseDouble((String) factors.get("1-1-2"));
		} else if (Integer.parseInt((String) factors.get("1-2-0")) <= hour
				&& hour < Integer.parseInt((String) factors.get("1-2-1"))) {
			return Double.parseDouble((String) factors.get("1-2-2"));
		} else if (Integer.parseInt((String) factors.get("1-3-0")) <= hour
				&& hour < Integer.parseInt((String) factors.get("1-3-1"))) {
			return Double.parseDouble((String) factors.get("1-3-2"));
		} else if (Integer.parseInt((String) factors.get("1-4-0")) <= hour
				&& hour < Integer.parseInt((String) factors.get("1-4-1"))) {
			return Double.parseDouble((String) factors.get("1-4-2"));
		} else {
			return Double.parseDouble((String) factors.get("1-5-2"));
		}
	}

	private void initconfig() {

		factors.put("pricefactor", "1");
		factors.put("elapsefactor", "1");
		factors.put("departurefactor", "1");
		factors.put("arrivalfactor", "1");
		factors.put("totalfactor", "1");

		factors.put("startdistance", "1");
		factors.put("enddistance", "1");

		factors.put("0-0-0", "0");
		factors.put("0-0-1", "6");
		factors.put("0-0-2", "0.25");
		factors.put("0-1-0", "6");
		factors.put("0-1-1", "12");
		factors.put("0-1-2", "1");
		factors.put("0-2-0", "12");
		factors.put("0-2-1", "18");
		factors.put("0-2-2", "0.75");
		factors.put("0-3-0", "18");
		factors.put("0-3-1", "24");
		factors.put("0-3-2", "0.25");
		factors.put("0-4-0", "18");
		factors.put("0-4-1", "24");
		factors.put("0-4-2", "0.25");
		factors.put("0-5-0", "18");
		factors.put("0-5-1", "24");
		factors.put("0-5-2", "0.25");

		factors.put("1-0-0", "0");
		factors.put("1-0-1", "6");
		factors.put("1-0-2", "0.25");
		factors.put("1-1-0", "6");
		factors.put("1-1-1", "12");
		factors.put("1-1-2", "0.75");
		factors.put("1-2-0", "12");
		factors.put("1-2-1", "18");
		factors.put("1-2-2", "1");
		factors.put("1-3-0", "18");
		factors.put("1-3-1", "24");
		factors.put("1-3-2", "0.75");
		factors.put("1-4-0", "18");
		factors.put("1-4-1", "24");
		factors.put("1-4-2", "0.75");
		factors.put("1-5-0", "18");
		factors.put("1-5-1", "24");
		factors.put("1-5-2", "0.75");

		factors.put("pricefactor", config.getProperty("pricefactor", "1"));
		factors.put("elapsefactor", config.getProperty("elapsefactor", "1"));
		factors.put("departurefactor", config.getProperty("departurefactor", "1"));
		factors.put("arrivalfactor", config.getProperty("arrivalfactor", "1"));
		factors.put("totalfactor", config.getProperty("totalfactor", "1"));

		factors.put("startdistance", config.getProperty("startdistance", "1"));
		factors.put("enddistance", config.getProperty("enddistance", "1"));

		factors.put("0-0-0", config.getProperty("0-0-0", "0"));
		factors.put("0-0-1", config.getProperty("0-0-1", "6"));
		factors.put("0-0-2", config.getProperty("0-0-2", "0.25"));
		factors.put("0-1-0", config.getProperty("0-1-0", "6"));
		factors.put("0-1-1", config.getProperty("0-1-1", "12"));
		factors.put("0-1-2", config.getProperty("0-1-2", "1"));
		factors.put("0-2-0", config.getProperty("0-2-0", "12"));
		factors.put("0-2-1", config.getProperty("0-2-1", "18"));
		factors.put("0-2-2", config.getProperty("0-2-2", "0.75"));
		factors.put("0-3-0", config.getProperty("0-3-0", "18"));
		factors.put("0-3-1", config.getProperty("0-3-1", "24"));
		factors.put("0-3-2", config.getProperty("0-3-2", "0.25"));
		factors.put("0-4-0", config.getProperty("0-4-0", "18"));
		factors.put("0-4-1", config.getProperty("0-4-1", "24"));
		factors.put("0-4-2", config.getProperty("0-4-2", "0.25"));
		factors.put("0-5-0", config.getProperty("0-5-0", "18"));
		factors.put("0-5-1", config.getProperty("0-5-1", "24"));
		factors.put("0-5-2", config.getProperty("0-5-2", "0.25"));

		factors.put("1-0-0", config.getProperty("1-0-0", "0"));
		factors.put("1-0-1", config.getProperty("1-0-1", "6"));
		factors.put("1-0-2", config.getProperty("1-0-2", "0.25"));
		factors.put("1-1-0", config.getProperty("1-1-0", "6"));
		factors.put("1-1-1", config.getProperty("1-1-1", "12"));
		factors.put("1-1-2", config.getProperty("1-1-2", "1"));
		factors.put("1-2-0", config.getProperty("1-2-0", "12"));
		factors.put("1-2-1", config.getProperty("1-2-1", "18"));
		factors.put("1-2-2", config.getProperty("1-2-2", "1"));
		factors.put("1-3-0", config.getProperty("1-3-0", "18"));
		factors.put("1-3-1", config.getProperty("1-3-1", "24"));
		factors.put("1-3-2", config.getProperty("1-3-2", "0.25"));
		factors.put("1-4-0", config.getProperty("1-4-0", "18"));
		factors.put("1-4-1", config.getProperty("1-4-1", "24"));
		factors.put("1-4-2", config.getProperty("1-4-2", "0.25"));
		factors.put("1-5-0", config.getProperty("1-5-0", "18"));
		factors.put("1-5-1", config.getProperty("1-5-1", "24"));
		factors.put("1-5-2", config.getProperty("1-5-2", "0.25"));
	}

	private void initfactor() {
		factor[0] = Double.parseDouble((String) factors.get("pricefactor"));
		factor[1] = Double.parseDouble((String) factors.get("startdistance"));
		factor[2] = Double.parseDouble((String) factors.get("enddistance"));
		factor[3] = Double.parseDouble((String) factors.get("totalfactor"));
		// factor[1]=Double.parseDouble((String) factors.get("elapsefactor"));
		factor[4] = Double.parseDouble((String) factors.get("departurefactor"));
		factor[5] = Double.parseDouble((String) factors.get("arrivalfactor"));

	}
}

