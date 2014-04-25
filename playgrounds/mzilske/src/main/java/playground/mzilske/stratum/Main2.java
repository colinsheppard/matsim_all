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

package playground.mzilske.stratum;

import com.google.common.collect.ImmutableMap;
import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.counts.Counts;
import playground.mzilske.cadyts.CadytsModule;
import playground.mzilske.cdr.CallBehavior;
import playground.mzilske.cdr.CompareMain;
import playground.mzilske.cdr.ZoneTracker;
import playground.mzilske.cdranalysis.StreamingOutput;
import playground.mzilske.controller.CharyparNagelModule;
import playground.mzilske.controller.Controller;
import playground.mzilske.controller.ControllerModule;
import playground.mzilske.util.IterationSummaryFileControlerListener;

import java.io.IOException;
import java.io.PrintWriter;

public class Main2 {


    public static void main(String[] args) {

        Module phoneModule = new AbstractModule() {
            @Override
            protected void configure() {
                bind(ZoneTracker.LinkToZoneResolver.class).to(MyLinkToZoneResolver.class);
                bind(CallBehavior.class).to(MyCallBehavior.class);
            }
        };

        Injector injector = Guice.createInjector(
                new ControllerModule(),
                new CharyparNagelModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Config.class).toProvider(OneWorkplaceOneStratumUnderestimated.ConfigProvider.class).in(Singleton.class);
                        bind(Scenario.class).toProvider(OneWorkplaceOneStratumUnderestimated.class).in(Singleton.class);
                        bind(CompareMain.class).in(Singleton.class);
                        Multibinder<ControlerListener> controlerListenerBinder = Multibinder.newSetBinder(binder(), ControlerListener.class);
                        controlerListenerBinder.addBinding().toProvider(new CallControlerListener());
                    }
                },
                phoneModule
        );


        Controller controler = injector.getInstance(Controller.class);
        controler.run();

        final Network groundTruthNetwork = injector.getInstance(Network.class);
        final VolumesAnalyzer groundTruthVolumes = injector.getInstance(VolumesAnalyzer.class);
        final CompareMain compareMain = injector.getInstance(CompareMain.class);

        final Counts allCounts = CompareMain.volumesToCounts(groundTruthNetwork, groundTruthVolumes);

        Injector injector2 = Guice.createInjector(
                new ControllerModule(),
                new CadytsModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Network.class).annotatedWith(Names.named("groundTruthNetwork")).toInstance(groundTruthNetwork);
                        bind(VolumesAnalyzer.class).annotatedWith(Names.named("groundTruthVolumes")).toInstance(groundTruthVolumes);
                        bind(CompareMain.class).toInstance(compareMain);

                        bind(Config.class).toProvider(ScenarioReconstructor.ConfigProvider.class).in(Singleton.class);
                        bind(Scenario.class).toProvider(ScenarioReconstructor.class).in(Singleton.class);
                        bind(Counts.class).annotatedWith(Names.named("allCounts")).toInstance(allCounts);
                        bind(Counts.class).annotatedWith(Names.named("calibrationCounts")).toInstance(allCounts);

                        bind(ScoringFunctionFactory.class).to(MyScoringFunctionFactory2.class);

                        Multibinder<ControlerListener> controlerListenerBinder = Multibinder.newSetBinder(binder(), ControlerListener.class);
                        controlerListenerBinder.addBinding().to(MetaPopulationReplanningControlerListener.class);
                        controlerListenerBinder.addBinding().to(MetaPopulationScoringControlerListener.class);
                        controlerListenerBinder.addBinding().toProvider(MyControlerListenerProvider.class);
                    }
                },
                phoneModule
        );

        Controller controler2 = injector2.getInstance(Controller.class);

        controler2.run();
    }

    static class MyControlerListenerProvider implements javax.inject.Provider<ControlerListener> {

        @javax.inject.Inject
        OutputDirectoryHierarchy controlerIO;

        @javax.inject.Inject
        MetaPopulations metaPopulations;

        @Override
        public ControlerListener get() {
            return new IterationSummaryFileControlerListener(controlerIO,
                    ImmutableMap.<String, IterationSummaryFileControlerListener.Writer>of(
                            "metapopulationplans.txt",
                            new IterationSummaryFileControlerListener.Writer() {
                                @Override
                                public StreamingOutput notifyStartup(StartupEvent event) {
                                    return new StreamingOutput() {
                                        @Override
                                        public void write(PrintWriter pw) throws IOException {
                                            pw.printf("%s\t%s\t%s\t%s\n",
                                                    "iteration",
                                                    "metapopulation",
                                                    "scalefactor",
                                                    "score");
                                        }
                                    };
                                }

                                @Override
                                public StreamingOutput notifyIterationEnds(final IterationEndsEvent event) {
                                    return new StreamingOutput() {
                                        @Override
                                        public void write(PrintWriter pw) throws IOException {
                                            int i=0;
                                            for (MetaPopulation countLink : metaPopulations.getMetaPopulations()) {
                                                for (MetaPopulationPlan plan : countLink.getPlans()) {
                                                    pw.printf("%d\t%d\t%f\t%f\n",
                                                            event.getIteration(),
                                                            i,
                                                            plan.getScaleFactor(),
                                                            plan.getScore());
                                                }
                                                i++;
                                            }
                                        }
                                    };
                                }
                            }
                    ));
        }
    }

}