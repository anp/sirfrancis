package io.sirfrancis.bacon.tasks;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMultimap;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import io.dropwizard.servlets.tasks.Task;
import io.sirfrancis.bacon.util.StringRandomizer;
import org.graphstream.algorithm.BetweennessCentrality;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;

import java.io.PrintWriter;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

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
		Graph gsGraph = new MultiGraph("Betweenness graph");
		OrientGraphNoTx graph = factory.getNoTx();

		List<CentralityRating> quizOrderList = new LinkedList<>();

		for (Vertex movieVertex : graph.getVerticesOfClass("Movie")) {
			String imdbID = movieVertex.getProperty("imdbID");
			Node movieNode = gsGraph.addNode(imdbID);

			for (Vertex personVertex : movieVertex.getVertices(
					Direction.BOTH, "Acted", "Directed", "Wrote")) {

				String name = personVertex.getProperty("name");

				Node personNode = gsGraph.getNode(name);
				if (personNode == null) {
					personNode = gsGraph.addNode(name);
				}

				gsGraph.addEdge(randomStrings.nextString(), movieNode, personNode);

			}
		}

		System.out.println("GraphStream DB initialized, calculating centrality.");

		BetweennessCentrality betweennessCentrality = new BetweennessCentrality();
		betweennessCentrality.init(gsGraph);
		betweennessCentrality.compute();

		System.out.println("Centrality calculated. Adding imdb id's to hashmap.");

		for (Node n : gsGraph.getEachNode()) {
			if (n.getId().startsWith("tt")) {
				double centrality = n.getNumber("Cb");
				quizOrderList.add(new CentralityRating(n.getId(), centrality));
			}

		}

		System.out.println("Sorting quiz order.");

		quizOrderList.sort(new Comparator<CentralityRating>() {
			@Override
			public int compare(CentralityRating first, CentralityRating second) {
				return (second.centrality >= first.centrality) ? 1 : -1;
			}
		});
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
