package io.sirfrancis.bacon.db;

import com.orientechnologies.orient.core.exception.OTransactionException;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import io.sirfrancis.bacon.core.Movie;
import io.sirfrancis.bacon.core.Rating;
import io.sirfrancis.bacon.core.User;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class RatingDAO {
	private OrientGraphFactory factory;
	private MovieDAO movieDAO;
	private int maxRetries;

	public RatingDAO(OrientGraphFactory factory, int maxRetries, String amazonPrefix) {
		this.factory = factory;
		this.movieDAO = new MovieDAO(factory, amazonPrefix);
		this.maxRetries = maxRetries;
	}

	public Rating addRating(User user, String imdbID, int rating) {
		if (rating < 0 || rating > 10)
			throw new IllegalArgumentException("Invalid number for rating.");

		OrientGraph graph = factory.getTx();
		Rating addedRating = null;

		for (int retry = 0; retry < maxRetries; retry++) {
			try {
				OrientVertex userVertex = graph.getVertex(graph.getVertexByKey("User.username", user.getUsername()).getId());
				OrientVertex movieVertex = graph.getVertex(graph.getVertexByKey("Movie.imdbID", imdbID).getId());

				for (Edge e : userVertex.getEdges(movieVertex, Direction.OUT, "rated")) {
					graph.removeEdge(e);
				}

				Edge ratingEdge = graph.addEdge(null, userVertex, movieVertex, "rated");
				ratingEdge.setProperty("rating", rating);

				Movie ratedMovie = movieDAO.buildMovie(movieVertex);
				addedRating = new Rating(ratedMovie, rating);

				graph.commit();
				break;
			} catch (OTransactionException e) {
			}
		}
		graph.shutdown();

		return addedRating;
	}

	public List<Rating> getRatings(User user) {
		OrientGraph graph = factory.getTx();
		List<Rating> ratings = new LinkedList<>();

		try {
			Vertex userVertex = graph.getVertexByKey("User.username", user.getUsername());
			for (Edge e : userVertex.getEdges(Direction.OUT, "rated")) {
				OrientEdge oe = graph.getEdge(e.getId());
				int rating = e.getProperty("rating");

				Vertex movieVertex = oe.getVertex(Direction.IN);
				Movie movie = movieDAO.buildMovie(movieVertex);

				ratings.add(new Rating(movie, rating));
			}
		} finally {
			graph.shutdown();
		}

		ratings.sort(new RatingComparator());

		return ratings;
	}

	private class RatingComparator implements Comparator<Rating> {
		@Override
		public int compare(Rating o1, Rating o2) {
			if (o2.getRating() == o2.getRating()) {
				return o2.getMovie().getTitle().compareTo(
						o1.getMovie().getTitle());
			}
			return o2.getRating() - o1.getRating();
		}
	}
}
