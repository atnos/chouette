
package mobi.chouette.exchange.report;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import lombok.Data;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder={"lineCount",
		"routeCount",
		"connectionLinkCount",
		"timeTableCount",
		"stopAreaCount",
		"accessPointCount",
		"vehicleJourneyCount",
		"journeyPatternCount"})
@Data
public class DataStats {

	@XmlElement(name = "line_count")
	private int lineCount = 0;

	@XmlElement(name = "route_count")
	private int routeCount = 0;

	@XmlElement(name = "connection_link_count")
	private int connectionLinkCount = 0;

	@XmlElement(name = "time_table_count")
	private int timeTableCount = 0;

	@XmlElement(name = "stop_area_count")
	private int stopAreaCount = 0;

	@XmlElement(name = "access_point_count")
	private int accessPointCount = 0;

	@XmlElement(name = "vehicle_journey_count")
	private int vehicleJourneyCount = 0;

	@XmlElement(name = "journey_pattern_count")
	private int journeyPatternCount = 0;

}