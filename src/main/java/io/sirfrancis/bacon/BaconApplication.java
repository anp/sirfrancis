package io.sirfrancis.bacon;

import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import io.dropwizard.Application;
import io.dropwizard.auth.basic.BasicAuthProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.sirfrancis.bacon.auth.HTTPAuthenticator;
import io.sirfrancis.bacon.cli.BootstrapDBCommand;
import io.sirfrancis.bacon.db.MovieDAO;
import io.sirfrancis.bacon.db.RatingDAO;
import io.sirfrancis.bacon.db.RecommendationsDAO;
import io.sirfrancis.bacon.db.UserDAO;
import io.sirfrancis.bacon.health.OrientHealthCheck;
import io.sirfrancis.bacon.resources.*;
import io.sirfrancis.bacon.tasks.WarmupTask;
import ru.vyarus.dropwizard.orient.OrientServerBundle;

public class BaconApplication extends Application<BaconConfiguration> {

	public static void main(String[] args) throws Exception {
		new BaconApplication().run(args);
	}

	@Override
	public String getName() {
		return "bacon";
	}

	@Override
	public void initialize(final Bootstrap<BaconConfiguration> bootstrap) {
		//OrientDB
		bootstrap.addBundle(new OrientServerBundle<>(getConfigurationClass()));
		//CLI command
		bootstrap.addCommand(new BootstrapDBCommand());
		//add HTML rendering/views
		bootstrap.addBundle(new ViewBundle());
		//add static content
	}

	@Override
	public void run(BaconConfiguration config, Environment environment) {
		config.initFactory();

		OrientGraphFactory factory = BaconConfiguration.getFactory();

		//user creation/deletion api resources
		environment.jersey().register(
				new UserCreateResource(
						new UserDAO(factory, BaconConfiguration.getMaxDbRetries())));

		environment.jersey().register(
				new UserDeleteResource(
						new UserDAO(factory, BaconConfiguration.getMaxDbRetries())));

		//movie search api resource
		environment.jersey().register(
				new MovieSearchResource(
						new MovieDAO(factory, config.getAmazonPrefix())));

		//rating add/list/ignore resources
		environment.jersey().register(
				new RatingAddResource(
						new RatingDAO(
								factory, BaconConfiguration.getMaxDbRetries(), config.getAmazonPrefix())));

		environment.jersey().register(
				new RatingGetResource(
						new RatingDAO(
								factory, BaconConfiguration.getMaxDbRetries(), config.getAmazonPrefix())));

		environment.jersey().register(
				new RatingIgnoreResource(
						new RatingDAO(
								factory, BaconConfiguration.getMaxDbRetries(), config.getAmazonPrefix())));

		//recommendations resource
		environment.jersey().register(
				new RecommendationsResource(
						new RecommendationsDAO(factory, BaconConfiguration.getMaxDbRetries(), config.getAmazonPrefix())));

		//authentication
		environment.jersey().register(
				new BasicAuthProvider<>(
						new HTTPAuthenticator(), "sirfrancis.io"));

		//db healthcheck
		environment.healthChecks().register("database", new OrientHealthCheck(factory));

		//db warmup task
		environment.admin().addTask(new WarmupTask(factory, config.getAmazonPrefix()));
	}
}