package io.sirfrancis.bacon;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.sirfrancis.bacon.health.TemplateHealthCheck;
import io.sirfrancis.bacon.resources.HelloWorldResource;

/**
 * Created by Adam on 1/4/2015.
 */
public class BaconApplication extends Application<BaconConfiguration> {
	public static void main(String[] args) throws Exception {
		new BaconApplication().run(args);
	}

	@Override
	public String getName() {
		return "bacon";
	}

	@Override
	public void initialize(Bootstrap<BaconConfiguration> bootstrap) {
		//fill in later
	}

	@Override
	public void run(BaconConfiguration config, Environment environment) {
		final HelloWorldResource resource = new HelloWorldResource(
				config.getTemplate(),
				config.getDefaultName()
		);


		final TemplateHealthCheck healthCheck =
				new TemplateHealthCheck(config.getTemplate());

		environment.healthChecks().register("template",healthCheck);
		environment.jersey().register(resource);
	}
}
