package io.sirfrancis.bacon.db;

import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import io.sirfrancis.bacon.core.Movie;
import io.sirfrancis.bacon.core.Rating;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.enums.MovieProps;
import io.sirfrancis.bacon.db.enums.Vertices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class QuizDAO {
	private static Logger LOGGER = LoggerFactory.getLogger(QuizDAO.class);
	private static List<Movie> quizItems;
	private MovieDAO movieDAO;
	private RatingDAO ratingDAO;

	public QuizDAO() {
		this.movieDAO = new MovieDAO();
		this.ratingDAO = new RatingDAO();
	}

	public List<Movie> getQuizMovies(int perPage, int pageNumber, User user) {

		HashSet<String> ratedIMDBIDs = new HashSet<>();

		ratingDAO.getRatings(user)
				.stream()
				.map(Rating::getMovie)
				.map(Movie::getImdbID)
				.forEach(ratedIMDBIDs::add);

		int startIndex = perPage * pageNumber;
		int endIndex = perPage * (pageNumber + 1);

		List<Movie> userQuizItems = new ArrayList<>();
		quizItems.stream()
				.filter(m -> !ratedIMDBIDs.contains(m.getImdbID()))
				.forEach(userQuizItems::add);

		if (userQuizItems.size() >= endIndex) {
			return userQuizItems.subList(startIndex, endIndex);
		} else if (userQuizItems.size() >= startIndex) {
			return userQuizItems.subList(startIndex, userQuizItems.size() - 1);
		} else {
			return new ArrayList<>();
		}

	}

	public void initQuiz() {
		OrientGraph graph = GraphConnection.getGraph();
		quizItems = new ArrayList<>();
		String baseQuery = "select from " + Vertices.MOVIE + " where " +
				MovieProps.RTMETER + " <> 0 AND " + MovieProps.RTNREVIEWS + " > 30";

		Iterable<Vertex> results = graph.command(new OCommandSQL(baseQuery)).execute();

		for (Vertex v : results) {
			quizItems.add(movieDAO.buildMovie(v));
		}

		quizItems = quizItems.stream()
				.sorted(Comparator.comparingInt(Movie::getRottenTomatoMeter))
				.collect(Collectors.toCollection(ArrayList::new));

		Collections.reverse(quizItems);
	}
}
