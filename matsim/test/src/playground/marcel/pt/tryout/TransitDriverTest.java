/* *********************************************************************** *
 * project: org.matsim.*
 * BusDriverTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.marcel.pt.tryout;

import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.Events;
import org.matsim.core.facilities.FacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.world.World;
import org.xml.sax.SAXException;

import playground.marcel.pt.integration.ExperimentalTransitRouteFactory;
import playground.marcel.pt.integration.TransitDriver;
import playground.marcel.pt.integration.TransitQueueSimulation;
import playground.marcel.pt.integration.TransitQueueVehicle;
import playground.marcel.pt.interfaces.TransitVehicle;
import playground.marcel.pt.mocks.MockAgent;
import playground.marcel.pt.transitSchedule.Departure;
import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitRoute;
import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderTest;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderV1;

public class TransitDriverTest extends MatsimTestCase {

	private static final String INPUT_TEST_FILE_TRANSITSCHEDULE = "transitSchedule.xml";
	private static final String INPUT_TEST_FILE_NETWORK = "network.xml";
	private static final String INPUT_TEST_FILE_FACILITIES = "facilities.xml";

	public void testPersonsLeavingBus() throws SAXException, ParserConfigurationException, IOException {
		loadConfig(null);
		final String inputDir = "test/input/" + TransitScheduleReaderTest.class.getPackage().getName().replace('.', '/') + "/";

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(inputDir + INPUT_TEST_FILE_NETWORK);
		FacilitiesImpl facilities = new FacilitiesImpl();
		new MatsimFacilitiesReader(facilities).readFile(inputDir + INPUT_TEST_FILE_FACILITIES);

		World world = new World();
		world.setFacilityLayer(facilities);
		world.setNetworkLayer(network);
		world.complete();

		TransitSchedule schedule = new TransitSchedule();
		new TransitScheduleReaderV1(schedule, network, facilities).readFile(inputDir + INPUT_TEST_FILE_TRANSITSCHEDULE);

		TransitLine lineT1 = schedule.getTransitLines().get(new IdImpl("T1"));
//		CreateTimetableForStop timetable = new CreateTimetableForStop(lineT1);
		assertNotNull("could not get transit line.", lineT1);

		TransitRoute route1 = lineT1.getRoutes().get(new IdImpl("1"));
		Map<Id, Departure> departures = route1.getDepartures();

		Events events = new Events();
		TransitQueueSimulation sim = new TransitQueueSimulation(network, new PopulationImpl(), events, facilities);

		TransitDriver driver = new TransitDriver(lineT1, route1, departures.values().iterator().next(), sim);
		TransitVehicle bus = new TransitQueueVehicle(20, events);
		driver.setVehicle(bus);

		Facility home = facilities.getFacilities().get(new IdImpl("home"));
		Facility stop2 = facilities.getFacilities().get(new IdImpl("stop2"));
		Facility stop3 = facilities.getFacilities().get(new IdImpl("stop3"));
		Facility stop4 = facilities.getFacilities().get(new IdImpl("stop4"));
		Facility stop6 = facilities.getFacilities().get(new IdImpl("stop6"));

		MockAgent agent1 = new MockAgent(home, stop2);
		MockAgent agent2 = new MockAgent(home, stop3);
		MockAgent agent3 = new MockAgent(home, stop4);
		MockAgent agent4 = new MockAgent(home, stop3);
		MockAgent agent5 = new MockAgent(home, stop6);
		bus.addPassenger(agent1);
		bus.addPassenger(agent2);
		bus.addPassenger(agent3);
		bus.addPassenger(agent4);
		bus.addPassenger(agent5);
		
		assertEquals("wrong number of passengers.", 5, bus.getPassengers().size());

		Link link = driver.chooseNextLink();
		driver.moveOverNode();
		while (link != null) {
			Link nextLink = driver.chooseNextLink();
			if (nextLink != null) {
				assertEquals("current link and next link must have common node.", link.getToNode(), nextLink.getFromNode());
			}
			link = nextLink;
			if (link != null) {
				driver.moveOverNode();
			}
		}

		assertEquals("wrong number of passengers.", 0, bus.getPassengers().size());
	}

	public void testPersonsEnteringBus() throws SAXException, ParserConfigurationException, IOException { // TODO [MR] disabled test
		loadConfig(null);
		final String inputDir = "test/input/" + TransitScheduleReaderTest.class.getPackage().getName().replace('.', '/') + "/";

		NetworkLayer network = new NetworkLayer();
		network.getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());
		new MatsimNetworkReader(network).readFile(inputDir + INPUT_TEST_FILE_NETWORK);
		FacilitiesImpl facilities = new FacilitiesImpl();
		new MatsimFacilitiesReader(facilities).readFile(inputDir + INPUT_TEST_FILE_FACILITIES);

		World world = new World();
		world.setFacilityLayer(facilities);
		world.setNetworkLayer(network);
		world.complete();

		TransitSchedule schedule = new TransitSchedule();
		new TransitScheduleReaderV1(schedule, network, facilities).readFile(inputDir + INPUT_TEST_FILE_TRANSITSCHEDULE);

		TransitLine lineT1 = schedule.getTransitLines().get(new IdImpl("T1"));
//		CreateTimetableForStop timetable = new CreateTimetableForStop(lineT1);
		assertNotNull("could not get transit line.", lineT1);

		TransitRoute route1 = lineT1.getRoutes().get(new IdImpl("1"));
		Map<Id, Departure> departures = route1.getDepartures();

		Events events = new Events();
		TransitQueueSimulation sim = new TransitQueueSimulation(network, new PopulationImpl(), events, facilities);
		
		TransitDriver driver = new TransitDriver(lineT1, route1, departures.values().iterator().next(), sim);
		TransitVehicle bus = new TransitQueueVehicle(20, events);
		driver.setVehicle(bus);

		Facility work = facilities.getFacilities().get(new IdImpl("work"));
		Facility stop2 = facilities.getFacilities().get(new IdImpl("stop2"));
		Facility stop3 = facilities.getFacilities().get(new IdImpl("stop3"));
		Facility stop4 = facilities.getFacilities().get(new IdImpl("stop4"));
		Facility stop6 = facilities.getFacilities().get(new IdImpl("stop6"));

		MockAgent agent1 = new MockAgent(stop2, work);
		MockAgent agent2 = new MockAgent(stop3, work);
		MockAgent agent3 = new MockAgent(stop4, work);
		MockAgent agent4 = new MockAgent(stop3, work);
		MockAgent agent5 = new MockAgent(stop6, work);
		sim.agentDeparts(agent1, stop2.getLink());
		sim.agentDeparts(agent2, stop3.getLink());
		sim.agentDeparts(agent3, stop4.getLink());
		sim.agentDeparts(agent4, stop3.getLink());
		sim.agentDeparts(agent5, stop6.getLink());
		
		assertEquals("wrong number of passengers.", 0, bus.getPassengers().size());

		Link link = driver.chooseNextLink();
		driver.moveOverNode();
		while (link != null) {
			Link nextLink = driver.chooseNextLink();
			if (nextLink != null) {
				assertEquals("current link and next link must have common node.", link.getToNode(), nextLink.getFromNode());
			}
			link = nextLink;
			if (link != null) {
				driver.moveOverNode();
			}
		}

		assertEquals("wrong number of passengers.", 5, bus.getPassengers().size());
	}

}
