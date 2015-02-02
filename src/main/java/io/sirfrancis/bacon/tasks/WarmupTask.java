package io.sirfrancis.bacon.tasks;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMultimap;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import io.dropwizard.servlets.tasks.Task;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.RatingDAO;
import io.sirfrancis.bacon.db.RecommendationsDAO;
import io.sirfrancis.bacon.db.UserDAO;

import java.io.PrintWriter;
import java.util.Random;

public class WarmupTask extends Task {
	private OrientGraphFactory factory;
	private UserDAO userDAO;
	private RatingDAO ratingDAO;
	private RecommendationsDAO recommendationsDAO;
	private RandomString randomizer = new RandomString(15);
	private int maxRetries = BaconConfiguration.getMaxDbRetries();

	public WarmupTask(OrientGraphFactory factory, String amazonPrefix) {
		super("warmup");
		this.factory = factory;
		userDAO = new UserDAO(factory, maxRetries);
		ratingDAO = new RatingDAO(factory, maxRetries, amazonPrefix);
		recommendationsDAO = new RecommendationsDAO(factory, maxRetries, amazonPrefix);
	}

	@Override
	@Timed
	public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
		warmupDB();
		warmupJIT(BaconConfiguration.getWarmupIterations());
	}

	private void warmupDB() {
		OrientGraph graph = factory.getTx();

		try {
			for (Vertex v : graph.getVertices()) {
				for (Edge e : v.getEdges(Direction.BOTH)) {
					for (String key : e.getPropertyKeys()) {
						Object assigner = e.getProperty(key);
						int hashCode = assigner.hashCode();
					}
				}

				for (String key : v.getPropertyKeys()) {
					Object assigner = v.getProperty(key);
					int hashCode = assigner.hashCode();
				}
			}
		} finally {
			graph.shutdown();
		}
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

	private class RandomString {

		private final Random random = new Random();
		private char[] symbols;
		private char[] buf;

		public RandomString(int length) {
			StringBuilder tmp = new StringBuilder();
			for (char ch = '0'; ch <= '9'; ++ch)
				tmp.append(ch);
			for (char ch = 'a'; ch <= 'z'; ++ch)
				tmp.append(ch);
			symbols = tmp.toString().toCharArray();

			if (length < 1)
				throw new IllegalArgumentException("length < 1: " + length);
			buf = new char[length];
		}

		public String nextString() {
			for (int idx = 0; idx < buf.length; ++idx)
				buf[idx] = symbols[random.nextInt(symbols.length)];
			return new String(buf);
		}
	}
}
