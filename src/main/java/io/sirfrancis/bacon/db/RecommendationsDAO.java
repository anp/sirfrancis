package io.sirfrancis.bacon.db;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.orientechnologies.orient.core.exception.OTransactionException;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.*;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.core.Movie;
import io.sirfrancis.bacon.core.Recommendation;
import io.sirfrancis.bacon.core.User;

import java.util.*;

public class RecommendationsDAO {
	private OrientGraphFactory factory;
	private MovieDAO movieDAO;
	private int maxRetries;

	public RecommendationsDAO(OrientGraphFactory factory) {
		this.factory = factory;
		movieDAO = new MovieDAO(factory);
		this.maxRetries = BaconConfiguration.getMaxDbRetries();
	}

	public static <K extends Comparable<? super K>, V extends Comparable<? super V>> Map<K, V> sortByValues(Map<K, V> map) {
		List<Map.Entry<K, V>> entries = new LinkedList<>(map.entrySet());

		Collections.sort(entries, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

		Map<K, V> sortedMap = new LinkedHashMap<>();

		for (Map.Entry<K, V> entry : entries) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}

	@Timed
	@Metered
	public List<Recommendation> getRecommendations(User user) {
		List<Recommendation> recommendations = new LinkedList<>();
		OrientGraph graph = factory.getTx();

		try {
			Vertex userVertex = graph.getVertexByKey("User.username", user.getUsername());
			for (Edge e : userVertex.getEdges(Direction.IN, "recommended")) {
				OrientEdge oe = graph.getEdge(e.getId());
				int score = e.getProperty("score");

				Vertex movieVertex = oe.getVertex(Direction.OUT);
				Movie movie = movieDAO.buildMovie(movieVertex);

				recommendations.add(new Recommendation(movie, score));
			}
		} finally {
			graph.shutdown();
		}

		recommendations.sort(new Comparator<Recommendation>() {
			@Override
			public int compare(Recommendation o1, Recommendation o2) {
				return o2.getScore() - o1.getScore();
			}
		});

		return recommendations;
	}

	@Timed
	@Metered
	public List<Recommendation> buildRecommendations(User user, int maxRecommendations) {
		OrientGraph graph = factory.getTx();

		Map<String, Integer> recMap = new HashMap<>();
		Set<String> ignoredMovies = new HashSet<>();

		int MIN_SCORE = 100;

		try {
			Vertex userVertexVanilla = graph.getVertexByKey("User.username", user.getUsername());
			OrientVertex userVertex = graph.getVertex(userVertexVanilla.getId());

			long ratingsUpdated = userVertexVanilla.getProperty("ratingsUpdated");
			long recommendationsUpdated = userVertexVanilla.getProperty("recommendationsUpdated");

			if (recommendationsUpdated > ratingsUpdated) return getRecommendations(user);

			//clear previous recommendations
			for (Edge e : userVertex.getEdges(Direction.IN, "recommended")) {
				graph.removeEdge(e);
			}

			graph.commit();

			//MANY LOOPS
			for (Edge e : userVertex.getEdges(Direction.OUT, "rated")) {
				//all recommendations are built from this rating
				int topLevelScore = e.getProperty("rating");

				//don't give score to unliked movies
				if (topLevelScore < 5) continue;

				//get all of the people associated with this rating
				Vertex likedMovie = e.getVertex(Direction.IN);

				//for each person that is rated implicitly by movie rating
				String[] personRels = { "Acted", "Directed", "Wrote" };
				for (String rel : personRels) {
					Iterable<Vertex> peopleVertices = likedMovie.getVertices(Direction.IN, rel);

					//count number of people with this role to decide on score amount
					int numPeopleOnMovieInRole = 0;
					for (Vertex person : peopleVertices) {
						numPeopleOnMovieInRole++;
					}
					//skip this if there's no one in that role
					if (numPeopleOnMovieInRole == 0) continue;

					//decide how much each role can add to score
					int itemScore = (topLevelScore * 100) / numPeopleOnMovieInRole;

					for (Vertex likedPerson : peopleVertices) {

						OrientVertex orientLikedPerson = graph.getVertex(likedPerson.getId());

						//go through each movie they've worked on
						for (Vertex toRecMovie : orientLikedPerson.getVertices(Direction.OUT, rel)) {
							String imdbID = toRecMovie.getProperty("imdbID");

							//add the score to the map or increment the previous score
							if (recMap.containsKey(imdbID)) {
								recMap.put(imdbID, recMap.get(imdbID) + itemScore);
							} else {
								recMap.put(imdbID, itemScore);
							}
						}
					}
				}

				String likedIMDBID = likedMovie.getProperty("imdbID");
				if (!ignoredMovies.contains(likedIMDBID)) {
					ignoredMovies.add(likedIMDBID);
				}

			}

			Map<String, Integer> sortedRecMap = sortByValues(recMap);

			sortedRecMap
					.entrySet()
					.stream()    //get a stream
					.filter(
							//don't show any crappy recommendations or ones we've ignored
							entry -> entry.getValue() >= MIN_SCORE &&
									!ignoredMovies.contains(entry.getKey()))
					.limit(maxRecommendations)  //only show the first n recommendations
					.forEach(entry -> {
						//handle problems with concurrent version exceptions
						for (int retry = 0; retry < maxRetries; retry++) {
							try {
								//send the first n recommendations back to the database
								String imdbID = entry.getKey();
								int score = entry.getValue();

								Vertex movieToRecommend = graph.getVertexByKey("Movie.imdbID", imdbID);

								Edge e = graph.addEdge(null, movieToRecommend, userVertex, "recommended");
								e.setProperty("score", score);
								graph.commit();
								break;
							} catch (OTransactionException e) {
								//no need to take action, next iteration will reload variables
							}
						}
					});

			recommendationsUpdated = System.currentTimeMillis();
			userVertexVanilla.setProperty("recommendationsUpdated", recommendationsUpdated);

		} catch (Exception e) {
			graph.rollback();
			throw e;
		} finally {
			graph.shutdown();
		}

		return getRecommendations(user);
	}
}
