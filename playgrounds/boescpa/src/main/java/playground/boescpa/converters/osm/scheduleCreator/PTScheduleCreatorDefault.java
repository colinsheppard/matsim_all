/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.osm.scheduleCreator;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * The default implementation of PTStationCreator (using the Swiss-HAFAS-Schedule).
 *
 * @author boescpa
 */
public class PTScheduleCreatorDefault extends PTScheduleCreator {

	private Map<String, Integer> vehiclesUndefined = new HashMap<>();
	private CoordinateTransformation transformWGS84toCH1903_LV03 = TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03");
	private final int initialDelay = 60; // [s] In MATSim a pt route starts with the arrival at the first station. In HAFAS with the departure at the first station. Ergo we have to set a delay which gives some waiting time at the first station while still keeping the schedule.

	public PTScheduleCreatorDefault(TransitSchedule schedule, Vehicles vehicles) {
		super(schedule, vehicles);
	}

	@Override
	public final void createSchedule(String osmFile, String hafasFolder, Network network, String vehicleFile) {
		log.info("Creating the schedule...");

		{ // Create PTLines:
			log.info("Creating pt lines from HAFAS file...");
			// 1. Read all vehicles from vehicleFile:
			readVehicles(vehicleFile);
			// 2. Read all stops from HAFAS-BFKOORD_GEO
			readStops(hafasFolder + "/BFKOORD_GEO");
			// 3. Create all lines from HAFAS-Schedule
			readLines(hafasFolder + "/FPLAN");
			// 4. Print undefined vehicles
			printVehiclesUndefined();
			log.info("Creating pt lines from HAFAS file... done.");
		}

		{ // Complement the PTStations:
			log.info("Correcting pt station coordinates based on OSM...");

			// TODO-boescpa Implement complementPTStations...
			// Check and correct pt-Station-coordinates with osm-knowledge.
			// work with this.schedule...

			log.info("Correcting pt station coordinates based on OSM... done.");
		}

		log.info("Creating the schedule... done.");
	}

	////////////////// Local Helpers /////////////////////

	/**
	 * Reads all the vehicle types from the file specified.
	 *
	 * @param vehicleFile
	 */
	protected void readVehicles(String vehicleFile) {
		try {
			FileReader reader = new FileReader(vehicleFile);
			BufferedReader readsLines = new BufferedReader(reader);
			// read header 1 and 2
			readsLines.readLine();
			readsLines.readLine();
			// start the actual readout:
			String newLine = readsLines.readLine();
			while (newLine != null) {
				String[] newType = newLine.split(";");
				// The first line without a key breaks the readout.
				if (newType.length == 0) {
					break;
				}
				// Create the vehicle:
				Id<VehicleType> typeId = Id.create(newType[0].trim(), VehicleType.class);
				VehicleType vehicleType = vehicleBuilder.createVehicleType(typeId);
				vehicleType.setLength(Double.parseDouble(newType[1]));
				vehicleType.setWidth(Double.parseDouble(newType[2]));
				vehicleType.setAccessTime(Double.parseDouble(newType[3]));
				vehicleType.setEgressTime(Double.parseDouble(newType[4]));
				if (newType[5].matches("serial")) {
					vehicleType.setDoorOperationMode(VehicleType.DoorOperationMode.serial);
				} else if (newType[5].matches("parallel")) {
					vehicleType.setDoorOperationMode(VehicleType.DoorOperationMode.parallel);
				}
				vehicleType.setPcuEquivalents(Double.parseDouble(newType[6]));
				vehicles.addVehicleType(vehicleType);
				// Read the next line:
				newLine = readsLines.readLine();
			}
			readsLines.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void readStops(String BFKOORD_GEOFile) {
		try {
			FileReader reader = new FileReader(BFKOORD_GEOFile);
			BufferedReader readsLines = new BufferedReader(reader);
			String newLine = readsLines.readLine();
			while (newLine != null) {
				/*Spalte Typ Bedeutung
				1−7 INT32 Nummer der Haltestelle
				9−18 FLOAT X-Koordinate
				20−29 FLOAT Y-Koordinate
				31−36 INT16 Z-Koordinate (optional)
				38ff CHAR Kommentarzeichen "%"gefolgt vom Klartext des Haltestellennamens (optional zur besseren Lesbarkeit)*/
				Id<TransitStopFacility> stopId = Id.create(newLine.substring(0, 7), TransitStopFacility.class);
				double xCoord = Double.parseDouble(newLine.substring(8, 18));
				double yCoord = Double.parseDouble(newLine.substring(19, 29));
				Coord coord = this.transformWGS84toCH1903_LV03.transform(new CoordImpl(xCoord, yCoord));
				String stopName = newLine.substring(39, newLine.length());
				createStop(stopId, coord, stopName);
				newLine = readsLines.readLine();
			}
			readsLines.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createStop(Id<TransitStopFacility> stopId, Coord coord, String stopName) {
		TransitStopFacility stopFacility = this.scheduleBuilder.createTransitStopFacility(stopId, coord, false);
		stopFacility.setName(stopName);
		this.schedule.addStopFacility(stopFacility);
		//log.info("Added " + schedule.getFacilities().get(stopId).toString());
	}

	protected void readLines(String FPLAN) {
		try {
			Map<Id<TransitLine>,PtLineFPLAN> linesFPLAN = new HashMap<>();
			PtRouteFPLAN currentRouteFPLAN = null;

			FileReader reader = new FileReader(FPLAN);
			BufferedReader readsLines = new BufferedReader(reader);
			String newLine = readsLines.readLine();
			while (newLine != null) {
				if (newLine.charAt(0) == '*') {
					if (newLine.charAt(1) == 'Z') {
						// Initialzeile neue Fahrt
						/*Spalte Typ Bedeutung
						1−2 CHAR *Z
						4−8 INT32 Fahrtnummer
						10−15 CHAR Verwaltung (6-stellig); Die Verwaltungsangabe darf
						keine Leerzeichen enthalten.
						17−21 INT16 leer // Tatsächlich unterscheidet dieser Eintrag noch verschiedene Fahrtvarianten...
						23−25 INT16 Taktanzahl; gibt die Anzahl der noch folgenden Takte
						an.
						27−29 INT16 Taktzeit in Minuten (Abstand zwischen zwei Fahrten).*/
						// Get the appropriate transit line...
						Id<TransitLine> lineId = Id.create(newLine.substring(9, 15).trim(), TransitLine.class);
						PtLineFPLAN lineFPLAN;
						if (linesFPLAN.containsKey(lineId)) {
							lineFPLAN = linesFPLAN.get(lineId);
						} else {
							lineFPLAN = new PtLineFPLAN(lineId.toString());
							linesFPLAN.put(lineId, lineFPLAN);
						}
						// Create the new route in this line...
						int routeNr = 0;
						Id<TransitRoute> routeId = Id.create(newLine.substring(3, 8).trim() + "_" + String.format("%03d", routeNr), TransitRoute.class);
						while (lineFPLAN.idRoutesFPLAN.contains(routeId)) {
							routeNr++;
							routeId = Id.create(newLine.substring(3, 8).trim() + "_" + String.format("%03d", routeNr), TransitRoute.class);
						}
						int numberOfDepartures = 0;
						int cycleTime = 0;
						try {
							numberOfDepartures = Integer.parseInt(newLine.substring(22, 25));
							cycleTime = Integer.parseInt(newLine.substring(26, 29));
						} catch (Exception e) {
						}
						currentRouteFPLAN = new PtRouteFPLAN(lineId, routeId, numberOfDepartures, cycleTime);
						lineFPLAN.addPtRouteFPLAN(currentRouteFPLAN);
					} else if (newLine.charAt(1) == 'T') {
						// Initialzeile neue freie Fahrt (Linien welche nicht nach Taktfahrplan fahren...)
						log.error("*T-Line in HAFAS discovered. Please implement appropriate read out.");
					} else if (newLine.charAt(1) == 'G') {
						// Verkehrsmittelzeile
						/*Spalte Typ Bedeutung
						1−2 CHAR *G
						4−6 CHAR Verkehrsmittel bzw. Gattung
						8−14 [#]INT32 (optional) Laufwegsindex oder Haltestellennummer,
							ab der die Gattung gilt.
						16−22 [#]INT32 (optional) Laufwegsindex oder Haltestellennummer,
							bis zu der die Gattung gilt.
						24−29 [#]INT32 (optional) Index für das x. Auftreten oder
						Abfahrtszeitpunkt // 26-27 hour, 28-29 minute
						31−36 [#]INT32 (optional) Index für das x. Auftreten oder
						Ankunftszeitpunkt*/
						if (currentRouteFPLAN != null) {
							// Vehicle Id:
							currentRouteFPLAN.setUsedVehicle(newLine.substring(3, 6));
							// First Departure:
							int hourFirstDeparture = Integer.parseInt(newLine.substring(25, 27));
							int minuteFirstDeparture = Integer.parseInt(newLine.substring(27, 29));
							currentRouteFPLAN.setFirstDepartureTime(hourFirstDeparture, minuteFirstDeparture);
						} else {
							log.error("*G-Line before appropriate *Z-Line.");
						}
					}
				} else if (newLine.charAt(0) == '+') { // Regionszeile (Bedarfsfahrten)
					// We don't have this transport mode in  MATSim (yet). => Delete Route and if Line now empty, delete Line.
					log.error("+-Line in HAFAS discovered. Please implement appropriate read out.");
				} else { // Laufwegzeile
					/*Spalte Typ Bedeutung
					1−7 INT32 Haltestellennummer
					9−29 CHAR (optional zur Lesbarkeit) Haltestellenname
					30−35 INT32 Ankunftszeit an der Haltestelle (lt. Ortszeit der
							Haltestelle) // 32-33 hour, 34-35 minute
					37−42 INT32 Abfahrtszeit an Haltestelle (lt. Ortszeit der
					Haltestelle) // 39-40 hour, 41-42 minute
					44−48 INT32 Ab dem Halt gültige Fahrtnummer (optional)
							50−55 CHAR Ab dem Halt gültige Verwaltung (optional)
							57−57 CHAR (optional) "X", falls diese Haltestelle auf dem
					Laufschild der Fahrt aufgeführt wird.*/
					if (currentRouteFPLAN != null) {
						double arrivalTime = 0;
						try {
							arrivalTime = Double.parseDouble(newLine.substring(31, 33)) * 60 * 60 +
									Double.parseDouble(newLine.substring(33, 35)) * 60;
						} catch (Exception e) {
						}
						double departureTime = 0;
						try {
							departureTime = Double.parseDouble(newLine.substring(38, 40)) * 60 * 60 +
									Double.parseDouble(newLine.substring(40, 42)) * 60;
						} catch (Exception e) {

						}
						currentRouteFPLAN.addStop(newLine.substring(0, 7), arrivalTime, departureTime);
					} else {
						log.error("Laufweg-Line before appropriate *Z-Line.");
					}
				}
				newLine = readsLines.readLine();
			}
			readsLines.close();
			// Create lines:
			for (Id<TransitLine> transitLine : linesFPLAN.keySet()) {
				linesFPLAN.get(transitLine).createLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void printVehiclesUndefined() {
		for (String vehicleUndefined : vehiclesUndefined.keySet()) {
			log.warn("Undefined vehicle " + vehicleUndefined + " occured in " + vehiclesUndefined.get(vehicleUndefined) + " routes.");
		}
	}

	private class PtLineFPLAN {
		public final Id<TransitLine> lineId;
		private final List<PtRouteFPLAN> routesFPLAN = new ArrayList<>();
		public final Set<Id<TransitRoute>> idRoutesFPLAN = new HashSet<>();

		public PtLineFPLAN(String lineId) {
			this.lineId = Id.create(lineId, TransitLine.class);
		}

		public void createLine() {
			TransitLine line = scheduleBuilder.createTransitLine(lineId);
			for (PtRouteFPLAN route : this.routesFPLAN) {
				TransitRoute transitRoute = route.getRoute();
				if (transitRoute != null) {
					line.addRoute(transitRoute);
				}
			}
			if (!line.getRoutes().isEmpty()) {
				schedule.addTransitLine(line);
				//log.info("Added " + schedule.getTransitLines().get(lineId).toString());
			}
		}

		public void addPtRouteFPLAN(PtRouteFPLAN route) {
			routesFPLAN.add(route);
			idRoutesFPLAN.add(route.routeId);
		}
	}

	private class PtRouteFPLAN {
		public final Id<TransitLine> idOwnerLine;
		private final Id<TransitRoute> routeId;
		private final int numberOfDepartures;
		private final int cycleTime; // [sec]
		private final List<TransitRouteStop> stops = new ArrayList<>();

		private int firstDepartureTime = -1; //[sec]

		public void setFirstDepartureTime(int hour, int minute) {
			if (firstDepartureTime < 0) {
				this.firstDepartureTime = (hour * 3600) + (minute * 60);
			}
		}

		private String usedVehicleID = null;
		private VehicleType usedVehicleType = null;

		public void setUsedVehicle(String usedVehicle) {
			Id<VehicleType> typeId = Id.create(usedVehicle.trim(), VehicleType.class);
			usedVehicleType = vehicles.getVehicleTypes().get(typeId);
			if (usedVehicleType == null) {
				Integer occurances = vehiclesUndefined.get(usedVehicle.trim());
				if (occurances == null) {
					vehiclesUndefined.put(usedVehicle.trim(), 1);
				} else {
					vehiclesUndefined.put(usedVehicle.trim(), occurances + 1);
				}
			}
			usedVehicleID = typeId.toString() + "_" + idOwnerLine.toString() + "_" + routeId.toString();
		}

		public PtRouteFPLAN(Id<TransitLine> idOwnerLine, Id<TransitRoute> routeId, int numberOfDepartures, int cycleTime) {
			this.idOwnerLine = idOwnerLine;
			this.routeId = routeId;
			this.numberOfDepartures = numberOfDepartures + 1; // Number gives all occurrences of route additionally to first... => +1
			this.cycleTime = cycleTime * 60; // Cycle time is given in minutes in HAFAS -> Have to change it here...
		}

		/**
		 * Creates a schedule-route with the set characteristics.
		 * @return TransitRoute or NULL if no departures can be created for the route.
		 */
		public TransitRoute getRoute() {
			List<Departure> departures = this.getDepartures();
			if (departures != null) {
				TransitRoute transitRoute = scheduleBuilder.createTransitRoute(routeId, null, stops, "pt");
				for (Departure departure : departures) {
					transitRoute.addDeparture(departure);
				}
				return transitRoute;
			} else {
				return null;
			}
		}

		/**
		 * @param stopId
		 * @param arrivalTime   Expected as seconds from midnight or zero if not available.
		 * @param departureTime Expected as seconds from midnight or zero if not available.
		 */
		public void addStop(String stopId, double arrivalTime, double departureTime) {
			TransitStopFacility stopFacility = schedule.getFacilities().get(Id.create(stopId, TransitStopFacility.class));
			if (stopFacility == null) {
				log.error(idOwnerLine.toString() + "-" + routeId.toString() + ": " + "Stop facility " + stopId + " not found in facilities. Stop will not be added to route. Please check.");
				return;
			}
			double arrivalDelay = 0.0;
			if (arrivalTime > 0 && firstDepartureTime > 0) {
				arrivalDelay = arrivalTime + initialDelay - firstDepartureTime;
			}
			double departureDelay = 0.0;
			if (departureTime > 0 && firstDepartureTime > 0) {
				departureDelay = departureTime + initialDelay - firstDepartureTime;
			} else if (arrivalDelay > 0) {
				departureDelay = arrivalDelay + initialDelay;
			}
			stops.add(createRouteStop(stopFacility, arrivalDelay, departureDelay));
		}

		/**
		 * @return A list of all departures of this route.
		 * If firstDepartureTime or usedVehicle are not set before this is called, null is returned.
		 * If vehicleType is not set, the vehicle is not in the list and entry will not be created.
		 */
		private List<Departure> getDepartures() {
			if (firstDepartureTime < 0 || usedVehicleID == null) {
				log.error("getDepartures before first departureTime and usedVehicleId set.");
				return null;
			}
			if (usedVehicleType == null) {
				//log.warn("VehicleType not defined in vehicles list.");
				return null;
			}

			List<Departure> departures = new ArrayList<>();
			for (int i = 0; i < numberOfDepartures; i++) {
				// Departure ID
				Id<Departure> departureId = Id.create(idOwnerLine.toString() + "_" + routeId.toString() + "_" + String.format("%04d", i + 1), Departure.class);
				// Departure time
				double departureTime = firstDepartureTime + (i * cycleTime) - initialDelay;
				// Departure vehicle
				Id<Vehicle> vehicleId = Id.create(usedVehicleID + "_" + String.format("%04d", i + 1), Vehicle.class);
				vehicles.addVehicle(vehicleBuilder.createVehicle(vehicleId, usedVehicleType));
				// Departure
				departures.add(createDeparture(departureId, departureTime, vehicleId));
			}
			return departures;
		}

		private TransitRouteStop createRouteStop(TransitStopFacility stopFacility, double arrivalDelay, double departureDelay) {
			TransitRouteStop routeStop = scheduleBuilder.createTransitRouteStop(stopFacility, arrivalDelay, departureDelay);
			routeStop.setAwaitDepartureTime(true); // Only *T-Lines (currently not implemented) would have this as false...
			return routeStop;
		}

		private Departure createDeparture(Id<Departure> departureId, double departureTime, Id<Vehicle> vehicleId) {
			Departure departure = scheduleBuilder.createDeparture(departureId, departureTime);
			departure.setVehicleId(vehicleId);
			return departure;
		}
	}

}
