package air.scenario;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * 
 * @deprecated use DgCreateFlightScenario to create a flight scenario
 */
@Deprecated
public class SfOsm2Matsim {

	public static void main(String[] args) {
		SfOsmAerowayParser osmReader = new SfOsmAerowayParser(TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
						TransformationFactory.WGS84));
		String input = args[0];				// OSM Input File
		osmReader.parse(input);
		osmReader.writeToFile(args[1]);
	}

}
