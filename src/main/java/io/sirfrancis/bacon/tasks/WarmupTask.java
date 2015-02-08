package io.sirfrancis.bacon.tasks;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMultimap;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import io.dropwizard.servlets.tasks.Task;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.RatingDAO;
import io.sirfrancis.bacon.db.RecommendationsDAO;
import io.sirfrancis.bacon.db.UserDAO;
import io.sirfrancis.bacon.util.StringRandomizer;

import java.io.PrintWriter;
import java.util.Random;

public class WarmupTask extends Task {
	private OrientGraphFactory factory;
	private UserDAO userDAO;
	private RatingDAO ratingDAO;
	private RecommendationsDAO recommendationsDAO;
	private StringRandomizer randomizer = new StringRandomizer(15);

	public WarmupTask(OrientGraphFactory factory) {
		super("warmup");
		this.factory = factory;
		userDAO = new UserDAO(factory);
		ratingDAO = new RatingDAO(factory);
		recommendationsDAO = new RecommendationsDAO(factory);
	}

	@Override
	@Timed
	public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
		warmupJIT(BaconConfiguration.getWarmupIterations());
	}

	private void warmupJIT(int cycles) {
		OrientGraph graph = factory.getTx();
		try {
			for (int iterations = 0; iterations < cycles; iterations++) {
				String username = randomizer.nextString();
				String password = randomizer.nextString();
				Optional<User> userOptional = userDAO.createUser(username, password);

				//if that username is in use, let's go on to our next attempt
				if (!userOptional.isPresent()) {
					continue;
				}

				User user = userOptional.get();

				Random random = new Random();
				int modulo = random.nextInt(500) + 1000;
				int counter = 0;

				//add a bunch of bogus ratings
				for (Vertex v : graph.getVerticesOfClass("Movie")) {
					if (counter % modulo == 0) {
						String imdbID = v.getProperty("imdbID");
						ratingDAO.addRating(user, imdbID, random.nextInt(10));
					}
					counter++;
				}

				recommendationsDAO.buildRecommendations(user, 100);

				userDAO.deleteUser(user);
			}
		} finally {
			graph.shutdown();
		}
	}
}
