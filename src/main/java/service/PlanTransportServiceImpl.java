package com.sinbad.itinerary.service.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.sinbad.itinerary.dao.base.Base2Mapper;
import com.sinbad.itinerary.dao.base.BaseAirportMapper;
import com.sinbad.itinerary.dao.base.BaseTransportMapper;
import com.sinbad.itinerary.dao.base.BaseTransportWayMapper;
import com.sinbad.itinerary.logic.planner.LocationPlanner;
import com.sinbad.itinerary.logic.planner.Planned;
import com.sinbad.itinerary.logic.planner.TspLocationPlanner;
import com.sinbad.itinerary.model.SimpleJsonResult;
import com.sinbad.itinerary.model.bean.BaseTransportInfo;
import com.sinbad.itinerary.model.bean.BaseTransportPath;
import com.sinbad.itinerary.model.bean.Flight;
import com.sinbad.itinerary.model.bean.TourContext;
import com.sinbad.itinerary.model.dto.input.itinerary.ItineraryInput;
import com.sinbad.itinerary.model.dto.itinerary.ItineraryParameter;
import com.sinbad.itinerary.model.dto.output.itinerary.LocateDto;
import com.sinbad.itinerary.model.dto.output.vehicle.AirportTransferOutput;
import com.sinbad.itinerary.model.dto.output.vehicle.CarOutput;
import com.sinbad.itinerary.model.dto.output.vehicle.FlightOutput;
import com.sinbad.itinerary.model.dto.output.vehicle.FlightSegment;
import com.sinbad.itinerary.model.dto.output.vehicle.TransportOutput;
import com.sinbad.itinerary.model.entity.base.Base2;
import com.sinbad.itinerary.model.entity.base.BaseAirport;
import com.sinbad.itinerary.model.entity.base.BaseCity;
import com.sinbad.itinerary.model.entity.base.BaseTransportWay;
import com.sinbad.itinerary.service.PlanFlightService;
import com.sinbad.itinerary.service.PlanService;
import com.sinbad.itinerary.service.PlanTransportService;
import com.sinbad.itinerary.util.ArrayUtil;
import com.sinbad.itinerary.util.DateUtils;
import com.sinbad.itinerary.util.DistanceUtils;
import com.sinbad.itinerary.util.JsonUtil;
import com.sinbad.itinerary.util.ListUtils;
import com.sinbad.itinerary.util.MapUtil;


/**
 * 交通规划总类，用于规划交通，调用其它交通规划类.
 * 功能
 * 1 根据实际情况选择飞机，汽车或其它交通。
 * 2 调用PlanFlightService 规划机票
 * @author owen
 *
 */
@Service
public class PlanTransportServiceImpl implements PlanTransportService {

	@Override
	public SimpleJsonResult plan(String param) throws Exception {
		// TODO Auto-generated method stub
		// Map<String,Object> map=JsonUtil.toBean(param, Map.class);
		SimpleJsonResult simpleJsonResult = new SimpleJsonResult();
		ItineraryInput input = JsonUtil.toBean(param, ItineraryInput.class);
		ItineraryParameter parameter = planService.convertItineraryParameter(input);
		List<TransportOutput> result = plan(parameter);
		// System.out.println(map.get("depart_time"));
		logger.debug("flights: " + JsonUtil.Obj2Json(result));
		simpleJsonResult.setStatus(SimpleJsonResult.STATUS_OK);
		simpleJsonResult.setData(result);

		return simpleJsonResult;
	}

	@Override
	public List<TransportOutput> plan(ItineraryParameter param) throws Exception {
		// TODO Auto-generated method stub
		logger.debug("input param: "+JsonUtil.Obj2Json(param));
		
		return plan2(param.getStartTime(), param.getDepartCity(), param.getReturnCity(), param.getDays(),
				param.getDestinations(),param.getAdultNum(),param.getChildNum());
	}


	public List<TransportOutput> plan2(Date date, Base2 departCity, Base2 arriveCity, 
			List<Integer> days, List<Base2> cities,Integer adultNum,Integer childNum) throws Exception {
		List<Date> dates = DateUtils.days2dates2(date, days);
		List<Base2> flightCities = new ArrayList<Base2>();
		flightCities.add(departCity);
		flightCities.addAll(cities);
		flightCities.add(arriveCity);
		logger.debug("flightCityes: " + JsonUtil.Obj2Json(flightCities));
		return plan(dates, flightCities,adultNum,childNum);
	}
	

	/** 
	 * 规划两城市间的交通.
	 * 如果城市间距离需要航班且有航班则使用飞机。
	 * 如果城市间距离需要航班，但没有航班则找就近机场
	 * 如果城市间距离不需要航班，则用汽车。
	 * @see com.sinbad.itinerary.service.PlanTransportService#plan(java.util.List, java.util.List, java.lang.Integer, java.lang.Integer)
	 */
	@Override
	
	public List<TransportOutput> plan(List<Date> dates, List<Base2> bases,Integer adultNum,Integer childNum) throws Exception {
		// TODO Auto-generated method stub
	
		List<Flight> flights = new ArrayList<Flight>();
		List<TransportOutput> transports = new ArrayList<TransportOutput>();
		Base2 depart;
		Base2 arrive;
		Boolean hasFlight;
		Boolean needFlight;
		int stage=0;
		for (int i = 0; i < bases.size() - 1; i++) {
			stage=genTripStage(i,bases.size()-2);
			depart = bases.get(i);
			arrive = bases.get(i + 1);
			hasFlight = planFlightService.hasFlightByArea(depart, arrive);
			needFlight = planFlightService.needFlight(depart, arrive);
			if (hasFlight && needFlight) {
				addFlight(dates.get(i),flights,transports,depart,arrive,adultNum,childNum,stage);
			} else if (!hasFlight && needFlight) {
				// add flight
				addFlightIndirect(dates.get(i),depart,arrive,adultNum,childNum,stage,flights,transports);
			} else {
				logger.debug("car situation, using car transportation, hasFlight: "+hasFlight+", needFlight: "+needFlight);
				//addCar(dates.get(i),transports,depart,arrive);
				addOtherTransport(dates.get(i),transports,depart,arrive);
			}
			logger.info("departCity: " + depart.getName_zh() + ", arriveCity: " + arrive.getName_zh() + ", hasFlight: "
					+ hasFlight + ", needFlight: " + needFlight);
		}
		addTransportName(transports);
		return transports;
	}
	
	private void addFlightIndirect(Date date,Base2 depart,Base2 arrive,Integer adultNum,Integer childNum,Integer stage,List<Flight> flights,List<TransportOutput> transports) 
	throws Exception{
		Base2 airportArrive=planFlightService.getNearestAirport(arrive);
		Base2 airportDepart=planFlightService.getNearestAirport(depart);
		if(planFlightService.worthFly(depart, arrive, airportDepart, airportArrive)){
			if(planFlightService.hasAirport(depart)){
				logger.debug("depart has airport and arrive has not airport");
				addFlight(date,flights,transports,depart,airportArrive,adultNum,childNum,stage);
			}else if(planFlightService.hasAirport(arrive)){
				logger.debug("arrive has airport and depart has not airport");
				addFlight(date,flights,transports,airportDepart,arrive,adultNum,childNum,stage);
			}else{
				logger.debug("arrive and depart has no airport");
				addFlight(date,flights,transports,airportDepart,airportArrive,adultNum,childNum,stage); 
			}					
		}else{
			logger.debug("transport situation, using transportation");
			addTransport(date,transports,depart,arrive);
		}
	}
	
	/**
	 * 获得旅行阶段
	 * 0代表第一个城市
	 * 1代表中间城市
	 * 2代表最后一个城市
	 * @param i
	 * @param total
	 * @return
	 */
	private int genTripStage(int i,int total){
		if(i==0){
			return 0;
		}else if(i<total){
			return 1;
		}else{
			return 2;
		}
	}
	
	/**
	 * 获取主要交通方式名称
	 * @param transports
	 */
	private void addTransportName(List<TransportOutput> transports) {
		List<BaseTransportWay> ways=baseTransportWayMapper.getAllBaseTransportWay();
		Map<Integer,String>wayMap=new HashMap<Integer,String>();
		for(BaseTransportWay ele:ways) {
			wayMap.put(ele.getId(), ele.getName());
		}
		for(TransportOutput ele:transports) {
			ele.setTypeName(wayMap.get(ele.getType()));
		}
	}
	
	/**
	 * 规划两城市非飞机交通
	 * 如果数据库里的城市交通表有录入两城市间的交通，则使用数据库里的交通方式。
	 * 否则使用汽车
	 * @param date
	 * @param transports
	 * @param depart
	 * @param arrive
	 */
	private void addOtherTransport(Date date,List<TransportOutput> transports, Base2 depart,Base2 arrive) {
		List<BaseTransportInfo> baseTransportInfoes=baseTransportMapper.queryAllTransportPath(depart.getUid(), arrive.getUid());
		logger.debug("transportInfo: "+JsonUtil.Obj2Json(baseTransportInfoes));
		if(baseTransportInfoes.size()>0) {
			addCustomTransport(date,transports,depart,arrive,baseTransportInfoes.get(0));
		}else {
			addCar(date,transports,depart,arrive);
		}
	}
	
	/**
	 * 使用数据库里城市交通表的交通
	 * @param date
	 * @param transports
	 * @param depart
	 * @param arrive
	 * @param transportInfo
	 */
	private void addCustomTransport(Date date,List<TransportOutput> transports,Base2 depart,Base2 arrive,BaseTransportInfo transportInfo) {
		TransportOutput transportOutput=toCustomTransportOutput(date,depart,arrive,transportInfo);
		transports.add(transportOutput);
	}
	
	/**
	 * 规划飞机
	 * @param date
	 * @param flights
	 * @param transports
	 * @param depart
	 * @param arrive
	 * @param adultNum
	 * @param childNum
	 * @param stage
	 * @throws Exception
	 */
	private void addFlight(Date date,List<Flight> flights,List<TransportOutput> transports,Base2 depart,Base2 arrive,Integer adultNum,Integer childNum,int stage) throws Exception{
		Flight flight = planFlightService.plan(date, depart, arrive,adultNum,childNum,stage);
		TransportOutput transportOutput;
		if(flight!=null){
			transportOutput = toFlightOutput(flight, depart, arrive);
			transportOutput.setType(TransportOutput.FLIGHT);
			transports.add(transportOutput);
			flights.add(flight);					
		}else{
			addTransport(date,transports,depart,arrive);
			logger.error("flight interface error");
		}
	}
	
	/**
	 * 规划 汽车
	 * @param date
	 * @param transports
	 * @param depart
	 * @param arrive
	 */
	private void addCar(Date date,List<TransportOutput> transports,Base2 depart,Base2 arrive){
		TransportOutput transportOutput = toCarOutput(date, depart, arrive);
		transportOutput.setType(TransportOutput.CAR);
		transports.add(transportOutput);		
	}
	
	private void addTransport(Date date,List<TransportOutput> transports,Base2 depart, Base2 arrive){
		TransportOutput transportOutput = toTransportOutput(date, depart, arrive);
		transports.add(transportOutput);
	}
	
	/**
	 * 添加汽车交通
	 * @param departDate
	 * @param departBase
	 * @param arriveBase
	 * @return
	 */
	private TransportOutput toCarOutput(Date departDate, Base2 departBase, Base2 arriveBase) {
		TransportOutput car = new CarOutput();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar ca = Calendar.getInstance();
		ca.setTime(departDate);
		ca.set(Calendar.HOUR_OF_DAY, TourContext.STARTHOUR.intValue());
		car.setArrive(toLocateDto(arriveBase));
		car.setDepart(toLocateDto(departBase));
		car.setType(TransportOutput.CAR);
		car.setStart_time(format.format(ca.getTime()));
		Double hour = DistanceUtils.getDistance(departBase, arriveBase) / 80;
		car.setEnd_time(format.format(new Date((long) (ca.getTimeInMillis() + hour * 3600000))));
		return car;
	}

	/**
	 * 添加通用交通
	 * @param departDate
	 * @param departBase
	 * @param arriveBase
	 * @return
	 */
	private TransportOutput toTransportOutput(Date departDate, Base2 departBase, Base2 arriveBase){
		TransportOutput output=new TransportOutput();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar ca = Calendar.getInstance();
		ca.setTime(departDate);
		ca.set(Calendar.HOUR_OF_DAY, TourContext.STARTHOUR.intValue());
		output.setArrive(toLocateDto(arriveBase));
		output.setDepart(toLocateDto(departBase));
		output.setStart_time(format.format(ca.getTime()));
		Double hour = DistanceUtils.getDistance(departBase, arriveBase) / 80;
		output.setEnd_time(format.format(new Date((long) (ca.getTimeInMillis() + hour * 3600000))));
		return output;
	}
	
	/**
	 * 航班格式转换
	 * @param flight
	 * @param depart
	 * @param arrive
	 * @return
	 */
	private TransportOutput toFlightOutput(Flight flight, Base2 depart, Base2 arrive) {
		FlightOutput flightOutput = new FlightOutput();
		LocateDto l1 = toLocateDto(depart);
		LocateDto l2 = toLocateDto(arrive);
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		flightOutput.setType(TransportOutput.FLIGHT);
		flightOutput.setDepart(l1);
		flightOutput.setArrive(l2);
		flightOutput.setStart_time(format.format(flight.getDepartTime()));
		flightOutput.setEnd_time(format.format(flight.getArriveTime()));
		flightOutput.setAttach(flight.getRouting());
		flightOutput.setAttach2(flight.getAttach());
		flightOutput.setSegments(generateFlightSegment(flight));
		flightOutput.setArriveAirportName(flight.getArriveAirportName());
		flightOutput.setArriveAirportUid(flight.getArriveAirportUid());
		flightOutput.setDepartAirportName(flight.getDepartAirportName());
		flightOutput.setDepartAirportUid(flight.getDepartAirportUid());
		flightOutput.setDepartAirportCode(flight.getDepartAirportCode());
		flightOutput.setArriveAirportCode(flight.getArriveAirportCode());
		flightOutput.setPrice(flight.getTotalPrice());
		flightOutput.setOriginPrice(flight.getOriginTotalPrice());
		
		return flightOutput;
	}

	/**
	 * 自定义交通格式转换
	 * @param date
	 * @param departBase
	 * @param arriveBase
	 * @param transportInfo
	 * @return
	 */
	private TransportOutput toCustomTransportOutput(Date date,Base2 departBase,Base2 arriveBase,BaseTransportInfo transportInfo) {
		TransportOutput transportOutput=new TransportOutput();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		List<BaseTransportPath> path=transportInfo.getPathPoint();
		Calendar ca = Calendar.getInstance();
		ca.setTime(date);
		ca.set(Calendar.HOUR_OF_DAY, TourContext.STARTHOUR.intValue());
		transportOutput.setDepart(toLocateDto(departBase));
		transportOutput.setArrive(toLocateDto(arriveBase));
		transportOutput.setStart_time(format.format(date));
		transportOutput.setEnd_time(format.format(date.getTime()+getTransportTimeInMilisecond(path)));
		// there maybe many transport types exist in the transportOutput
		transportOutput.setType(getTransportType(path));
		transportOutput.setAttach(transportInfo);
		return transportOutput;
	}
	
	/**
	 * 获得交通方式类型
	 * @param path
	 * @return
	 */
	private Integer getTransportType(List<BaseTransportPath> path) {
		Long time=0L;
		Long temp=0L;
		Double hour;
		Double minute;
		Integer type=null;
		for(BaseTransportPath ele:path) {
			if(ele.getType()!=2) {
				hour=Double.parseDouble(ele.getTime_clock());
				minute=Double.parseDouble(ele.getTime_minute());
				temp=(long) (minute*60000+hour*3600000);
				if(temp>=time) {
					time=temp;
					type=ele.getTransport_way();
				}
			}
		}
		return type;
	}
	
	private Long getTransportTimeInMilisecond(List<BaseTransportPath> path) {
		Long time=0L;
		Double hour;
		Double minute;
		for(BaseTransportPath ele:path) {
			if(ele.getType()!=2) {
				hour=Double.parseDouble(ele.getTime_clock());
				minute=Double.parseDouble(ele.getTime_minute());
				time=(long) (time+minute*60000+hour*3600000);
			}
		}
		return time;
	}
	
	/**
	 * 
	 * @param flight
	 * @return
	 */
	private List<FlightSegment> generateFlightSegment(Flight flight) {
		//Map<String, Object> routing = (Map<String, Object>) flight.getRouting();
		Map<String, Object> routing = (Map<String,Object>)((Map<String, Object>) flight.getAttach()).get(PlanFlightServiceImpl.FAREFIELD);
		List<FlightSegment> flightSegments = new ArrayList<FlightSegment>();
		FlightSegment flightSegment;
		List<Map<String, Object>> legs = (List<Map<String, Object>>) routing.get(LEG);
		Map<String, Object> leg = legs.get(0);
		Map<String, Object> flight1 = (Map<String, Object>) leg.get(FLIGHT);
		List<Map<String, Object>> segments = (List<Map<String, Object>>) flight1.get(SEGMENTS);
		for (int i = 0; i < segments.size(); i++) {
			flightSegment = new FlightSegment();
			Map<String, Object> segment = segments.get(i);
			flightSegment.setAirline((String) segment.get(AIRLINE));
			flightSegment.setPlane((String) segment.get(PLANE));
			flightSegment.setClassG((String) segment.get(CLASSG));
			flightSegment.setArrive_airport_code((String) segment.get(APORT));
			flightSegment.setDepart_airport_code((String) segment.get(DPORT));
			flightSegment.setDepart_time((String) segment.get(DTIME));
			flightSegment.setArrive_time((String) segment.get(ATIME));
			flightSegments.add(flightSegment);
		}
		return flightSegments;
	}

	/**
	 * base 格式转化
	 * @param base
	 * @return
	 */
	private LocateDto toLocateDto(Base2 base) {
		LocateDto locateDto = new LocateDto();
		locateDto.setPoi_uid(base.getUid());
		locateDto.setUid_url(base.getUid_url());
		locateDto.setDetail(base.getDetail());
		locateDto.setLatitude(base.getLatitude());
		locateDto.setLongitude(base.getLongitude());
		locateDto.setName_cn(base.getName_zh());
		locateDto.setName_cn_url(base.getName_zh_url());
		locateDto.setName_en(base.getName_en());
		return locateDto;
	}

	@Override
	public Boolean hasFlight(BaseCity city1, BaseCity city2) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 生成航班接送机
	 */
	public List<AirportTransferOutput> getAirportTransferes(List<TransportOutput> transports) {
		if (transports == null) {
			return null;
		}
		List<AirportTransferOutput> airportTransferes = new ArrayList<AirportTransferOutput>();
		TransportOutput transport;
		for (int i = 0; i < transports.size(); i++) {
			transport = transports.get(i);
			if (transport.getType() == TransportOutput.FLIGHT) {
				if(i==0){
					airportTransferes.add(getAirportPickup((FlightOutput) transport));
				}else if(i==transports.size()-1){
					airportTransferes.add(getAirportDropoff((FlightOutput) transport));
				}else{
					airportTransferes.add(getAirportPickup((FlightOutput) transport));
					airportTransferes.add(getAirportDropoff((FlightOutput) transport));
				}
			}
		}
		return airportTransferes;
	}

	/**
	 * 生成航班接机服务
	 * @param flight
	 * @return
	 */
	public AirportTransferOutput getAirportPickup(FlightOutput flight) {
		AirportTransferOutput airportTransferOutput = new AirportTransferOutput();
		BaseAirport arriveAirport = baseAirportMapper.getAirportByCode(flight.getArriveAirportCode());
		fillAirportInfo(airportTransferOutput, arriveAirport);
		fillPickupInfo(airportTransferOutput, arriveAirport, flight);
		return airportTransferOutput;
	}

	/**
	 * 生成航班送机服务
	 * @param flight
	 * @return
	 */
	public AirportTransferOutput getAirportDropoff(FlightOutput flight) {
		AirportTransferOutput airportTransferOutput = new AirportTransferOutput();
		BaseAirport departAirport = baseAirportMapper.getAirportByCode(flight.getDepartAirportCode());
		fillAirportInfo(airportTransferOutput, departAirport);
		fillDropoffInfo(airportTransferOutput, departAirport, flight);
		return airportTransferOutput;
	}

	
	public void fillAirportInfo(AirportTransferOutput airportTransferOutput, BaseAirport airport) {
		airportTransferOutput.setPoi_uid(airport.getUid());
		airportTransferOutput.setUid_url(airport.getUid_url());
		airportTransferOutput.setName_cn(airport.getName_zh());
		airportTransferOutput.setName_cn_url(airport.getName_zh_url());
		airportTransferOutput.setLatitude(airport.getLatitude());
		airportTransferOutput.setLongitude(airport.getLongitude());
		airportTransferOutput.setName_en(airport.getName_en());
		airportTransferOutput.setDetail(airport.getDetail());
		airportTransferOutput.setPoi_type(10);
	}

	public AirportTransferOutput fillPickupInfo(AirportTransferOutput airportTransferOutput, BaseAirport airport,
			FlightOutput flightOutput) {
		try {
			airportTransferOutput.setOrder(AirportTransferOutput.PICKUP);
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Base2 base = base2Mapper.getBase(airport.getParent_uid());
			Integer hour = (int) Math.ceil(DistanceUtils.getDistance(airport, base) / 60D);
			logger.debug("distance: " + DistanceUtils.getDistance(airport, base) + ", cost hour: " + hour);
			Date arriveTime = simpleDateFormat.parse(flightOutput.getEnd_time());
			Date startTime = new Date(arriveTime.getTime());
			airportTransferOutput.setStart_time(simpleDateFormat.format(startTime));
			airportTransferOutput.setEnd_time(simpleDateFormat.format(new Date(arriveTime.getTime()+ 3600000 * hour)));

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return airportTransferOutput;
	}

	
	public AirportTransferOutput fillDropoffInfo(AirportTransferOutput airportTransferOutput, BaseAirport airport,
			FlightOutput flightOutput) {
		try {
			airportTransferOutput.setOrder(AirportTransferOutput.DROPOFF);
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Base2 base = base2Mapper.getBase(airport.getParent_uid());
			Integer hour = (int) Math.ceil(DistanceUtils.getDistance(airport, base) / 60D);
			Date departTime;
			departTime = simpleDateFormat.parse(flightOutput.getStart_time());
			Date startTime = new Date(departTime.getTime() - 9000000 -3600000* hour);
			airportTransferOutput.setStart_time(simpleDateFormat.format(startTime));
			airportTransferOutput.setEnd_time(flightOutput.getStart_time());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return airportTransferOutput;
	}

	
	public List<LocateDto> extractDestinations(List<TransportOutput> transports){
		List<LocateDto> locations=new ArrayList<LocateDto>();
		for(int i=0;i<transports.size()-1;i++){
			locations.add(transports.get(i).getArrive());
		}
		return locations;
	}
	
	public SimpleJsonResult optimalDestinationSequence(String param) throws Exception{
		SimpleJsonResult simpleJsonResult = new SimpleJsonResult();
		ItineraryInput input = JsonUtil.toBean(param, ItineraryInput.class);
		ItineraryParameter parameter = planService.convertItineraryParameter(input);
		List<TransportOutput> transports = plan(parameter);
		List<LocateDto> locations=extractDestinations(transports);
		simpleJsonResult.setStatus(SimpleJsonResult.STATUS_OK);
		simpleJsonResult.setData(locations);
		simpleJsonResult.setMessage("adjusted destinations");
		return simpleJsonResult;
	}
	

	/**
	 * 交通规划
	 * 1. 如果输入城市中没有任和一个城市和出发城市或在返回城市存在航班，则返回null，需要重新输入城市
	 * 2. 如果输入城市中有多个城市和出发或返回城市存在航班，则选取和出发，返回城市最近的城市飞。
	 * 3. 如果输入城市中只有一个城市和出发，返回城市存在航班，则从此城市飞进和飞出。
	 * @param date
	 * @param depart
	 * @param arrive
	 * @param days
	 * @param cities
	 * @param adultNum
	 * @param childNum
	 * @return
	 * @throws Exception
	 */
	public List<TransportOutput> plan(Date date, Base2 depart, Base2 arrive, List<Integer> days,
			List<Base2> cities,Integer adultNum,Integer childNum) throws Exception {
		List<Base2> departFlightCities = planFlightService.getFlightBases(depart, cities);
		List<Base2> arriveFlightCities = planFlightService.getFlightBases(arrive, cities);
		logger.debug("departFlightCities size: " + departFlightCities.size() + ", arriveFlightCities size: "
				+ arriveFlightCities.size());
		if (departFlightCities.size() == 0 || arriveFlightCities.size() == 0) {
			// error handle
			logger.error("no city to fly into or out");
			return null;
		} else if (departFlightCities.size() == 1 && arriveFlightCities.size() > 1) {
			if(arriveFlightCities.size()==1&&departFlightCities.get(0) == arriveFlightCities.get(0)){
				return plan(date, depart, arrive, departFlightCities.get(0), days, cities,adultNum,childNum);
			}else{
				Base2 departNearestCity = planFlightService.getNearestBase(depart, departFlightCities);
				arriveFlightCities.remove(departNearestCity);
				Base2 arriveNearestCity = planFlightService.getNearestBase(arrive, arriveFlightCities);
				logger.debug("departNearestCity: " + departNearestCity.getName_zh() + ", arriveNearestCity: "
						+ arriveNearestCity.getName_zh());
				return plan(date, depart, arrive, departNearestCity, arriveNearestCity, days, cities,adultNum,childNum);				
			}
			
		} else {
			Base2 arriveNearestCity = planFlightService.getNearestBase(arrive, arriveFlightCities);
			departFlightCities.remove(arriveNearestCity);
			Base2 departNearestCity = planFlightService.getNearestBase(depart, departFlightCities);
			logger.debug("departNearestCity: " + departNearestCity.getName_zh() + ", arriveNearestCity: "
					+ arriveNearestCity.getName_zh());
			return plan(date, depart, arrive, departNearestCity, arriveNearestCity, days, cities,adultNum,childNum);

		}

		// add logic for add a day for the departNearestCity in the days.
		// List<Date> dates=DateUtils.days2dates(date, days);
	}

	/**
	 * 输入城市中只有一个城市和出发，返回城市有航班情况.
	 * @param date
	 * @param departCity
	 * @param arriveCity
	 * @param inOutCity
	 * @param days
	 * @param cities
	 * @return
	 * @throws Exception 
	 */

	public List<TransportOutput> plan(Date date, Base2 departCity, Base2 arriveCity, Base2 inOutCity,
			List<Integer> days, List<Base2> cities,Integer adultNum,Integer childNum) throws Exception {
		Map<Base2, Integer> cityDayMap = ListUtils.map(cities, days);
		LocationPlanner planner = new LocationPlanner();
		Planned planAlg = new TspLocationPlanner();
		planner.setPlanner(planAlg);
		int[] plannedCityOrder = planner.performPlan(cities, inOutCity);
		int index = ListUtils.getIndex(cities, inOutCity);
		int startIndex = ArrayUtil.getIndex(plannedCityOrder, index);
		// List<City> plannedCities=ListUtils.reorder(cities, plannedCityOrder);
		List<Base2> plannedCities = ListUtils.reorder(cities, plannedCityOrder, startIndex);
		//planFlightService.updateDaysCities(cityDayMap, days, plannedCities, inOutCity);
		List<Integer> newDays=planFlightService.getNewDays(cityDayMap, days, plannedCities, inOutCity);
		List<Base2> newPlannedCities=planFlightService.getNewCities(departCity,arriveCity, inOutCity,plannedCities);
		// add logic for add a day for the departNearestCity in the days.
		List<Date> dates = DateUtils.days2dates2(date, newDays);
		List<Base2> flightCities=new ArrayList<Base2>();

		logger.debug("new dates: "+JsonUtil.Obj2Json(dates));
		logger.debug("new planned cities: "+JsonUtil.Obj2Json(newPlannedCities));
		return plan(dates, newPlannedCities,adultNum,childNum);
	}



	/**
	 * 输入城市中有多个城市和出发，返回城市有航班情况.
	 * @throws Exception 
	 */
	public List<TransportOutput> plan(Date date, Base2 departCity, Base2 arriveCity, Base2 firstCity, Base2 lastCity,
			List<Integer> days, List<Base2> cities,Integer adultNum,Integer childNum) throws Exception {
		// did not use tsp alg now tor tests;
		List<Base2> flightCities=new ArrayList<Base2>();
		//List<Integer> flightDays=new ArrayList<Integer>();
		  Map<Base2,Integer> cityDayMap=ListUtils.map(cities, days);
		  LocationPlanner planner=new LocationPlanner();
		  Planned planAlg=new TspLocationPlanner(); 
		  planner.setPlanner(planAlg); 
		  int[] plannedCityOrder= planner.performPlan(cities, firstCity, lastCity);
		  logger.debug("plannedCityOrder: "+JsonUtil.Obj2Json(plannedCityOrder)+" changed cities"+JsonUtil.Obj2Json(cities));
		  List<Base2> plannedCities=ListUtils.reorder(cities,plannedCityOrder);
		  // hack for now
		  //cities=plannedCities;
		  //logger.debug("plannedCities: "+JsonUtil.Obj2Json(plannedCities));
		  //logger.debug("ities: "+JsonUtil.Obj2Json(cities));
		  //List<Integer> plannedDays=ListUtils.reorder(days,plannedCityOrder);
		  List<Integer> plannedDays=MapUtil.getValues(cityDayMap,plannedCities);
		  List<Date> dates=DateUtils.days2dates2(date, plannedDays);
		  flightCities.add(departCity);
		  flightCities.addAll(plannedCities);
		  flightCities.add(arriveCity);
		  logger.debug("flightCityes: " + JsonUtil.Obj2Json(flightCities));
/*		List<Date> dates = DateUtils.days2dates(date, days);
		List<Base2> flightCities = new ArrayList<Base2>();
		flightCities.add(departCity);
		flightCities.addAll(cities);
		flightCities.add(arriveCity);
		logger.debug("flightCityes: " + JsonUtil.Obj2Json(flightCities));*/
		return plan(dates, flightCities,adultNum,childNum);
	}
	
	@Resource
	private PlanService planService;
	@Resource
	private PlanFlightService planFlightService;

	@Resource
	private BaseAirportMapper baseAirportMapper;
	@Resource
	private Base2Mapper base2Mapper;
	@Resource
	private BaseTransportMapper baseTransportMapper;
	@Resource
	private BaseTransportWayMapper baseTransportWayMapper;
	
	public static final String LEG="leg";
	public static final String SEGMENTS="segments";
	public static final String FLIGHT="flight";
	public static final String AIRLINE="airline";
	public static final String PLANE="plane";
	public static final String CLASSG="classG";
	public static final String DPORT="dport";
	public static final String APORT="aport";
	public static final String DTIME="dtime";
	public static final String ATIME="atime";
	private Logger logger = Logger.getLogger(PlanTransportServiceImpl.class);
}
