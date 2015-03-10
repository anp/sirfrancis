package io.sirfrancis.bacon.db;

import com.orientechnologies.orient.core.exception.OTransactionException;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import edu.uci.ics.jung.algorithms.scoring.BetweennessCentrality;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseMultigraph;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.core.Movie;
import io.sirfrancis.bacon.core.Rating;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.enums.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.*;

public class QuizDAO {
	private static Logger LOGGER = LoggerFactory.getLogger(QuizDAO.class);
	private MovieDAO movieDAO;
	private RatingDAO ratingDAO;
	private int maxRetries;

	public QuizDAO() {
		this.movieDAO = new MovieDAO();
		this.ratingDAO = new RatingDAO();
		maxRetries = BaconConfiguration.getMaxRetries();
	}

	private static <K, V extends Comparable<? super V>> List<K> reverseSortKeysByValue(Map<K, V> map) {
		Map<K, V> temp = new LinkedHashMap<>();

		map.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue))
				.forEach(e -> temp.put(e.getKey(), e.getValue()));

		List<K> result = new ArrayList<>();

		temp.entrySet().stream().forEach(e -> result.add(0, e.getKey()));

		return result;
	}

	public List<Movie> getQuizMovies(int perPage, int pageNumber, User user) {
		OrientGraph graph = GraphConnection.getGraph();
		List<Movie> quizItems = new LinkedList<>();

		HashSet<String> ratedIMDBIDs = new HashSet<>();

		ratingDAO.getRatings(user)
				.stream()
				.map(Rating::getMovie)
				.map(Movie::getImdbID)
				.forEach(ratedIMDBIDs::add);

		try {
			String baseQuery = "select from " + Vertices.MOVIE + " where " + Indexes.MOVIE_QUIZORDER + " > -1";

			Iterable<Vertex> results = graph.command(new OCommandSQL(baseQuery)).execute();

			for (Vertex v : results) {
				String id = v.getProperty(MovieProps.IMDBID);

				if (!ratedIMDBIDs.contains(id))
					quizItems.add(movieDAO.buildMovie(v));
			}

		} finally {
			graph.shutdown();
		}

		int startIndex = perPage * pageNumber;
		int endIndex = perPage * (pageNumber + 1);

		if (quizItems.size() >= endIndex) {
			quizItems = quizItems.subList(startIndex, endIndex);
		}

		return quizItems;
	}

	public void writeQuizPath() {
		List<String> quizOrderList = calculateCentralityRatings();
		OrientGraph graph = GraphConnection.getGraph();

		LOGGER.info("Adding order for quiz list to graph.");

		for (Vertex m : graph.getVerticesOfClass(Vertices.MOVIE)) {
			for (int i = 0; i < maxRetries; i++) {
				try {
					m.setProperty(MovieProps.QUIZ_ORDER, -1);

					graph.commit();
					break;
				} catch (OTransactionException ote) {
				}
			}
		}

		//Vertex quizVertex = graph.getVerticesOfClass(Vertices.QUIZSTART).iterator().next();
		//if (quizVertex == null) graph.addVertex("class:" + Vertices.QUIZSTART);

		int currQuizPos = 0;
		for (String id : quizOrderList) {
			for (int i = 0; i < maxRetries; i++) {
				try {
					Vertex nextVertex = graph.getVertexByKey(Indexes.MOVIE_IMDBID, id);
					nextVertex.setProperty(MovieProps.QUIZ_ORDER, currQuizPos);
					graph.commit();
					currQuizPos++;
					break;
				} catch (OTransactionException ote2) {
				}
			}
		}
		graph.shutdown();
	}

	private List<String> calculateCentralityRatings() {

		Graph<String, Integer> jGraph = buildJungGraph();
		LOGGER.info("In-memory JUNG graph loaded for betweenness calculations.");
		Map<String, Double> betweenRankings = new LinkedHashMap<>();

		BetweennessCentrality<String, Integer> betweennessCentrality = new BetweennessCentrality<>(jGraph);

		for (String v : jGraph.getVertices()) {
			if (v.startsWith("tt")) {
				betweenRankings.put(v, betweennessCentrality.getVertexScore(v));
			}
		}
		LOGGER.info("Done with betweenness calculations.");

		/*jGraph = buildJungGraph();
		LOGGER.info("In-memory JUNG graph loaded for closeness calculations.");
		Map<String, Double> closenessRankings = new LinkedHashMap<>();

		ClosenessCentrality<String, Integer> closenessCentrality = new ClosenessCentrality<>(jGraph);

		for (String v : jGraph.getVertices()) {
			if (v.startsWith("tt")) {
				//closenessRankings.put(v, closenessCentrality.getVertexScore(v));
			}
		}
		LOGGER.info("Done with closeness calculations.");*/

		List<String> quizOrderList = new LinkedList<>();

		//Iterator<String> closenessIterator = reverseSortKeysByValue(closenessRankings).iterator();

		//|| closenessIterator.hasNext()) {
/*try {
	String nextCloseness = closenessIterator.next();

	if (!quizOrderList.contains(nextCloseness))
		quizOrderList.add(nextCloseness);
} catch (NoSuchElementException nsee) {
	//do nothing, we don't care
}*/
		reverseSortKeysByValue(betweenRankings).stream().filter(
				s -> !quizOrderList.contains(s)).forEach(quizOrderList::add);

		return quizOrderList;
	}


	public Graph<String, Integer> buildJungGraph() {
		Graph<String, Integer> jGraph = new UndirectedSparseMultigraph<>();
		OrientGraph graph = GraphConnection.getGraph();
		SecureRandom random = new SecureRandom();

		int numVertices = 0;
		for (Vertex movieVertex : graph.getVerticesOfClass(Vertices.MOVIE)) {
			String imdbID = movieVertex.getProperty(MovieProps.IMDBID);
			double tomatoMeter = movieVertex.getProperty(MovieProps.RTRATING);

			if (tomatoMeter > 0) {
				numVertices++;
				jGraph.addVertex(imdbID);

				for (Vertex personVertex : movieVertex.getVertices(
						Direction.BOTH, Edges.ACTED, Edges.DIRECTED, Edges.WROTE)) {

					String name = personVertex.getProperty(PersonProps.NAME);

					if (!jGraph.containsVertex(name)) jGraph.addVertex(name);

					int randomEdge = random.nextInt();
					while (jGraph.containsEdge(randomEdge)) randomEdge = random.nextInt();

					jGraph.addEdge(randomEdge, imdbID, name);

				}
			}
		}
		LOGGER.info("Creating JUNG graph with " + numVertices + " movies parsed.");

		return jGraph;
	}


}
