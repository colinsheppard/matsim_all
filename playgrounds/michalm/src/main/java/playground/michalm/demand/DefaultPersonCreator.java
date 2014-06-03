/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

package playground.michalm.demand;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;

import playground.michalm.zone.Zone;


public class DefaultPersonCreator
    implements PersonCreator
{
    private final Scenario scenario;
    private final PopulationFactory pf;

    private int curentAgentId = 0;


    public DefaultPersonCreator(Scenario scenario)
    {
        this.scenario = scenario;
        this.pf = scenario.getPopulation().getFactory();
    }


    @Override
    public Person createPerson(Plan plan, Zone fromZone, Zone toZone)
    {
        String strId = String.format("%07d", curentAgentId++);
        Person person = pf.createPerson(scenario.createId(strId));
        person.addPlan(plan);
        return person;
    }
}