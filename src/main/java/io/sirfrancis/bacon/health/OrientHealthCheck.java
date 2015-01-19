package io.sirfrancis.bacon.health;

import com.codahale.metrics.health.HealthCheck;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

/**
 * Created by anp68 on 1/19/2015.
 */
public class OrientHealthCheck extends HealthCheck{
	private OrientGraphFactory factory;

	public OrientHealthCheck(OrientGraphFactory factory) {
		this.factory = factory;
	}

	@Override
	protected Result check() throws Exception {
		if (!factory.exists()) {
			return Result.unhealthy("Database connection failed");
		}
		return Result.healthy();
	}
}
