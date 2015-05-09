package io.sirfrancis.bacon.resources;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import io.sirfrancis.bacon.core.Movie;
import io.sirfrancis.bacon.core.Rating;
import io.sirfrancis.bacon.core.Recommendation;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.RatingDAO;
import io.sirfrancis.bacon.db.RecommendationsDAO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by adam on 1/26/15.
 */

@Path("/recommendations/{numReturned}")
@Produces(MediaType.APPLICATION_JSON)
public class RecommendationsResource {
	private RecommendationsDAO recommendationsDAO;
	private RatingDAO ratingDAO;

	public RecommendationsResource(RecommendationsDAO recommendationsDAO, RatingDAO ratingDAO) {
		this.recommendationsDAO = recommendationsDAO;
		this.ratingDAO = ratingDAO;
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValues(Map<K, V> map) {
		List<Map.Entry<K, V>> entries = new LinkedList<>(map.entrySet());

		Collections.sort(entries, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

		Map<K, V> sortedMap = new LinkedHashMap<>();

		for (Map.Entry<K, V> entry : entries) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}

	@GET
	@Timed
	public List<Recommendation> getRecommendations(@PathParam("numReturned") int numRecommendations,
												   @Auth User user) {
		if (recommendationsDAO.needToUpdateRecommendations(user)) {
			int MIN_SCORE = 100;

			List<Rating> ratings = ratingDAO.getRatings(user);
			Map<Movie, Integer> newRecommendations = new HashMap<>();

			ratings.parallelStream().forEach(rating -> {
				Movie m = rating.getMovie();
				int topLevelScore = (rating.getRating() - 4) * 100;

				recommendationsDAO.getNeighborMoviesAndStrengths(m).entrySet().stream()
						.filter(e -> !ratings.contains(new Rating(e.getKey(), 0)))
						.forEach(e -> {
							Movie movie = e.getKey();
							int currentScore = (int) (e.getValue() * topLevelScore);

							synchronized (newRecommendations) {
								int oldScore = newRecommendations.getOrDefault(movie, 0);
								newRecommendations.put(movie, oldScore + currentScore);
							}
						});
			});

			Set<Recommendation> toWrite = newRecommendations.entrySet().stream()
					.filter(e -> e.getValue() >= MIN_SCORE)
					.map(e -> new Recommendation(e.getKey(), e.getValue()))
					.collect(Collectors.toSet());

			recommendationsDAO.writeRecommendations(user, toWrite);

		}

		return recommendationsDAO.getRecommendations(user, numRecommendations);
	}

}
