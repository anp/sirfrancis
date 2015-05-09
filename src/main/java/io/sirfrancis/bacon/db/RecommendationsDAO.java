package io.sirfrancis.bacon.db;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.intent.OIntentMassiveRead;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import io.sirfrancis.bacon.core.Movie;
import io.sirfrancis.bacon.core.Recommendation;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.enums.Edges;
import io.sirfrancis.bacon.db.enums.Indexes;
import io.sirfrancis.bacon.db.enums.PersonProps;
import io.sirfrancis.bacon.db.enums.UserProps;

import java.util.*;

public class RecommendationsDAO {
	private MovieDAO movieDAO;

	public RecommendationsDAO() {
		this.movieDAO = new MovieDAO();
	}

	public boolean needToUpdateRecommendations(User user) {
		OrientGraph graph = GraphConnection.getGraph();
		boolean needUpdating;
		try {
			Vertex userVertex = graph.getVertexByKey(Indexes.USER_USERNAME, user.getUsername());

			long ratingsUpdated = userVertex.getProperty(UserProps.RAT_UPDATED);
			long recommendationsUpdated = userVertex.getProperty(UserProps.REC_UPDATED);

			needUpdating = (recommendationsUpdated > ratingsUpdated);
		} finally {
			graph.shutdown();
		}

		return needUpdating;
	}

	@Timed
	@Metered
	public List<Recommendation> getRecommendations(User user, int maxRecommendations) {
		OrientGraph graph = GraphConnection.getGraph();
		List<Recommendation> recommendations = new LinkedList<>();

		try {
			Vertex userVertex = graph.getVertexByKey(Indexes.USER_USERNAME, user.getUsername());
			for (Edge e : userVertex.getEdges(Direction.IN, Edges.RECOMMENDED)) {
				int score = e.getProperty("score");

				Vertex movieVertex = e.getVertex(Direction.OUT);
				Movie movie = movieDAO.buildMovie(movieVertex);

				recommendations.add(new Recommendation(movie, score));
			}

		} finally {
			graph.shutdown();
		}

		recommendations.sort((o1, o2) -> o2.getScore() - o1.getScore());

		int lastIndex = recommendations.size() < maxRecommendations ? recommendations.size() : maxRecommendations;

		return recommendations.subList(0, lastIndex);
	}


	@Timed
	@Metered
	public void writeRecommendations(User user, Set<Recommendation> recommendations) {
		OrientGraph graph = GraphConnection.getGraph();

		try {
			graph.declareIntent(new OIntentMassiveInsert());

			Vertex userVertex = graph.getVertexByKey(Indexes.USER_USERNAME, user.getUsername());

			//clear previous recommendations
			for (Edge e : userVertex.getEdges(Direction.BOTH, Edges.RECOMMENDED)) {
				graph.removeEdge(e);
			}

			recommendations.stream()
					.forEach(recommendation -> {
						String imdbID = recommendation.getMovie().getImdbID();
						int score = recommendation.getScore();
						for (int i = 0; i < 10; i++) {
							try {
								Vertex movieToRecommend = graph.getVertexByKey(Indexes.MOVIE_IMDBID, imdbID);
								Edge e = graph.addEdge(null, movieToRecommend, userVertex, Edges.RECOMMENDED);
								e.setProperty(Edges.SCORE, score);
								graph.commit();
								break;
							} catch (OConcurrentModificationException ocme) {
								//do nothing, will retry write
							}
						}
					});

			userVertex.setProperty(UserProps.REC_UPDATED, System.currentTimeMillis());
			graph.commit();
		} finally {
			graph.shutdown();
		}
	}

	@Timed
	@Metered
	public Map<Movie, Double> getNeighborMoviesAndStrengths(Movie movie) {
		OrientGraph graph = GraphConnection.getGraph();
		Map<Movie, Double> neighborMap = new HashMap<>();


		try {
			graph.declareIntent(new OIntentMassiveRead());

			//get all of the people associated with this rating
			Vertex likedMovie = graph.getVertexByKey(Indexes.MOVIE_IMDBID, movie.getImdbID());

			//for each person that is rated implicitly by movie rating
			Set<String> people = new HashSet<>();

			Iterable<Vertex> peopleVertices = likedMovie.getVertices(Direction.IN, Edges.ACTED, Edges.DIRECTED, Edges.WROTE);

			//count number of people with this role to decide on score amount
			int numPeopleOnMovieInRole = 0;
			for (Vertex person : peopleVertices) {
				numPeopleOnMovieInRole++;
			}

			//skip this if there's no one in that role
			if (numPeopleOnMovieInRole != 0) {

				//decide how much each role can add to score
				double normalizedStrength = 1 / numPeopleOnMovieInRole;

				for (Vertex likedPerson : peopleVertices) {

					String name = likedPerson.getProperty(PersonProps.NAME);
					if (!people.contains(name)) {
						people.add(name);
						//go through each movie they've worked on
						for (Vertex toRecMovieVertex : likedPerson.getVertices(Direction.OUT, Edges.ACTED, Edges.DIRECTED, Edges.WROTE)) {
							Movie toRecMovie = movieDAO.buildMovie(toRecMovieVertex);

							//add the score to the map or increment the previous score
							if (neighborMap.containsKey(toRecMovie)) {
								neighborMap.put(toRecMovie, neighborMap.get(toRecMovie) + normalizedStrength);
							} else {
								neighborMap.put(toRecMovie, normalizedStrength);
							}
						}
					}
				}
			}

		} finally {
			graph.shutdown();
		}

		return neighborMap;
	}
}
