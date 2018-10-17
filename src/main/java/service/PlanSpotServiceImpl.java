package com.sinbad.itinerary.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

import javax.annotation.Resource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.sinbad.itinerary.dao.SpotMapper;
import com.sinbad.itinerary.dao.TagMapper;
import com.sinbad.itinerary.dao.base.BaseSpotMapper;
import com.sinbad.itinerary.dao.product.ProductMapper;
import com.sinbad.itinerary.logic.factory.BaseSpotFactory;
import com.sinbad.itinerary.logic.planner.LocationPlanner;
import com.sinbad.itinerary.logic.planner.Planned;
import com.sinbad.itinerary.logic.planner.TspLocationPlanner;
import com.sinbad.itinerary.logic.scorer.BaseSpotScorer;
import com.sinbad.itinerary.logic.scorer.SpotScorer;
import com.sinbad.itinerary.model.SimpleJsonResult;
import com.sinbad.itinerary.model.bean.OpenHour;
import com.sinbad.itinerary.model.bean.OpenRule;
import com.sinbad.itinerary.model.bean.Tag;
import com.sinbad.itinerary.model.bean.TourContext;
import com.sinbad.itinerary.model.dto.input.itinerary.ItineraryInput;
import com.sinbad.itinerary.model.dto.itinerary.ItineraryParameter;
import com.sinbad.itinerary.model.entity.Spot;
import com.sinbad.itinerary.model.entity.base.Base2;
import com.sinbad.itinerary.model.entity.base.BaseSpot;
import com.sinbad.itinerary.model.entity.product.Product;
import com.sinbad.itinerary.model.geo.Marker;
import com.sinbad.itinerary.model.other.Context;
import com.sinbad.itinerary.model.poi.Base;
import com.sinbad.itinerary.model.poi.Itinerary;
import com.sinbad.itinerary.service.PlanService;
import com.sinbad.itinerary.service.PlanSpotService;
import com.sinbad.itinerary.util.ArrayUtil;
import com.sinbad.itinerary.util.TimeJumpUtils;
import com.sinbad.itinerary.util.DateUtils;
import com.sinbad.itinerary.util.DistanceUtils;
import com.sinbad.itinerary.util.JsonUtil;
import com.sinbad.itinerary.util.ListUtils;
import com.sinbad.itinerary.util.StaticData;

/**
 * 景点规划服务类.
 * 1. 根据标签，流行度选择景点。
 * 2. 使用TSP算法规划景点浏览顺序。
 * 
 * @author owen
 *
 */
@Service
public class PlanSpotServiceImpl implements PlanSpotService {

	/**
	 * 废弃方法
	 * @param locations
	 * @return
	 */
	private List<Marker> getLocationsAsList(List<Spot> locations) {
		List<Marker> locations1 = new ArrayList<Marker>();
		for (int i = 0; i < locations.size(); i++) {
			Marker location = new Marker();
			location.setLatitude(locations.get(i).getLatitude());
			location.setLongitude(locations.get(i).getLongitude());
			location.setNameCn(locations.get(i).getName().getNamezh());
			location.setRank(locations.get(i).getRank());
			locations1.add(location);
		}
		return locations1;
	}


	public List<Spot> plan(Date date, Base base, Integer startHour, Integer endHour, Integer adultNum,
			Integer childNum) {
		Integer currentHour = 0;

		while (currentHour > startHour && currentHour < endHour) {

		}

		return null;
	}

	public List<Spot> plan(Date startDate, Integer days, Base2 base, List<Integer> startHour, List<Integer> endHour,
			Integer adultNum, Integer childNum) {

		return null;
	}

	/**
	 * 单个城市景点推荐
	 * 当当前时间小于游览时间时，选择评分最高的景点，获取景点游览时间，循环选择下一个景点直到游览时间结束
	 * @param startDate
	 * @param endDate
	 * @param base
	 * @param requireSpots
	 * @param excludeSpots
	 * @param pace
	 * @param adultNum
	 * @param childNum
	 * @param tags
	 * @param startHour
	 * @param endHour
	 * @return
	 */
	public List<BaseSpot> plan(Date startDate,Date endDate,Base2 base,List<BaseSpot> requireSpots,List<BaseSpot> excludeSpots,
							Integer pace,Integer adultNum,Integer childNum,List<Tag> tags,
							Integer startHour,Integer endHour){
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar calendar=Calendar.getInstance();
		calendar.setTime(startDate);
		if(calendar.get(Calendar.HOUR_OF_DAY)<startHour) {
			calendar.set(Calendar.HOUR_OF_DAY, startHour);
		}
		calendar=TimeJumpUtils.dayJump(calendar, startHour, endHour);
		Double lat=base.getLatitude();
		Double lon=base.getLongitude();
		//SpotDto spotDto;
		//List<SpotDto> spotDtoes=new ArrayList<SpotDto>();
		List<Tag> wholeTags = tagMapper.getTags();
		Map<String, Double> dayTagFrequencyStr = initTagFrequencyStr(wholeTags);
		Map<String, Integer> dayTagCountStr = new HashMap<String, Integer>();
		Queue<BaseSpot> spotQueue=recommendSpots(calendar.getTime(),endDate,startHour,endHour,base,requireSpots,excludeSpots,tags,dayTagFrequencyStr);
		List<BaseSpot> spots=new ArrayList<BaseSpot>();
		while(calendar.getTimeInMillis()<endDate.getTime()) {
			BaseSpot spot = chooseSpot(spotQueue, calendar.getTime(), dayTagCountStr,
					dayTagFrequencyStr,base);
			if(spot==null){
				spot=recommendSpot(calendar.getTime(),base.getLatitude(),base.getLongitude(),excludeSpots,dayTagCountStr,dayTagFrequencyStr,base);
			}
			updateDayTagCount(spot, dayTagCountStr);
			logger.debug("spot: " + JsonUtil.Obj2Json(spot));
			System.out.println(" before transport: "+format.format(calendar.getTime()));
			try {
				//spotDto = toSpotDto(calendar.getTime(),spot, adultNum,childNum);
				Double transportTime = getTransportTime(lat, lon,
						spot.getLatitude(), spot.getLongitude());
				if(spot.getName_zh()=="自由活动"){
					//calendar.set(Calendar.HOUR_OF_DAY, (int) (calendar.get(Calendar.HOUR_OF_DAY)));
				}else{
					//calendar.set(Calendar.HOUR_OF_DAY, (int) (calendar.get(Calendar.HOUR_OF_DAY) + transportTime));
					calendar.setTimeInMillis((long) (calendar.getTimeInMillis()+transportTime*3600000));
					lat=spot.getLatitude();
					lon=spot.getLongitude();
				}
				System.out.println(" after transport: "+format.format(calendar.getTime()));
				//context.setCurrentDate(calendar.getTime());
				//logger.debug("transportTime: "+transportTime);
				//spotDto.setStart_time(format.format(calendar.getTime()));
				spot.setStartDate(calendar.getTime());
				Double interval=Math.ceil(0.5 * (spot.getLong_play() + spot.getShort_play()));
				Integer interval_int = interval.intValue();
				//spotDto.setDuration(interval);
				calendar.set(Calendar.HOUR_OF_DAY, (int) (calendar.get(Calendar.HOUR_OF_DAY) + interval_int));
				//context.setCurrentDate(calendar.getTime());
				//spotDto.setEnd_time(format.format(calendar.getTime()));
				spot.setEndDate(calendar.getTime());
				//spotDtoes.add(spotDto);
				spots.add(spot);
			}catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(TimeJumpUtils.needLunchJump(calendar)) {
				calendar=TimeJumpUtils.lunchJump(calendar);
			}
			
			if(TimeJumpUtils.needDayJump(calendar, endHour)) {
				calendar=TimeJumpUtils.dayJump(calendar, startHour, endHour);
				dayTagCountStr.clear();
			}
		}
		return spots;
	}
	
	/**
	 * 多个城市景点推荐
	 * 循环每一个城市，获取推荐景点
	 * @param startDate
	 * @param days
	 * @param bases
	 * @param pace
	 * @param adultNum
	 * @param childNum
	 * @param tags
	 * @return
	 * @throws InstantiationException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws Exception
	 */
	public List<BaseSpot> plan2(Date startDate, List<Integer> days, List<Base2> bases, Integer pace, Integer adultNum,
			Integer childNum,List<Tag> tags) throws InstantiationException, InvocationTargetException, NoSuchMethodException, Exception {
		List<Date> dates=DateUtils.days2dates(startDate, days);
		List<BaseSpot> spots=new ArrayList<BaseSpot>();
		List<BaseSpot> citySpots;
		List<BaseSpot> requireSpots=new ArrayList<BaseSpot>();
		List<BaseSpot> excludeSpots=new ArrayList<BaseSpot>();
		initConfig();
		Integer startHour=getStartHour(pace);
		Integer endHour=getEndHour(pace);
		for(int i=0;i<dates.size()-1;i++) {
			citySpots=plan(dates.get(i),dates.get(i+1),bases.get(i),requireSpots,excludeSpots,pace,adultNum,childNum,tags,startHour,endHour);
			spots.addAll(citySpots);
			excludeSpots.addAll(citySpots);
		}
		return spots;
	}
	

	/**
	 * 景点推荐
	 * 首先根据景点流行度，标签匹配度选择景点
	 * 然后选择评分最高的景点进行推荐
	 * 最后根据tsp算法优化游览顺序
	 * @param startDate
	 * @param endDate
	 * @param startHour
	 * @param endHour
	 * @param base
	 * @param requireSpots
	 * @param totalSpots
	 * @param tags
	 * @param dayTagFrequencyStr
	 * @return
	 */
	private Queue<BaseSpot> recommendSpots(Date startDate,Date endDate,Integer startHour,Integer endHour,
			Base2 base,List<BaseSpot> requireSpots,List<BaseSpot> totalSpots,List<Tag> tags,Map<String,Double> dayTagFrequencyStr){ 
		List<BaseSpot> spots = getSpotsByBase(base);
		spots.removeAll(totalSpots); 
		logger.debug("base: " + base.getName_zh_url() + ", spots: " + spots.size());
		// List<Tag> tags = new ArrayList<Tag>();
		BaseSpotScorer spotScorer = new BaseSpotScorer(spots, requireSpots, tags, base.getLatitude(),
				base.getLongitude());
		spotScorer.score(spots);
		List<BaseSpot> chooseSpots = extractTravelSpots(spots, startDate, endDate,
				dayTagFrequencyStr,base,startHour,endHour);
		logger.debug("choosed spots: "+JsonUtil.Obj2Json(chooseSpots));
		LocationPlanner planner = new LocationPlanner();
		Planned planAlgorithm = new TspLocationPlanner();
		planner.setPlanner(planAlgorithm);
		int[] plannedSpotOrder = planner.performPlan(chooseSpots, chooseSpots.get(0));
		int startIndex = ArrayUtil.getIndex(plannedSpotOrder, 0);
		List<BaseSpot> reOrderedSpots = ListUtils.reorder(chooseSpots, plannedSpotOrder, startIndex);
		logger.debug("reOrderedSpots: " + JsonUtil.Obj2Json(reOrderedSpots));
		Queue<BaseSpot> spotQueue = new LinkedList<BaseSpot>();
		spotQueue.addAll(reOrderedSpots);
		return spotQueue;
	}

	/**
	 * 推荐单个景点
	 * @param date
	 * @param latitude
	 * @param longitude
	 * @param excludeSpots
	 * @param dayTagCountStr
	 * @param dayTagFrequencyStr
	 * @param base
	 * @return
	 */
	private BaseSpot recommendSpot(Date date, double latitude, double longitude, Collection<BaseSpot> excludeSpots,
			Map<String, Integer> dayTagCountStr, Map<String, Double> dayTagFrequencyStr, Base2 base) {
		List<BaseSpot> spotsInArea = getSpotsByPoint(latitude, longitude, HOURDISTANCE);
		// logger.debug("before remove recommended
		// spots-------------------------------------------------------spots size:
		// "+spotsInArea.size()+", total size: "+excludeSpots.size());
		spotsInArea.removeAll(excludeSpots);
		// logger.debug("after remove recommended
		// spots-------------------------------------------------------spots size:
		// "+spotsInArea.size()+", total size: "+excludeSpots.size());
		List<BaseSpot> requireSpots = new ArrayList<BaseSpot>();
		List<Tag> tags = new ArrayList<Tag>();
		BaseSpotScorer spotScorer = new BaseSpotScorer(spotsInArea, requireSpots, tags, latitude, longitude);
		spotScorer.score(spotsInArea);
		Queue<BaseSpot> spotsQueue = new PriorityQueue<BaseSpot>();
		spotsQueue.addAll(spotsInArea);
		BaseSpot spot = chooseSpot(spotsQueue, date, dayTagCountStr, dayTagFrequencyStr, base);
		return spot;
	}


	/**
	 * 废弃方法
	 * @param spots
	 * @param startDate
	 * @param day
	 * @param dayTagFrequencyStr
	 * @param base
	 * @param startHour
	 * @param endHour
	 * @return
	 */
	private List<BaseSpot> extractTravelSpots(List<BaseSpot> spots, Date startDate, Integer day,
			Map<String, Double> dayTagFrequencyStr,Base2 base,int startHour,int endHour) {
		logger.debug("extravelTravelSpots starting");
		Date endDate=DateUtils.day2date(startDate, day);
		return extractTravelSpots(spots,startDate,endDate,dayTagFrequencyStr,base,startHour,endHour);
	}
	private List<BaseSpot> extractTravelSpots(List<BaseSpot> spots, Date startDate, Date endDate,
			Map<String, Double> dayTagFrenquencyStr,Base2 base,int startHour,int endHour) {
		logger.debug("extravelTravelSpots starting");
		//Date endDate=DateUtils.day2date(startDate, day);
		List<BaseSpot> result = new ArrayList<BaseSpot>();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startDate);
		Queue<BaseSpot> spotQueue = new PriorityQueue<BaseSpot>(Collections.reverseOrder());
		spotQueue.addAll(spots);
		BaseSpot spot;
		Map<String, Integer> dayTagCountStr = new HashMap<String, Integer>();
		calendar=TimeJumpUtils.dayJump(calendar, startHour, endHour);
		while(calendar.getTimeInMillis()<endDate.getTime()) {
			spot = chooseSpot(spotQueue, calendar.getTime(), dayTagCountStr, dayTagFrenquencyStr,base);
			updateDayTagCount(spot, dayTagCountStr);
			// Integer
			// transportTime=getTransportTime(context.getCurrentLat(),context.getCurrentLon(),spot.getLatitude(),spot.getLongitude());
			Integer playTime = (int) Math.ceil(0.5 * (spot.getLong_play() + spot.getShort_play()));
			calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + playTime);
			result.add(spot);
			calendar=TimeJumpUtils.dayJump(calendar, startHour, endHour);
			if(TimeJumpUtils.needDayJump(calendar, endHour)) {
				dayTagCountStr.clear();
			}
		}
		logger.debug("extractTravelSpots finished");
		return result;
	}

	/**
	 * 更新每天游览的标签类型数量
	 * 某类型的景点每天游览的数量不能超过一定数目
	 * @param spot
	 * @param dayTagCount
	 */
	private void updateDayTagCount(BaseSpot spot, Map<String, Integer> dayTagCount) {
		// Map<String, Integer> dayTagCount = context.getDayTagCountStr();
		if (spot.getTags() != null) {
			for (Tag tag : spot.getTags()) {
				String tagName = tag.getNamezh();
				if (dayTagCount.containsKey(tagName)) {
					dayTagCount.put(tagName, dayTagCount.get(tagName) + 1);
					logger.debug("" + tagName + ": " + dayTagCount.get(tagName) + ", " + spot.getName_zh() + ", spot: "
							+ JsonUtil.Obj2Json(spot));
				} else {
					dayTagCount.put(tagName, 1);
				}
			}
		}
	}

	/**
	 * 根据标签，是否开放，是否有产品选择景点。
	 * 如果当天游览的景点标签数量没超过指定数量，该景点可选
	 * 如果当天景点开放，该景点可选
	 * 如果该景点有产品，该景点可选。
	 * @param spotQueue
	 * @param date
	 * @param dayTagCountStr
	 * @param dayTagFrequencyStr
	 * @param base
	 * @return
	 */
	private BaseSpot chooseSpot(Queue<BaseSpot> spotQueue, Date date, Map<String, Integer> dayTagCountStr,
			Map<String, Double> dayTagFrequencyStr, Base2 base) {
		BaseSpot spot = (BaseSpot) spotQueue.poll();
		if (spot == null) {
			logger.error("run out of spots");
			spot = BaseSpotFactory.getWander(base);
			return spot;
		}
		logger.debug("current date: " + date + ", name_zh: " + spot.getName_zh() + ", uid: " + spot.getUid());
		List<BaseSpot> unFitSpots = new ArrayList<BaseSpot>();
		boolean isOpen = isOpen(spot, date);
		boolean tagChoose = tagChoose(spot, dayTagCountStr, dayTagFrequencyStr);
		boolean productChoose=productFlag(date,spot);
		while ((!isOpen || !tagChoose||!productChoose) && !spotQueue.isEmpty()) {
			logger.debug("unfit! isOpen: " + isOpen + ", tagChoose: " + tagChoose + ", product choose: "+productChoose+" " + spot.getName_zh());
			unFitSpots.add(spot);
			spot = (BaseSpot) spotQueue.poll();
			isOpen = isOpen(spot, date);
			tagChoose = tagChoose(spot, dayTagCountStr, dayTagFrequencyStr);
			productChoose=productChoose(date,spot);
		}
		spotQueue.addAll(unFitSpots);
		if (!isOpen || !tagChoose||!productChoose) {
			logger.info("run out of spots");
			return BaseSpotFactory.getWander(base);
		}
		logger.debug("choosed isOpen: " + isOpen + ", tagChoose: " + tagChoose +", product choose: "+productChoose+ " " + spot.getName_zh());
		return spot;
	}
	
	/**
	 * 选择景点是否考虑产品标志
	 * 我们选择景点时可以强制考虑产品，例如当只有有门票或在说有辛巴达门票时我们才选择这个景点。
	 * 可在配置文件中进行配置。
	 * @param date
	 * @param spot
	 * @return
	 */
	private boolean productFlag(Date date, BaseSpot spot) {
		boolean flag=true;
		if(StaticData.getConfigName("productChoose")!=null) {
			flag=Boolean.valueOf(StaticData.getConfigName("productChoose"));
		}
		logger.debug("product flag: "+flag);
		if(!flag) {
			logger.debug("test");
			return true;
		}else {
			return productChoose(date,spot);
		}
	}
	
	/**
	 * 当产品有库存则选择景点
	 * @param date
	 * @param spot
	 * @return
	 */
	private boolean productChoose(Date date,BaseSpot spot) {
		if(!hasProduct(spot)) {
			return true;
		}else {
			return isStocked(date,spot);			
		}
	}
	/**
	 * 判断某景点下是否有产品
	 * @param spot
	 * @return
	 */
	private boolean hasProduct(BaseSpot spot) {
		Integer size=productMapper.getProductSizeByParentUid(spot.getUid(), null);
		if(size>0) {
			return true;
		}else {
			return false;
		}
	}
	
	/**
	 * 判断某景点是否 有库存
	 * @param date
	 * @param spot
	 * @return
	 */
	private boolean isStocked(Date date,BaseSpot spot) {
		SimpleDateFormat format=new SimpleDateFormat(DateUtils.DATEFORMATYYMMDD);
		Integer count=baseSpotMapper.getStockSizeByDateUid(format.format(date), spot.getUid());
		if(count>0) {
			return true;
		}else {
			return false;
		}
	}
	

	/**
	 * 判断某景点标签是否超过当天游览标签数。
	 * @param spot
	 * @param tagsCount
	 * @param tagsFrequency
	 * @return
	 */
	private Boolean tagChoose(BaseSpot spot, Map<String, Integer> tagsCount, Map<String, Double> tagsFrequency) {
		Boolean choose = true;
		logger.debug("------------------------------------tagChoose----------------------- " + spot.getTags().size());
		if (spot.getTags() == null || spot.getTags().size() == 0) {
			logger.debug("spot.getTags==null situation: " + JsonUtil.Obj2Json(spot));
			return choose;
		}
		// Map<String, Integer> tagsCount = context.getDayTagCountStr();
		// Map<String, Double> tagsFrequency = context.getDayTagFrequencyStr();
		/*
		 * for(String key:tagsFrequency.keySet()){
		 * logger.debug(key+": "+tagsFrequency.get(key)); }
		 */
		for (Tag tag : spot.getTags()) {
			String tagName = tag.getNamezh();
			logger.debug("tagName: " + tagName + ", frequency: " + tagsFrequency.get(tagName) + ", count: "
					+ tagsCount.get(tagName));
			if (tagsCount.containsKey(tagName)) {
				// tagsCount.put(tagName,tagsCount.get(tagName) + 1);
				if (tagsCount.get(tagName) >= tagsFrequency.get(tagName)) {
					logger.debug("count>frequency, choose=false, tagName: " + tagName + ", frequency: "
							+ tagsFrequency.get(tagName));
					choose = false;
					return choose;
				}
			} else {
				if (tagsFrequency.get(tagName) == null) {
					logger.debug("noFrequency, choose=true");
					return choose;
				} else if (tagsFrequency.get(tagName) > 0) {
					return choose;
				} else {
					logger.debug("frequency <0 choose =false");
					choose = false;
					return choose;
				}
			}
		}
		logger.debug("other situation, choose=" + choose);
		return choose;
	}

	/**
	 * 判断当天景点是否开放
	 * @param spot
	 * @param date
	 * @return
	 */
	private Boolean isOpen(BaseSpot spot, Date date) {
		Calendar ca = Calendar.getInstance();
		ca.setTime(date);
		Integer currentDay = ca.get(Calendar.DAY_OF_WEEK);
		Integer currentHour = ca.get(Calendar.HOUR_OF_DAY);
		Integer currentMonth = ca.get(Calendar.MONTH);
		Integer currentDayOfMonth = ca.get(Calendar.DAY_OF_MONTH);

		String businessHour;
		if (spot.getRule_open() == null) {
			logger.debug("isOpen: true");
			return true;
		}
		businessHour = spot.getRule_open();
		String[] closeRuleDates = new String[0];
		String[] closeRuleMonthes = new String[0];
		OpenRule[] openRuleArray;
		logger.debug("uid: " + spot.getUid() + ", name: " + spot.getName_zh());
		if (spot.getRule_close_day() != null) {
			closeRuleDates = JsonUtil.toBean(spot.getRule_close_day(), String[].class);
			logger.debug("closeday: " + spot.getRule_close_day());
		}
		if (spot.getRule_close_month() != null) {
			logger.debug("closemonth: " + spot.getRule_close_month());
			closeRuleMonthes = JsonUtil.toBean(spot.getRule_close_month(), String[].class);
		}
		if (businessHour != null) {
			openRuleArray = JsonUtil.toBean(businessHour, OpenRule[].class);
			List<OpenRule> openRules = Arrays.asList(openRuleArray);
			logger.debug("openRules: " + JsonUtil.Obj2Json(openRules) + ", size: " + openRules.size());
			List<Integer> openDays = getOpenDays(openRules);
			if (!MonthOpen(closeRuleMonthes, currentMonth)) {
				logger.debug("month close");
				return false;
			} else if (!DateOpen(closeRuleDates, currentMonth, currentDayOfMonth)) {
				logger.debug("date close");
				return false;
			} else {
				if (openRules.size() == 0) {
					logger.debug("norule, isOpen true");
					return true;
				} else if (openDays.contains(currentDay)) {
					return hourOpen(openRules, currentDay, currentHour);
				} else {
					logger.debug("other situation, isOpen false");
					return false;
				}
			}
		}
		return true;
	}

	private Boolean DateOpen(String[] closeRuleDates, Integer currentMonth, Integer currentDayOfMonth) {
		String[] dateArray;
		for (String date : closeRuleDates) {
			dateArray = date.split("\\.");
			if (Integer.valueOf(dateArray[0]) == currentMonth && Integer.valueOf(dateArray[1]) == currentDayOfMonth) {
				return false;
			}
		}
		return true;
	}

	private Boolean MonthOpen(String[] closeRuleMonth, Integer currentMonth) {
		Integer month;
		for (String monthStr : closeRuleMonth) {
			month = Integer.valueOf(monthStr);
			if (month == currentMonth) {
				return false;
			}
		}
		return true;
	}

	private List<Integer> getOpenDays(List<OpenRule> openRules) {
		List<Integer> openDays = new ArrayList<Integer>();
		for (int i = 0; i < openRules.size(); i++) {
			if (openRules.get(i).getOpen_time() != null) {
				if (openRules.get(i).getDay() != 7) {
					openDays.add(openRules.get(i).getDay() + 1);
				} else {
					openDays.add(1);
				}
			}
		}
		return openDays;
	}


	private Boolean hourOpen(List<OpenRule> openRules, Integer currentDay, Integer currentHour) {
		if (currentDay == 1) {
			return hourOpen2(openRules, currentDay, currentHour, 6);
		} else {
			return hourOpen2(openRules, currentDay, currentHour, currentDay - 2);
		}
	}

	private Boolean hourOpen2(List<OpenRule> openRules, Integer currentDay, Integer currentHour, Integer openDay) {
		for (OpenHour openHour : openRules.get(openDay).getOpen_time()) {
			Double startHour = hour2Double(openHour.getStart());
			Double endHour = hour2Double(openHour.getEnd());
			// a small bug need to fix later, we need not only to make sure
			// current hour <=endHour, but need currentHour << endHour;
			if (startHour <= currentHour && currentHour < endHour - 1) {
				return true;
			}
		}
		return false;
	}

	private Double hour2Double(String hour) {
		String[] tokens = hour.split(":");
		Double hourInteger = Double.parseDouble(tokens[0]);
		Double hourDigits = Double.parseDouble(tokens[1]) / 60;
		return hourInteger + hourDigits;
	}

	/**
	 * 获取两点间交通时间
	 * @param lat1
	 * @param lon1
	 * @param lat2
	 * @param lon2
	 * @return
	 */
	private Double getTransportTime(Double lat1, Double lon1, Double lat2, Double lon2) {
		String speedStr=StaticData.getConfigName(TRANSPORT_SPEED);
		if(speedStr==null) {
			speedStr="80.0";
		}
		Double speed=Double.parseDouble(speedStr);
		return  (DistanceUtils.get_distance(lat1, lon1, lat2, lon2) / speed);
	}

	public List<BaseSpot> getSpotsByPoint(Base2 base){
		Double lat = base.getLatitude();
		Double lon = base.getLongitude();
		return getSpotsByPoint(lat, lon, DAYDISTANCE);
	}
	
	public List<BaseSpot> getSpotsByBase(Base2 base) {
		int spotChooseStrategy=0;
		String strategy=StaticData.getConfigName(SPOT_CHOOSE_STRATEGY);
		if(strategy!=null) {
			spotChooseStrategy=Integer.parseInt(strategy);
		}
		switch(spotChooseStrategy) {
		case 0:{return baseSpotMapper.getSpotTagsByPoi(base.getUid());}
		case 1:{return getSpotsByPoint(base);}
		default:{return null;}
		}
	}

	public List<BaseSpot> getSpotsByPoint(Double latitude, Double longitude, Double distance) {
		Double diffLat = DistanceUtils.getDiffLat(latitude, longitude, distance);
		Double diffLon = DistanceUtils.getDiffLon(latitude, longitude, distance);
		return baseSpotMapper.getSpotTagsByArea(latitude - diffLat, latitude + diffLat, longitude - diffLon,
				longitude + diffLon);
	}

	private List<BaseSpot> getSpotsByBaseStock(Date date,Base2 base){
		Double lat = base.getLatitude();
		Double lon = base.getLongitude();
		return getSpotsByPointStock(date,lat,lon,DAYDISTANCE);	
	}
	private List<BaseSpot> getSpotsByPointStock(Date date,Double latitude,Double longitude,Double distance){
		SimpleDateFormat simpleDateFormat=new SimpleDateFormat(DateUtils.DATEFORMATYYMMDD);
		Double diffLat = DistanceUtils.getDiffLat(latitude, longitude, distance);
		Double diffLon = DistanceUtils.getDiffLon(latitude, longitude, distance);
		return baseSpotMapper.getSpotTagsStockByArea(simpleDateFormat.format(date),latitude - diffLat, latitude + diffLat, longitude - diffLon, longitude + diffLon);		
	}


	/**
	 * 初始化标签频率
	 * @param tags
	 * @return
	 */
	public Map<Tag, Double> initTagFrequency(List<Tag> tags) {
		Map<Tag, Double> tagFrequency = new HashMap<Tag, Double>();
		for (int i = 0; i < tags.size(); i++) {
			tagFrequency.put(tags.get(i), tags.get(i).getFrequency());
		}
		return tagFrequency;
	}

	public Map<String, Double> initTagFrequencyStr(List<Tag> tags) {
		Map<String, Double> tagFrequency = new HashMap<String, Double>();
		for (int i = 0; i < tags.size(); i++) {
			tagFrequency.put(tags.get(i).getNamezh(), tags.get(i).getFrequency());
		}
		return tagFrequency;
	}

	/**
	 * 初始化配置
	 */
	private void initConfig() {

		Optional<String> startHourShortStrO=Optional.ofNullable(StaticData.getConfigName("starthourshort"));
		Optional<String> startHourMediumStrO=Optional.ofNullable(StaticData.getConfigName("starthourmedium"));
		Optional<String> startHourLongStrO=Optional.ofNullable(StaticData.getConfigName("starthourlong"));
		Optional<String> endHourShortStrO=Optional.ofNullable(StaticData.getConfigName("endhourshort"));
		Optional<String> endHourMediumStrO=Optional.ofNullable(StaticData.getConfigName("endhourmedium"));
		Optional<String> endHourLongStrO=Optional.ofNullable(StaticData.getConfigName("endhourlong"));
		 startHourShort=Double.parseDouble(startHourShortStrO.orElse("10.0"));
		 startHourMedium=Double.parseDouble(startHourMediumStrO.orElse("9.0"));
		 startHourLong=Double.parseDouble(startHourLongStrO.orElse("8.0"));
		 endHourShort=Double.parseDouble(endHourShortStrO.orElse("16.0"));
		 endHourMedium=Double.parseDouble(endHourMediumStrO.orElse("17.0"));
		 endHourLong=Double.parseDouble(endHourLongStrO.orElse("18.0")); 
				
				
	}
	
	/**
	 * 初始化出行节奏
	 * @param context
	 * @param pace
	 */
	private void initPace(TourContext context, Integer pace) {
		switch (pace.intValue()) {
		case 0: {
			context.setStartHour(10.0);
			context.setEndHour(16.0);
			break;
		}
		case 1: {
			context.setStartHour(9.0);
			context.setEndHour(17.0);
			break;
		}
		case 2: {
			context.setStartHour(8.0);
			context.setEndHour(18.0);
		}
		}
	}

	/**
	 * 计算一天开始游览时间，不同的游览节奏有不同的开始游览时间
	 * @param pace
	 * @return
	 */
	private Integer getStartHour(Integer pace) {
		switch (pace.intValue()) {
		case 0:{
			return startHourShort.intValue();}
			//break;}
		case 1:{
			return startHourMedium.intValue();}
			//break;}
		case 2:{
			return startHourLong.intValue();}
		}
		return null;
	}
	
	/**
	 * 计算一天结束游览时间
	 * @param pace
	 * @return
	 */
	private Integer getEndHour(Integer pace) {
		switch (pace.intValue()) {
		case 0:{
			return endHourShort.intValue();}
			//break;}
		case 1:{
			return endHourMedium.intValue();}
			//break;}
		case 2:{
			return endHourLong.intValue();}
		}
		return null;
	}
/*	public List<SpotDto> plan(ItineraryParameter param) throws InstantiationException, InvocationTargetException, NoSuchMethodException, Exception {
		// TODO Auto-generated method stub
		logger.debug("inputs: " + JsonUtil.Obj2Json(param));
		return plan(param.getStartTime(), param.getDays(), param.getDestinations(), param.getPace(),
				param.getAdultNum(), param.getChildNum(),param.getTags());
	}*/
	public List<BaseSpot> plan2(ItineraryParameter param) throws InstantiationException, InvocationTargetException, NoSuchMethodException, Exception {
		// TODO Auto-generated method stub
		logger.debug("inputs: " + JsonUtil.Obj2Json(param));
		return plan2(param.getStartTime(), param.getDays(), param.getDestinations(), param.getPace(),
				param.getAdultNum(), param.getChildNum(),param.getTags());
	}

	public SimpleJsonResult plan(String param)
			throws InstantiationException, InvocationTargetException, NoSuchMethodException, Exception {
		// TODO Auto-generated method stub
		// Map<String,Object> map=JsonUtil.toBean(param, Map.class);
		SimpleJsonResult simpleJsonResult = new SimpleJsonResult();
		ItineraryInput input = JsonUtil.toBean(param, ItineraryInput.class);
		ItineraryParameter parameter = planService.convertItineraryParameter(input);
		List<BaseSpot> result = plan2(parameter);
		// System.out.println(map.get("depart_time"));
		logger.debug(JsonUtil.Obj2Json(result));
		simpleJsonResult.setStatus(SimpleJsonResult.STATUS_OK);
		simpleJsonResult.setData(result);

		return simpleJsonResult;
	}

	@Resource
	private SpotMapper spotMapper;
	@Resource
	private PlanService planService;
	@Resource
	private BaseSpotMapper baseSpotMapper;
	@Resource
	private ProductMapper productMapper;
/*	@Resource
	private SProductTypeSecondaryMapper sProductTypeSecondaryMapper;
	@Resource
	private ImgUploadMapper imgUploadMapper;
	@Resource
	private SCurrencyMapper sCurrencyMapper;
	@Resource
	private ProductItemMapper productItemMapper;*/
	@Resource
	private TagMapper tagMapper;
	private Logger logger = Logger.getLogger(getClass());

	public static final Double HOURDISTANCE = 7.0;
	public static final Double DAYDISTANCE = 30.0;
	public static final Double DAYSDISTANCE = 100.0;
	
	public static Double startHourShort=10.0;
	public static Double startHourMedium=9.0;
	public static Double startHourLong=8.0;
	public static Double endHourShort=16.0;
	public static Double endHourMedium=17.0;
	public static Double endHourLong=18.0;
	
	public static final String SPOT_CHOOSE_STRATEGY="spotChooseStrategy";
	public static final String TRANSPORT_SPEED="transportspeed";
}
