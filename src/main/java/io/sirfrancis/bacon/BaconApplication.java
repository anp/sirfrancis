package io.sirfrancis.bacon;

import com.codahale.metrics.MetricRegistry;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import io.dropwizard.Application;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.basic.BasicAuthProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.sirfrancis.bacon.auth.HTTPAuthenticator;
import io.sirfrancis.bacon.cli.BootstrapDBCommand;
import io.sirfrancis.bacon.db.*;
import io.sirfrancis.bacon.health.OrientHealthCheck;
import io.sirfrancis.bacon.resources.*;
import io.sirfrancis.bacon.tasks.CreateQuizPathTask;
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
	}

	@Override
	public void run(BaconConfiguration config, Environment environment) {
		config.initFactory();

		OrientGraphFactory factory = BaconConfiguration.getFactory();

		//user creation/deletion api resources
		int maxDbRetries = BaconConfiguration.getMaxDbRetries();
		UserDAO userDAO = new UserDAO(factory);

		environment.jersey().register(new UserCreateResource(userDAO));

		environment.jersey().register(new UserCreateConfirmResource(userDAO));

		environment.jersey().register(new UserDeleteResource(userDAO));

		environment.jersey().register(new UserForgotPasswordResource(userDAO));

		environment.jersey().register(new UserChangePasswordResource(userDAO));

		//movie search api resource
		MovieDAO movieDAO = new MovieDAO(factory);
		environment.jersey().register(new MovieSearchResource(movieDAO));

		//rating add/list/ignore resources
		RatingDAO ratingDAO = new RatingDAO(factory);

		environment.jersey().register(new RatingAddResource(ratingDAO));

		environment.jersey().register(new RatingGetResource(ratingDAO));

		environment.jersey().register(new RatingIgnoreResource(ratingDAO));

		//recommendations resource
		RecommendationsDAO recommendationsDAO = new RecommendationsDAO(factory);

		environment.jersey().register(new RecommendationsResource(recommendationsDAO));

		//quiz resource
		QuizDAO quizDAO = new QuizDAO(factory);

		environment.jersey().register(new QuizResource(quizDAO));


		//authentication
		environment.jersey().register(
				new BasicAuthProvider<>(
						new CachingAuthenticator<>(new MetricRegistry(), new HTTPAuthenticator(),
								config.getAuthenticationCachePolicy()), "sirfrancis.io"));

		//db healthcheck
		environment.healthChecks().register("database", new OrientHealthCheck(factory));

		//db warmup task
		environment.admin().addTask(new WarmupTask(factory));

		//db quiz path task
		environment.admin().addTask(new CreateQuizPathTask(factory));
	}
}