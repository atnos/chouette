package mobi.chouette.exchange.kml.exporter;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.ChouetteIdGenerator;
import mobi.chouette.exchange.ChouetteIdGeneratorFactory;
import mobi.chouette.model.AccessLink;
import mobi.chouette.model.AccessPoint;
import mobi.chouette.model.ConnectionLink;
import mobi.chouette.model.NeptuneLocalizedObject;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;

import org.apache.commons.collections.map.ListOrderedMap;

import com.vividsolutions.jts.geom.Coordinate;

public class KmlData implements Constant{
	@Getter
	private String name;
	@Getter
	private ListOrderedMap extraData = new ListOrderedMap();
	@Getter
	private Map<String,KmlItem> items = new HashMap<>();
	
	public KmlData(String name)
	{
		this.name = name;
	}
	
	public void addExtraData(String key, Object value)
	{
		extraData.put(key,valueOf(value));
	}

	public KmlItem addNewItem(String id)
	{
		if (items.containsKey(id)) return null;
		KmlItem item = new KmlItem();
		item.setId(id);
		items.put(id,item);
		return item;
	}

	public class KmlItem {
		
		@Getter
		@Setter
		private String id;
		
		@Getter
		private ListOrderedMap attributes = new ListOrderedMap();
		@Getter
		private ListOrderedMap extraData = new ListOrderedMap();
		@Getter
		private List<KmlPoint> lineString;
		@Getter
		private List<List<KmlPoint>> multiLineString;
		@Getter
		private KmlPoint point;
		
		public void addAttribute(String key, Object value)
		{
			attributes.put(key,valueOf(value));
		}
		public void addExtraData(String key, Object value)
		{
			extraData.put(key,valueOf(value));
		}
		
		public void setPoint(NeptuneLocalizedObject object)
		{
			point = new KmlPoint(object);
		}
		
		public void addPoint(NeptuneLocalizedObject object)
		{
			if (lineString == null) lineString = new ArrayList<>();
			lineString.add(new KmlPoint(object));
		}
		public void addLineString(NeptuneLocalizedObject... objects)
		{
			if (multiLineString == null) multiLineString = new ArrayList<>();
			List<KmlPoint> ls = new ArrayList<>();
			multiLineString.add(ls);
			for (NeptuneLocalizedObject object : objects) {
				ls.add(new KmlPoint(object));
			}
				
		}
		public void addLineString(com.vividsolutions.jts.geom.LineString geometry)
		{
			if (multiLineString == null) multiLineString = new ArrayList<>();
			List<KmlPoint> ls = new ArrayList<>();
			multiLineString.add(ls);
			Coordinate[] coordinates = geometry.getCoordinates();
			for (Coordinate object : coordinates) {
				ls.add(new KmlPoint(object.x,object.y));
			}
				
		}
	}

	public class KmlPoint {
		@Getter
		public double latitude;
		@Getter
		public double longitude;
		
		public KmlPoint(NeptuneLocalizedObject object)
		{
			this.latitude = object.getLatitude().doubleValue(); 
			this.longitude = object.getLongitude().doubleValue(); 
		}
		
		public KmlPoint(BigDecimal latitude, BigDecimal longitude)
		{
			this.latitude = latitude.doubleValue(); 
			this.longitude = longitude.doubleValue(); 
		}

		public KmlPoint(double x, double y) {
			this.latitude = y; 
			this.longitude = x; 
		}
	}
	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss Z");
	private static final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss");
	private static String valueOf(Object data)
	{
		if (data == null) return "";
		if (data instanceof java.sql.Time)
		{
			return timeFormat.format(data);
		}
		if (data instanceof java.util.Date)
		{
			return dateFormat.format(data);
		}
		
		return data.toString();
	}

	public KmlItem addStopPoint(Context context, StopPoint point) throws ClassNotFoundException, IOException {
		KmlExportParameters parameters = (KmlExportParameters) context.get(PARAMETERS_FILE);
		ChouetteIdGenerator chouetteIdGenerator = (ChouetteIdGenerator) context.put(CHOUETTEID_GENERATOR, ChouetteIdGeneratorFactory.create(parameters.getDefaultFormat()));
		
		KmlItem item = addNewItem(chouetteIdGenerator.toSpecificFormatId(point.getChouetteId(), parameters.getDefaultCodespace(), point));
		if (item == null) return null;
		// item.setId(area.getChouetteId().getObjectId());
		StopArea area = point.getContainedInStopArea();
		if (area != null)
		{
		item.addAttribute("name", area.getName());
		item.addExtraData("objectid", chouetteIdGenerator.toSpecificFormatId(area.getChouetteId(), parameters.getDefaultCodespace(), area));
		item.addExtraData("object_version", area.getObjectVersion());
		item.addExtraData("creation_time", area.getCreationTime());
		item.addExtraData("creator_id", area.getCreatorId());
		item.addExtraData("name", area.getName());
		item.addExtraData("comment", area.getComment());
		item.addExtraData("area_type", area.getAreaType());
		item.addExtraData("registration_number", area.getRegistrationNumber());
		item.addExtraData("nearest_topic_name", area.getNearestTopicName());
		item.addExtraData("fare_code", area.getFareCode());
		item.addExtraData("longitude", area.getLongitude());
		item.addExtraData("latitude", area.getLatitude());
		item.addExtraData("long_lat_type", area.getLongLatType());
		item.addExtraData("country_code", area.getCountryCode());
		item.addExtraData("street_name", area.getStreetName());
		item.addExtraData("mobility_restricted_suitability", area.getMobilityRestrictedSuitable());
		item.addExtraData("stairs_availability", area.getStairsAvailable());
		item.addExtraData("lift_availability", area.getLiftAvailable());
		item.addExtraData("int_user_needs", area.getIntUserNeeds());
		if (area.getParent() != null)
		   item.addExtraData("parent_objectid", chouetteIdGenerator.toSpecificFormatId(area.getParent().getChouetteId(), parameters.getDefaultCodespace(), area.getParent()));
		item.setPoint(area);
		}
		return item;
	}

	public KmlItem addStopArea(Context context, StopArea area) throws ClassNotFoundException, IOException {
		KmlExportParameters parameters = (KmlExportParameters) context.get(PARAMETERS_FILE);
		ChouetteIdGenerator chouetteIdGenerator = (ChouetteIdGenerator) context.put(CHOUETTEID_GENERATOR, ChouetteIdGeneratorFactory.create(parameters.getDefaultFormat()));
		
		KmlItem item = addNewItem(chouetteIdGenerator.toSpecificFormatId(area.getChouetteId(), parameters.getDefaultCodespace(), area));
		if (item == null) return null;
		// item.setId(area.getChouetteId().getObjectId());
		item.addAttribute("name", area.getName());
		item.addExtraData("objectid", chouetteIdGenerator.toSpecificFormatId(area.getChouetteId(), parameters.getDefaultCodespace(), area));
		item.addExtraData("object_version", area.getObjectVersion());
		item.addExtraData("creation_time", area.getCreationTime());
		item.addExtraData("creator_id", area.getCreatorId());
		item.addExtraData("name", area.getName());
		item.addExtraData("area_type", area.getAreaType());
		item.addExtraData("registration_number", area.getRegistrationNumber());
		item.addExtraData("nearest_topic_name", area.getNearestTopicName());
		item.addExtraData("fare_code", area.getFareCode());
		item.addExtraData("country_code", area.getCountryCode());
		item.addExtraData("street_name", area.getStreetName());
		item.addExtraData("mobility_restricted_suitability", area.getMobilityRestrictedSuitable());
		item.addExtraData("stairs_availability", area.getStairsAvailable());
		item.addExtraData("lift_availability", area.getLiftAvailable());
		if (area.getParent() != null)
		   item.addExtraData("parent", chouetteIdGenerator.toSpecificFormatId(area.getParent().getChouetteId(), parameters.getDefaultCodespace(), area.getParent()));
		item.setPoint(area);
		return item;
	}
	
	public KmlItem addConnectionLink(Context context, ConnectionLink link) throws ClassNotFoundException, IOException {
		KmlExportParameters parameters = (KmlExportParameters) context.get(PARAMETERS_FILE);
		ChouetteIdGenerator chouetteIdGenerator = (ChouetteIdGenerator) context.put(CHOUETTEID_GENERATOR, ChouetteIdGeneratorFactory.create(parameters.getDefaultFormat()));
		
		KmlItem item = addNewItem(chouetteIdGenerator.toSpecificFormatId(link.getChouetteId(), parameters.getDefaultCodespace(), link));
		if (item == null) return null;
		// item.setId(link.getChouetteId().getObjectId());
		item.addAttribute("name", link.getName());
		item.addExtraData("objectid", chouetteIdGenerator.toSpecificFormatId(link.getChouetteId(), parameters.getDefaultCodespace(), link));
		item.addExtraData("object_version", link.getObjectVersion());
		item.addExtraData("creation_time", link.getCreationTime());
		item.addExtraData("creator_id", link.getCreatorId());
		item.addExtraData("name", link.getName());
		item.addExtraData("link_distance", link.getLinkDistance());
		item.addExtraData("link_type", link.getLinkType());
		item.addExtraData("default_duration", link.getDefaultDuration());
		item.addExtraData("frequent_traveller_duration", link.getFrequentTravellerDuration());
		item.addExtraData("occasional_traveller_duration", link.getOccasionalTravellerDuration());
		item.addExtraData("mobility_restricted_traveller_duration", link.getMobilityRestrictedTravellerDuration());
		item.addExtraData("mobility_restricted_suitability", link.getMobilityRestrictedSuitable());
		item.addExtraData("stairs_availability", link.getStairsAvailable());
		item.addExtraData("lift_availability", link.getLiftAvailable());
		if (link.getStartOfLink() != null && link.getEndOfLink() != null)
		{
		   item.addExtraData("departure_objectid", chouetteIdGenerator.toSpecificFormatId(link.getStartOfLink().getChouetteId(), parameters.getDefaultCodespace(), link.getStartOfLink()));
		   item.addPoint(link.getStartOfLink());
		   item.addExtraData("arrival_objectid", chouetteIdGenerator.toSpecificFormatId(link.getEndOfLink().getChouetteId(), parameters.getDefaultCodespace(), link.getEndOfLink()));
		   item.addPoint(link.getEndOfLink());
		}
		return item;
	}

	public KmlItem addAccessPoint(Context context, AccessPoint point) throws ClassNotFoundException, IOException {
		KmlExportParameters parameters = (KmlExportParameters) context.get(PARAMETERS_FILE);
		ChouetteIdGenerator chouetteIdGenerator = (ChouetteIdGenerator) context.put(CHOUETTEID_GENERATOR, ChouetteIdGeneratorFactory.create(parameters.getDefaultFormat()));
		
		KmlItem item = addNewItem(chouetteIdGenerator.toSpecificFormatId(point.getChouetteId(), parameters.getDefaultCodespace(), point));
		if (item == null) return null;
		// item.setId(point.getChouetteId().getObjectId());
		item.addAttribute("name", point.getName());
		item.addExtraData("objectid", chouetteIdGenerator.toSpecificFormatId(point.getChouetteId(), parameters.getDefaultCodespace(), point));
		item.addExtraData("object_version", point.getObjectVersion());
		item.addExtraData("creation_time", point.getCreationTime());
		item.addExtraData("creator_id", point.getCreatorId());
		item.addExtraData("name", point.getName());
		item.addExtraData("country_code", point.getCountryCode());
		item.addExtraData("street_name", point.getStreetName());
		item.addExtraData("openning_time", point.getOpeningTime());
		item.addExtraData("closing_time", point.getClosingTime());
		item.addExtraData("access_type", point.getType());
		item.addExtraData("mobility_restricted_suitability", point.getMobilityRestrictedSuitable());
		item.addExtraData("stairs_availability", point.getStairsAvailable());
		item.addExtraData("lift_availability", point.getLiftAvailable());
		item.addExtraData("stop_area_objectid", chouetteIdGenerator.toSpecificFormatId(point.getContainedIn().getChouetteId(), parameters.getDefaultCodespace(), point.getContainedIn()));
		item.setPoint(point);
		return item;
	}

	public KmlItem addAccessLink(Context context, AccessLink link) throws ClassNotFoundException, IOException {
		KmlExportParameters parameters = (KmlExportParameters) context.get(PARAMETERS_FILE);
		ChouetteIdGenerator chouetteIdGenerator = (ChouetteIdGenerator) context.put(CHOUETTEID_GENERATOR, ChouetteIdGeneratorFactory.create(parameters.getDefaultFormat()));
		
		KmlItem item = addNewItem(chouetteIdGenerator.toSpecificFormatId(link.getChouetteId(), parameters.getDefaultCodespace(), link));
		if (item == null) return null;
		// item.setId(link.getChouetteId().getObjectId());
		item.addAttribute("name", link.getName());
		item.addExtraData("access_link_type", link.getLinkType());
		item.addExtraData("objectid", chouetteIdGenerator.toSpecificFormatId(link.getChouetteId(), parameters.getDefaultCodespace(), link));
		item.addExtraData("object_version", link.getObjectVersion());
		item.addExtraData("creation_time", link.getCreationTime());
		item.addExtraData("creator_id", link.getCreatorId());
		item.addExtraData("name", link.getName());
		item.addExtraData("comment", link.getComment());
		item.addExtraData("link_distance", link.getLinkDistance());
		item.addExtraData("link_type", link.getLinkType());
		item.addExtraData("default_duration", link.getDefaultDuration());
		item.addExtraData("frequent_traveller_duration", link.getFrequentTravellerDuration());
		item.addExtraData("occasional_traveller_duration", link.getOccasionalTravellerDuration());
		item.addExtraData("mobility_restricted_traveller_duration", link.getMobilityRestrictedTravellerDuration());
		item.addExtraData("mobility_restricted_suitability", link.getMobilityRestrictedSuitable());
		item.addExtraData("stairs_availability", link.getStairsAvailable());
		item.addExtraData("lift_availability", link.getLiftAvailable());
		item.addExtraData("int_user_needs", link.getIntUserNeeds());
		item.addExtraData("link_orientation", link.getLinkOrientation());
		if (link.getAccessPoint() != null && link.getStopArea() != null)
		{
		   item.addExtraData("access_point_objectid", chouetteIdGenerator.toSpecificFormatId(link.getAccessPoint().getChouetteId(), parameters.getDefaultCodespace(), link.getAccessPoint()));
		   item.addPoint(link.getAccessPoint());
		   item.addExtraData("stop_area_objectid", chouetteIdGenerator.toSpecificFormatId(link.getStopArea().getChouetteId(), parameters.getDefaultCodespace(), link.getStopArea()));
		   item.addPoint(link.getStopArea());
		}
		return item;
	}


}
