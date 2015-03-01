package io.sirfrancis.bacon.db;

import com.orientechnologies.orient.core.exception.OTransactionException;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import edu.uci.ics.jung.algorithms.scoring.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.scoring.ClosenessCentrality;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseMultigraph;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.core.Movie;
import io.sirfrancis.bacon.db.enums.Edges;
import io.sirfrancis.bacon.db.enums.MovieProps;
import io.sirfrancis.bacon.db.enums.Vertices;
import io.sirfrancis.bacon.util.StringRandomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class QuizDAO {
	private static Logger LOGGER = LoggerFactory.getLogger(QuizDAO.class);
	private MovieDAO movieDAO;
	private int maxRetries;
	private StringRandomizer randomStrings = new StringRandomizer(25);
	private JungGraphFactory jFactory;

	public QuizDAO() {
		this.movieDAO = new MovieDAO();
		maxRetries = BaconConfiguration.getMaxRetries();
		jFactory = new JungGraphFactory();
	}

	private static <K, V extends Comparable<? super V>> List<K> reverseSortKeysByValue(Map<K, V> map) {
		Map<K, V> temp = new LinkedHashMap<>();

		map.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue))
				.forEach(e -> temp.put(e.getKey(), e.getValue()));

		List<K> result = new ArrayList<>();

		temp.entrySet().stream().forEach(e -> result.add(0, e.getKey()));

		return result;
	}

	public List<Movie> getQuizMovies(int perPage, int pageNumber) {
		OrientGraph graph = GraphConnection.getGraph();
		List<Movie> quizItems = new LinkedList<>();

		Vertex quizVertex = graph.getVerticesOfClass(Vertices.QUIZSTART).iterator().next();

		int endIndex = perPage * pageNumber;
		int startIndex = endIndex - perPage;

		for (int i = 0; i < endIndex; i++) {

			if (i >= startIndex) {
				Vertex nextVertex = null;
				nextVertex = quizVertex.getVertices(Direction.BOTH, Vertices.MOVIE).iterator().next();

				if (nextVertex != null) {
					quizItems.add(movieDAO.buildMovie(nextVertex));
				} else {
					break;
				}
			}
		}

		graph.shutdown();
		return quizItems;
	}

	public void writeQuizPath() {
		Set<String> quizOrderList = calculateCentralityRatings();
		OrientGraph graph = GraphConnection.getGraph();

		LOGGER.info("Adding edges for quiz path to graph.");

		for (int i = 0; i < maxRetries; i++) {
			try {
				for (Edge e : graph.getEdgesOfClass(Edges.QUIZPATH)) {
					graph.removeEdge(e);
				}
				graph.commit();
				break;
			} catch (OTransactionException ote) {
				graph.rollback();
			}
		}

		for (int i = 0; i < maxRetries; i++) {
			try {
				Vertex quizVertex = graph.getVertexByKey(Vertices.QUIZSTART + "." + Vertices.QUIZSTART, Vertices.QUIZSTART);
				if (quizVertex == null) graph.addVertex("class:" + Vertices.QUIZSTART);

				for (String id : quizOrderList) {
					Vertex nextVertex = graph.getVertexByKey(Vertices.MOVIE + "." + MovieProps.IMDBID, id);
					nextVertex.addEdge(Edges.QUIZPATH, quizVertex);
					quizVertex = nextVertex;
				}
				graph.commit();
				break;
			} catch (OTransactionException ote2) {
				graph.rollback();
			}
		}

		graph.shutdown();
	}

	private Set<String> calculateCentralityRatings() {

		Callable<List<String>> betweenCallable = () -> {
			Graph<String, String> jGraph = jFactory.buildJungGraph();

			Map<String, Double> betweenRankings = new LinkedHashMap<>();

			BetweennessCentrality<String, String> betweennessCentrality = new BetweennessCentrality<>(jGraph);

			int verticesProcessed = 0;
			for (String v : jGraph.getVertices()) {
				if (v.startsWith("tt")) {
					betweenRankings.put(v, betweennessCentrality.getVertexScore(v));
					verticesProcessed++;

					LOGGER.info("Betweenness centrality calculated for "
							+ verticesProcessed + " vertices.");
				}
			}

			return reverseSortKeysByValue(betweenRankings);
		};

		Callable<List<String>> closenessCallable = () -> {
			Graph<String, String> jGraph = jFactory.buildJungGraph();

			ClosenessCentrality<String, String> closenessCentrality = new ClosenessCentrality<>(jGraph);

			Map<String, Double> closenessRankings = new LinkedHashMap<>();

			int verticesProcessed = 0;
			for (String v : jGraph.getVertices()) {
				if (v.startsWith("tt")) {
					closenessRankings.put(v, closenessCentrality.getVertexScore(v));

					verticesProcessed++;

					LOGGER.info("Closeness centrality calculated for "
							+ verticesProcessed + " vertices.");
				}
			}

			return reverseSortKeysByValue(closenessRankings);
		};

		Set<String> quizOrderList = new LinkedHashSet<>();
		try {

			FutureTask<List<String>> betweenTask = new FutureTask<>(betweenCallable);
			FutureTask<List<String>> closenessTask = new FutureTask<>(closenessCallable);

			betweenTask.run();
			closenessTask.run();

			Iterator<String> betweenIterator = betweenTask.get().iterator();
			Iterator<String> closenessIterator = closenessTask.get().iterator();

			while (betweenIterator.hasNext() || closenessIterator.hasNext()) {
				try {
					String nextBetween = betweenIterator.next();
					quizOrderList.add(nextBetween);
				} catch (NoSuchElementException nsee) {
					//do nothing, we don't care
				}

				try {
					String nextCloseness = closenessIterator.next();
					quizOrderList.add(nextCloseness);
				} catch (NoSuchElementException nsee) {
					//do nothing, we don't care
				}

			}

		} catch (InterruptedException ie) {
			LOGGER.error("FutureTask interrupted", ie);
		} catch (ExecutionException ee) {
			LOGGER.error("Threaded execution problem", ee);
		}
		return quizOrderList;
	}

	private class JungGraphFactory {
		public Graph<String, String> buildJungGraph() {
			Graph<String, String> jGraph = new UndirectedSparseMultigraph<>();
			OrientGraph graph = GraphConnection.getGraph();

			for (Vertex movieVertex : graph.getVertices("vertexType", "Movie")) {
				String imdbID = movieVertex.getProperty("imdbID");

				jGraph.addVertex(imdbID);

				for (Vertex personVertex : movieVertex.getVertices(
						Direction.BOTH, "Acted", "Directed", "Wrote")) {

					String name = personVertex.getProperty("name");

					jGraph.addVertex(name);
					jGraph.addEdge(randomStrings.nextString(), imdbID, name);

				}
			}

			return jGraph;
		}
	}
}
