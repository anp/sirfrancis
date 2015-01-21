package io.sirfrancis.bacon;

import io.dropwizard.Application;
import io.dropwizard.auth.basic.BasicAuthProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.sirfrancis.bacon.auth.HTTPAuthenticator;
import io.sirfrancis.bacon.cli.DBInitCommand;
import io.sirfrancis.bacon.db.UserDAO;
import io.sirfrancis.bacon.health.OrientHealthCheck;
import io.sirfrancis.bacon.resources.*;
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

		environment.jersey().register(new UserCreateResource(new UserDAO(config.getFactory())));
		environment.jersey().register(new UserDeleteResource(new UserDAO(config.getFactory())));

		environment.jersey().register(new BasicAuthProvider<>(new HTTPAuthenticator(), "sirfrancis.io"));

		environment.healthChecks().register("database",new OrientHealthCheck(config.getFactory()));
	}
}