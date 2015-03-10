package io.sirfrancis.bacon.db;

import com.orientechnologies.orient.core.exception.OTransactionException;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.core.Movie;
import io.sirfrancis.bacon.core.Rating;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.enums.Edges;
import io.sirfrancis.bacon.db.enums.Indexes;
import io.sirfrancis.bacon.db.enums.UserProps;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class RatingDAO {
	private MovieDAO movieDAO;
	private int maxRetries;

	public RatingDAO() {
		this.movieDAO = new MovieDAO();
		this.maxRetries = BaconConfiguration.getMaxRetries();
	}

	public Rating addRating(User user, String imdbID, int rating) {
		OrientGraph graph = GraphConnection.getGraph();
		if (rating < 0 || rating > 10)
			throw new IllegalArgumentException("Invalid number for rating.");

		Rating addedRating = null;

		for (int i = 0; i < maxRetries; i++) {
			try {
				OrientVertex userVertex = graph.getVertex(graph.getVertexByKey(Indexes.USER_USERNAME, user.getUsername()).getId());
				OrientVertex movieVertex = graph.getVertex(graph.getVertexByKey(Indexes.MOVIE_IMDBID, imdbID).getId());

				for (Edge e : userVertex.getEdges(movieVertex, Direction.OUT, Edges.RATED)) {
					graph.removeEdge(e);
				}

				Edge ratingEdge = graph.addEdge(null, userVertex, movieVertex, Edges.RATED);
				ratingEdge.setProperty(Edges.RATING, rating);

				Movie ratedMovie = movieDAO.buildMovie(movieVertex);
				addedRating = new Rating(ratedMovie, rating);

				userVertex.setProperty(UserProps.RAT_UPDATED, System.currentTimeMillis());

				graph.commit();
				break;
			} catch (OTransactionException ote) {
			}
		}

		graph.shutdown();

		return addedRating;
	}

	public List<Rating> getRatings(User user) {
		OrientGraph graph = GraphConnection.getGraph();
		List<Rating> ratings = new LinkedList<>();

		try {
			Vertex userVertex = graph.getVertexByKey(Indexes.USER_USERNAME, user.getUsername());
			for (Edge e : userVertex.getEdges(Direction.OUT, Edges.RATED)) {
				int rating = e.getProperty(Edges.RATING);

				Vertex movieVertex = e.getVertex(Direction.IN);
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
