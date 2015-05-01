package io.sirfrancis.bacon;

import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.CacheBuilder;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthFactory;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.basic.BasicAuthFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.sirfrancis.bacon.auth.HTTPAuthenticator;
import io.sirfrancis.bacon.auth.SaltedHasher;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.*;
import io.sirfrancis.bacon.health.DBHealthCheck;
import io.sirfrancis.bacon.resources.*;
import io.sirfrancis.bacon.tasks.BackupAndDownloadOMDBExportTask;
import io.sirfrancis.bacon.tasks.DBUpdateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;

public class BaconApplication extends Application<BaconConfiguration> {
	private static Logger log = LoggerFactory.getLogger(BaconApplication.class);

	public static void main(String[] args) throws Exception {
		new BaconApplication().run(args);
	}

	@Override
	public String getName() {
		return "bacon";
	}

	@Override
	public void initialize(final Bootstrap<BaconConfiguration> bootstrap) {
		//bootstrap.addBundle(new OrientServerBundle<>(getConfigurationClass()));

	}

	@Override
	public void run(BaconConfiguration config, Environment environment) {
		try {
			SaltedHasher.setSecretKeyFactory();
		} catch (NoSuchAlgorithmException nsae) {
			log.error("Unable to initialize password hasher -- default algorithm not found. Exiting.");
			throw new RuntimeException("Exiting -- password hashing algorithm unavailable.");
		}

		environment.lifecycle().manage(new GraphConnection());
		//user creation/deletion api resources
		UserDAO userDAO = new UserDAO();

		environment.jersey().register(new UserCreateResource(userDAO));

		environment.jersey().register(new UserCreateConfirmResource(userDAO));

		environment.jersey().register(new UserDeleteResource(userDAO));

		environment.jersey().register(new UserForgotPasswordResource(userDAO));

		environment.jersey().register(new UserChangePasswordResource(userDAO));

		environment.jersey().register(new UserExistsResource(userDAO));

		//movie search api resource
		MovieDAO movieDAO = new MovieDAO();
		environment.jersey().register(new MovieSearchResource(movieDAO));

		//rating add/list/ignore resources
		RatingDAO ratingDAO = new RatingDAO();

		environment.jersey().register(new RatingAddResource(ratingDAO));

		environment.jersey().register(new RatingGetResource(ratingDAO));

		environment.jersey().register(new RatingIgnoreResource(ratingDAO));

		//recommendations resource
		RecommendationsDAO recommendationsDAO = new RecommendationsDAO();

		environment.jersey().register(new RecommendationsResource(recommendationsDAO));

		//quiz resource
		QuizDAO quizDAO = new QuizDAO();

		environment.jersey().register(new QuizResource(quizDAO));


		//authentication
		MetricRegistry metricRegistry = new MetricRegistry();
		HTTPAuthenticator httpAuthenticator = new HTTPAuthenticator(new UserDAO());
		CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.from(config.getAuthenticationCachePolicy());

		BasicAuthFactory<User> authFactory = new BasicAuthFactory<>(
				new CachingAuthenticator<>(metricRegistry, httpAuthenticator, cacheBuilder),
				"sirfrancis.io", User.class);

		environment.jersey().register(AuthFactory.binder(authFactory));

		//db healthcheck
		environment.healthChecks().register("database", new DBHealthCheck());

		//db update task
		environment.admin().addTask(new DBUpdateTask());

		environment.admin().addTask(new BackupAndDownloadOMDBExportTask());

		//while (GraphConnection.factoryIsNull()) {}
		//quizDAO.initQuiz();
	}
}
