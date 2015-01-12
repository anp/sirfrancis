package io.sirfrancis.bacon.db;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import io.sirfrancis.bacon.BaconConfiguration;

/**
 * Created by Adam on 1/11/2015.
 */
public class OrientFactoryWrapper {
	private static String dbPath;
	private static OrientGraphFactory factory;

	public OrientFactoryWrapper(BaconConfiguration config) {
		dbPath = config.getDBPath();
		factory = new OrientGraphFactory(dbPath);
	}

	public static OrientGraph getGraphInstance() {
		if (factory != null) {
			return factory.getTx();
		} else {
			return new OrientGraph(dbPath);
		}
	}
}
