package io.sirfrancis.bacon.tasks;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMultimap;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import io.dropwizard.servlets.tasks.Task;

import java.io.PrintWriter;

/**
 * Created by adam on 1/26/15.
 */
public class DBWarmupTask extends Task {
	private OrientGraphFactory factory;

	public DBWarmupTask(OrientGraphFactory factory) {
		super("db-warmup");
		this.factory = factory;
	}

	@Override
	@Timed
	public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
		OrientGraph graph = factory.getTx();

		try {
			for (Vertex v : graph.getVertices()) {
				for (Edge e : v.getEdges(Direction.BOTH)) {
					for (String key : e.getPropertyKeys()) {
						e.getProperty(key);
					}
				}

				for (String key : v.getPropertyKeys()) {
					v.getProperty(key);
				}
			}
		} finally {
			graph.shutdown();
		}
	}
}
