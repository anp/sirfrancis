package io.sirfrancis.bacon.tasks;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMultimap;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseMultigraph;
import io.dropwizard.servlets.tasks.Task;
import io.sirfrancis.bacon.util.StringRandomizer;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by adam on 2/4/15.
 */
public class CreateQuizPathTask extends Task{
	OrientGraphFactory factory;
	StringRandomizer randomStrings = new StringRandomizer(25);

	public CreateQuizPathTask(OrientGraphFactory factory) {
		super("create-quiz-path");
		this.factory = factory;
	}


	@Override
	@Timed
	public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
		OrientGraph graph = factory.getTx();

		try {
			List<CentralityRating> quizOrderList = calculateCentralityRatings();

			System.out.println("Adding quiz path edges.");

			for (Edge e : graph.getEdgesOfClass("quizPath")) {
				graph.removeEdge(e);
			}

			Vertex quizVertex = graph.getVertexByKey("quizStart.identifier", "quiz starting point");
			for (CentralityRating rating : quizOrderList) {
				Vertex nextVertex = graph.getVertexByKey("Movie.imdbID", rating.imdbID);
				nextVertex.addEdge("quizPath", quizVertex);
				quizVertex = nextVertex;
			}

		} finally {
			graph.shutdown();
		}
	}

	private List<CentralityRating> calculateCentralityRatings() {
		Graph<String, String> jGraph = new UndirectedSparseMultigraph<>();
		OrientGraphNoTx graph = factory.getNoTx();

		List<CentralityRating> quizOrderList = new LinkedList<>();

		for (Vertex movieVertex : graph.getVerticesOfClass("Movie")) {
			String imdbID = movieVertex.getProperty("imdbID");

			jGraph.addVertex(imdbID);

			for (Vertex personVertex : movieVertex.getVertices(
					Direction.BOTH, "Acted", "Directed", "Wrote")) {

				String name = personVertex.getProperty("name");

				jGraph.addVertex(name);
				jGraph.addEdge(randomStrings.nextString(), imdbID, name);

			}
		}

		System.out.println("GraphStream DB initialized, calculating centrality.");

		BetweennessCentrality<String, String> betweennessCentrality = new BetweennessCentrality<>(jGraph, true, false);
		betweennessCentrality.step();
		System.out.println("Centrality calculated. Adding imdb id's to hashmap.");

		betweennessCentrality.printRankings(false, true);

		Map<String, Number> rankings =
				betweennessCentrality.getVertexRankScores(betweennessCentrality.getRankScoreKey());

		for (Map.Entry<String, Number> ranking : rankings.entrySet()) {
			String id = ranking.getKey();
			if (id.startsWith("tt")) {
				quizOrderList.add(new CentralityRating(id, ranking.getValue().doubleValue()));
			}
		}

		return quizOrderList;
	}

	private class CentralityRating {
		public String imdbID;
		public double centrality;

		public CentralityRating(String imdbID, double centrality) {
			this.imdbID = imdbID;
			this.centrality = centrality;
		}
	}
}
