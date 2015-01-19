package io.sirfrancis.bacon;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.sirfrancis.bacon.cli.DBInitCommand;
import io.sirfrancis.bacon.health.OrientHealthCheck;
import io.sirfrancis.bacon.resources.AboutResource;
import io.sirfrancis.bacon.resources.AlmostConfirmedResource;
import io.sirfrancis.bacon.resources.HomeResource;
import io.sirfrancis.bacon.resources.SubConfirmedResource;
import ru.vyarus.dropwizard.orient.OrientServerBundle;

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
		//OrientDB
		bootstrap.addBundle(new OrientServerBundle<>(getConfigurationClass()));
		//CLI command
		bootstrap.addCommand(new DBInitCommand());
		//add HTML rendering/views
		bootstrap.addBundle(new ViewBundle());
	}

	@Override
	public void run(BaconConfiguration config, Environment environment) {
		config.initFactory();

		environment.jersey().register(new HomeResource(config));
		environment.jersey().register(new AboutResource(config));
		environment.jersey().register(new AlmostConfirmedResource(config));
		environment.jersey().register(new SubConfirmedResource(config));

		environment.healthChecks().register("database",new OrientHealthCheck(config.getFactory()));
	}
}