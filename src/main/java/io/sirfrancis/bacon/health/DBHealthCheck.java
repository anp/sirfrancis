package io.sirfrancis.bacon.health;

import com.codahale.metrics.health.HealthCheck;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import io.sirfrancis.bacon.db.GraphConnection;

/**
 * Created by anp68 on 1/19/2015.
 */
public class DBHealthCheck extends HealthCheck {

	@Override
	protected Result check() throws Exception {
		OrientGraph graph = GraphConnection.getGraph();

		if (!GraphConnection.started()) return Result.unhealthy("Graph connection not initiated.");

		if (graph == null) return Result.unhealthy("Graph connection not initiated.");

		return Result.healthy();
	}
}
