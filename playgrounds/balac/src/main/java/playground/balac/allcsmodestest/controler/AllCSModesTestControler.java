package playground.balac.allcsmodestest.controler;

import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.scenario.ScenarioUtils;

import playground.balac.allcsmodestest.controler.listener.AllCSModesTestListener;
import playground.balac.allcsmodestest.qsim.AllCSModesQsimFactory;
import playground.balac.allcsmodestest.scoring.AllCSModesScoringFunctionFactory;
import playground.balac.freefloating.config.FreeFloatingConfigGroup;
import playground.balac.freefloating.router.FreeFloatingRoutingModule;
import playground.balac.onewaycarsharingredisgned.config.OneWayCarsharingRDConfigGroup;
import playground.balac.onewaycarsharingredisgned.router.OneWayCarsharingRDRoutingModule;
import playground.balac.twowaycarsharingredisigned.config.TwoWayCSConfigGroup;
import playground.balac.twowaycarsharingredisigned.router.TwoWayCSRoutingModule;

public class AllCSModesTestControler extends Controler{
	
	
	public AllCSModesTestControler(Scenario scenario) {
		super(scenario);
	}


	public void init(Config config, Network network, Scenario sc) {
		AllCSModesScoringFunctionFactory allCSModesScoringFunctionFactory = new AllCSModesScoringFunctionFactory(
				      config, 
				      network, sc);
	    this.setScoringFunctionFactory(allCSModesScoringFunctionFactory); 	
				
		}
	
	@Override
	  protected void loadControlerListeners() {  
		  
	    super.loadControlerListeners();   
	    this.addControlerListener(new AllCSModesTestListener(this.getConfig().getModule("TwoWayCarsharing").getValue("statsFileName"), this.getConfig().getModule("FreeFloating").getValue("statsFileName"),this.getConfig().getModule("OneWayCarsharing").getValue("statsFileName")));
	  }
	public static void main(final String[] args) {
		
    	final Config config = ConfigUtils.loadConfig(args[0]);
    	OneWayCarsharingRDConfigGroup configGroup = new OneWayCarsharingRDConfigGroup();
    	config.addModule(configGroup);
    	
    	FreeFloatingConfigGroup configGroupff = new FreeFloatingConfigGroup();
    	config.addModule(configGroupff);
    	
    	TwoWayCSConfigGroup configGrouptw = new TwoWayCSConfigGroup();
    	config.addModule(configGrouptw);
    	
		final Scenario sc = ScenarioUtils.loadScenario(config);
		
		
		final AllCSModesTestControler controler = new AllCSModesTestControler( sc );
		
		try {
			controler.setMobsimFactory( new AllCSModesQsimFactory(sc, controler) );
		
		controler.setTripRouterFactory(
				new TripRouterFactory() {
					@Override
					public TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext) {
						// this factory initializes a TripRouter with default modules,
						// taking into account what is asked for in the config
					
						// This allows us to just add our module and go.
						final TripRouterFactory delegate = DefaultTripRouterFactoryImpl.createRichTripRouterFactoryImpl(controler.getScenario());

						final TripRouter router = delegate.instantiateAndConfigureTripRouter(routingContext);
						
						// add our module to the instance
						router.setRoutingModule(
							"twowaycarsharing",
							new TwoWayCSRoutingModule());
						
						router.setRoutingModule(
								"freefloating",
								new FreeFloatingRoutingModule());
						
						router.setRoutingModule(
								"onewaycarsharing",
								new OneWayCarsharingRDRoutingModule());
						
						// we still need to provide a way to identify our trips
						// as being twowaycarsharing trips.
						// This is for instance used at re-routing.
						final MainModeIdentifier defaultModeIdentifier =
							router.getMainModeIdentifier();
						router.setMainModeIdentifier(
								new MainModeIdentifier() {
									@Override
									public String identifyMainMode(
											final List<PlanElement> tripElements) {
										for ( PlanElement pe : tripElements ) {
											if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "twowaycarsharing" ) ) {
												return "twowaycarsharing";
											}
											else if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "onewaycarsharing" ) ) {
												return "onewaycarsharing";
											}
											else if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "freefloating" ) ) {
												return "freefloating";
											}
										}
										// if the trip doesn't contain a onewaycarsharing leg,
										// fall back to the default identification method.
										return defaultModeIdentifier.identifyMainMode( tripElements );
									}
								});
						
						return router;
					}

					
				});
		

      controler.init(config, sc.getNetwork(), sc);		
		
		controler.run();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

}