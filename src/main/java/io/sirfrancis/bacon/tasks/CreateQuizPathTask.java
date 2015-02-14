package io.sirfrancis.bacon.tasks;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMultimap;
import com.sendgrid.SendGrid;
import com.sendgrid.SendGridException;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import edu.uci.ics.jung.algorithms.scoring.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.scoring.ClosenessCentrality;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseMultigraph;
import io.dropwizard.servlets.tasks.Task;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.util.StringRandomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class CreateQuizPathTask extends Task {
	private static final Logger LOGGER = LoggerFactory.getLogger(CreateQuizPathTask.class);
	OrientGraphFactory factory;
	JungGraphFactory jFactory;
	StringRandomizer randomStrings = new StringRandomizer(25);
	SendGrid mailer;

	public CreateQuizPathTask(OrientGraphFactory factory) {
		super("create-quiz-path");
		this.factory = factory;
		this.jFactory = new JungGraphFactory(factory);
		mailer = new SendGrid(BaconConfiguration.getSendgridUsername(), BaconConfiguration.getSendgridPassword());
	}


	@Override
	@Timed
	public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
		OrientGraph graph = factory.getTx();
		LOGGER.info("Beginning calculations for quiz creation.");
		try {
			Set<String> quizOrderList = calculateCentralityRatings();

			LOGGER.info("Finished calculating centralities. Adding edges for quiz path to graph.");

			for (Edge e : graph.getEdgesOfClass("quizPath")) {
				graph.removeEdge(e);
			}

			Vertex quizVertex = graph.getVertexByKey("quizStart.identifier", "quiz starting point");
			for (String id : quizOrderList) {
				Vertex nextVertex = graph.getVertexByKey("Movie.imdbID", id);
				nextVertex.addEdge("quizPath", quizVertex);
				quizVertex = nextVertex;
			}

			SendGrid.Email completionEmail = new SendGrid.Email();
			completionEmail.setFrom("admin@sirfrancis.io");
			completionEmail.addTo("adam.n.perry@gmail.com");
			completionEmail.setSubject("Quiz Path Creation Complete");
			completionEmail.setText("Quiz Path Created Successfully. Try it out.");

			LOGGER.info("Finished calculating and storing quiz path.");
		} finally {
			graph.shutdown();

		}
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

					if (verticesProcessed % 10000 == 0) {
						LOGGER.info("Betweenness centrality calculated for "
								+ verticesProcessed + " vertices.");
					}

					if (verticesProcessed % 100000 == 0) {
						SendGrid.Email progressEmail = new SendGrid.Email();
						progressEmail.setFrom("admin@sirfrancis.io");
						progressEmail.addTo("adam.n.perry@gmail.com");
						progressEmail.setSubject("Betweenness centrality progress: "
								+ verticesProcessed + " vertices processed.");
						try {
							mailer.send(progressEmail);
						} catch (SendGridException sge) {
							LOGGER.error("Problem sending Betweenness Centrality progress email.", sge);
						}
					}
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

					if (verticesProcessed % 10000 == 0) {
						LOGGER.info("Closeness centrality calculated for "
								+ verticesProcessed + " vertices.");
					}

					if (verticesProcessed % 100000 == 0) {
						SendGrid.Email progressEmail = new SendGrid.Email();
						progressEmail.setFrom("admin@sirfrancis.io");
						progressEmail.addTo("adam.n.perry@gmail.com");
						progressEmail.setSubject("Closeness centrality progress: "
								+ verticesProcessed + " vertices processed.");
						try {
							mailer.send(progressEmail);
						} catch (SendGridException sge) {
							LOGGER.error("Problem sending Closeness Centrality progress email.", sge);
						}
					}
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
		} finally {
			return quizOrderList;
		}
	}

	private static <K, V extends Comparable<? super V>> List<K> reverseSortKeysByValue(Map<K, V> map) {
		Map<K, V> temp = new LinkedHashMap<>();

		map.entrySet().stream().sorted(Comparator.comparing(e -> e.getValue()))
				.forEach(e -> temp.put(e.getKey(), e.getValue()));

		List<K> result = new ArrayList<>();

		temp.entrySet().stream().forEach(e -> result.add(0, e.getKey()));

		return result;
	}

	private class JungGraphFactory {
		OrientGraphFactory oFactory;

		public JungGraphFactory(OrientGraphFactory oFactory) {
			this.oFactory = oFactory;
		}

		public Graph<String, String> buildJungGraph() {
			OrientGraph oGraph = oFactory.getTx();
			Graph<String, String> jGraph = new UndirectedSparseMultigraph<>();
			try {
				for (Vertex movieVertex : oGraph.getVerticesOfClass("Movie")) {
					String imdbID = movieVertex.getProperty("imdbID");

					jGraph.addVertex(imdbID);

					for (Vertex personVertex : movieVertex.getVertices(
							Direction.BOTH, "Acted", "Directed", "Wrote")) {

						String name = personVertex.getProperty("name");

						jGraph.addVertex(name);
						jGraph.addEdge(randomStrings.nextString(), imdbID, name);

					}
				}
			} finally {
				oGraph.shutdown();
			}
			return jGraph;
		}
	}
}
