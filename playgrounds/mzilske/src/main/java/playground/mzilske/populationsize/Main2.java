/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Main2.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.populationsize;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;

public class Main2 {

	public static void main(String[] args) {
		final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/");
		final RegimeResource uncongested = experiment.getRegime("uncongested");

        Scenario baseScenario = uncongested.getBaseRun().getOutputScenario();
        Population basePopulation = baseScenario.getPopulation();

//        Map<Id, Double> travelledDistancePerPerson = PowerPlans.travelledDistancePerPerson(basePopulation, baseScenario.getNetwork());
//        CloneHistogram.cloneHistogram(basePopulation, travelledDistancePerPerson, uncongested.getMultiRateRun("cadyts").getRateRun("two_0", "5"));
//        CloneHistogram.cloneHistogram(basePopulation, travelledDistancePerPerson, uncongested.getMultiRateRun("cadyts").getRateRun("two_5_3", "5"));
//        CloneHistogram.cloneHistogram(basePopulation, travelledDistancePerPerson, uncongested.getMultiRateRun("cadyts").getRateRun("two_10", "5"));
//        CloneHistogram.cloneHistogram(basePopulation, travelledDistancePerPerson, uncongested.getMultiRateRun("cadyts").getRateRun("two_20", "5"));
//        CloneHistogram.cloneHistogram(basePopulation, travelledDistancePerPerson, uncongested.getMultiRateRun("cadyts").getRateRun("two_50", "5"));


//        uncongested.getMultiRateRun("cadyts").twoRates("5");

//        uncongested.getMultiRateRun("cadyts").twoRates("0");
//        uncongested.getMultiRateRun("cadyts").simulateRate("two_5_6", 5);
//        uncongested.getMultiRateRun("cadyts").twoRates("10");

//        uncongested.getMultiRateRun("cadyts").simulateRate("two_0", 5);

//        uncongested.getMultiRateRun("cadyts").simulateRate("two_5", 5);
//        uncongested.getMultiRateRun("cadyts").simulateRate("two_10", 5);
//        uncongested.getMultiRateRun("cadyts").twoRates("20");
//        uncongested.getMultiRateRun("cadyts").simulateRate("two_20", 5);
//        uncongested.getMultiRateRun("cadyts").twoRates("50");
//        uncongested.getMultiRateRun("cadyts").simulateRate("two_50", 5);

//        uncongested.getMultiRateRun("cadyts").simulateRate("two_0", 10);

//        uncongested.getMultiRateRun("cadyts").simulateRate("two_5", 10);
//        uncongested.getMultiRateRun("cadyts").simulateRate("two_10", 10);
//        uncongested.getMultiRateRun("cadyts").simulateRate("two_20", 10);
        uncongested.getMultiRateRun("cadyts").simulateRate("two_50", 10);



//       uncongested.getMultiRateRun("cadyts").getRateRun("two_5", "5").cloneStatistics();
//         uncongested.getMultiRateRun("cadyts").distances2();

//         uncongested.getMultiRateRun("cadyts").errors();
	}

}