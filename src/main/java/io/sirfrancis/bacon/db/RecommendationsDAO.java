package io.sirfrancis.bacon.db;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.intent.OIntentMassiveRead;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import io.sirfrancis.bacon.core.Movie;
import io.sirfrancis.bacon.core.Recommendation;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.enums.*;

import java.util.*;

public class RecommendationsDAO {
	private MovieDAO movieDAO;

	public RecommendationsDAO() {
		this.movieDAO = new MovieDAO();
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
		OrientGraph graph = GraphConnection.getGraph();
		Map<String, Integer> recMap = new HashMap<>();
		Set<String> ignoredMovies = new HashSet<>();

		int MIN_SCORE = 100;

		try {
			graph.declareIntent(new OIntentMassiveRead());
			Vertex userVertex = graph.getVertexByKey(Indexes.USER_USERNAME, user.getUsername());

			long ratingsUpdated = userVertex.getProperty(UserProps.RAT_UPDATED);
			long recommendationsUpdated = userVertex.getProperty(UserProps.REC_UPDATED);

			if (recommendationsUpdated > ratingsUpdated) return getRecommendations(user);

			//clear previous recommendations
			for (Edge e : userVertex.getEdges(Direction.BOTH, Edges.RECOMMENDED)) {
				graph.removeEdge(e);
			}

			graph.commit();

			//MANY LOOPS
			for (Edge e : userVertex.getEdges(Direction.OUT, Edges.RATED)) {
				//all recommendations are built from this rating
				int topLevelScore = e.getProperty(Edges.RATING);

				//don't give score to unliked movies
				topLevelScore -= 4;

				//get all of the people associated with this rating
				Vertex likedMovie = e.getVertex(Direction.IN);

				//for each person that is rated implicitly by movie rating
				//String[] personRels = { Edges.ACTED, Edges.DIRECTED, Edges.WROTE };
				Set<String> people = new HashSet<>();

				//for (String rel : personRels) {
				Iterable<Vertex> peopleVertices = likedMovie.getVertices(Direction.IN, Edges.ACTED, Edges.DIRECTED, Edges.WROTE);

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
						String name = likedPerson.getProperty(PersonProps.NAME);
						if (people.contains(name)) {
							continue;
						} else {
							people.add(name);
						}

						//go through each movie they've worked on
						for (Vertex toRecMovie : likedPerson.getVertices(Direction.OUT, Edges.ACTED, Edges.DIRECTED, Edges.WROTE)) {
							String imdbID = toRecMovie.getProperty(MovieProps.IMDBID);

							//add the score to the map or increment the previous score
							if (recMap.containsKey(imdbID)) {
								recMap.put(imdbID, recMap.get(imdbID) + itemScore);
							} else {
								recMap.put(imdbID, itemScore);
							}
						}
					}
				//}

				String likedIMDBID = likedMovie.getProperty(MovieProps.IMDBID);
				if (!ignoredMovies.contains(likedIMDBID)) {
					ignoredMovies.add(likedIMDBID);
				}

			}

			Map<String, Integer> sortedRecMap = sortByValues(recMap);

			for (int i = 0; i < 10; i++) {
				try {
					sortedRecMap
							.entrySet()
							.stream()    //get a stream
							.filter(
									//don't show any crappy recommendations or ones we've ignored
									entry -> entry.getValue() >= MIN_SCORE &&
											!ignoredMovies.contains(entry.getKey()))
							.limit(maxRecommendations)  //only show the first n recommendations
							.forEach(entry -> {
								//send the first n recommendations back to the database
								String imdbID = entry.getKey();
								int score = entry.getValue();

								Vertex movieToRecommend = graph.getVertexByKey(Indexes.MOVIE_IMDBID, imdbID);

								Edge e = graph.addEdge(null, movieToRecommend, userVertex, Edges.RECOMMENDED);
								e.setProperty(Edges.SCORE, score);
							});


					recommendationsUpdated = System.currentTimeMillis();
					userVertex.setProperty(UserProps.REC_UPDATED, recommendationsUpdated);

					graph.commit();
					break;
				} catch (OConcurrentModificationException ocme) {
				}
			}

		} catch (Exception e) {
			throw e;
		}

		return getRecommendations(user);
	}
}
