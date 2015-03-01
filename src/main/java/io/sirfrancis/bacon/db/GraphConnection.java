package io.sirfrancis.bacon.db;

import com.orientechnologies.orient.client.db.ODatabaseHelper;
import com.orientechnologies.orient.core.exception.OSchemaException;
import com.orientechnologies.orient.core.index.OIndexException;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;
import io.dropwizard.lifecycle.Managed;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.db.enums.*;
import org.slf4j.Logger;

import java.io.File;

public class GraphConnection implements Managed {
	private static OrientGraphFactory factory;
	private static boolean started = false;
	private static Logger LOGGER = org.slf4j.LoggerFactory.getLogger(GraphConnection.class);

	public static OrientGraph getGraph() {
		return factory.getTx();
	}

	public static boolean started() {
		return factory.exists() && started;
	}

	private static void initSchemaTypes() {
		OrientGraphNoTx graph = factory.getNoTx();

		try {
			graph.createVertexType(Vertices.MOVIE);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie class (may already exist).");
		}
		OrientVertexType movieType = graph.getVertexType(Vertices.MOVIE);

		try {
			graph.createVertexType(Vertices.PERSON);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Person class (may already exist).");
		}
		OrientVertexType personType = graph.getVertexType(Vertices.PERSON);

		try {
			graph.createVertexType(Vertices.USER);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create User class (may already exist).");
		}
		OrientVertexType userType = graph.getVertexType(Vertices.USER);

		try {
			graph.createVertexType(Vertices.QUIZSTART);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create QuizStart class (may already exist).");
		}

		try {
			movieType.createProperty(MovieProps.TITLE, OType.STRING);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.title property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.INDEXTITLE, OType.STRING);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.indexTitle property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.IMDBID, OType.STRING);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.imdbid property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.OMDBID, OType.LONG);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.omdbid property (may already exist).");
		}

		try {
			movieType.createIndex(Indexes.MOVIE_IMDBID, "UNIQUE", MovieProps.IMDBID);
		} catch (OIndexException ose) {
			LOGGER.info("Unable to create Movie.imdbid index (may already exist).");
		}
		try {
			movieType.createIndex(Indexes.MOVIE_OMDBID, "UNIQUE", MovieProps.OMDBID);
		} catch (OIndexException ose) {
			LOGGER.info("Unable to create Movie.omdbid index (may already exist).");
		}

		try {
			movieType.createProperty(MovieProps.RUNTIME, OType.STRING);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.runtime property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.RELEASED, OType.STRING);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.released property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.LANGUAGE, OType.STRING);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.language property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.COUNTRY, OType.STRING);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.country property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.AWARDS, OType.STRING);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.awards property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.MPAARATING, OType.STRING);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.mpaaRating property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.METASCORE, OType.INTEGER);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.metascore property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.IMDBRATING, OType.DOUBLE);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.imdbRating property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.IMDBVOTES, OType.INTEGER);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.imdbVotes property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.GENRES, OType.STRING);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.genres property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.RTRATING, OType.DOUBLE);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.rtRating property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.RTMETER, OType.INTEGER);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.tomatoMeter property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.RTNREVIEWS, OType.INTEGER);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.rtNumReviews property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.RTNFRESHREVIEWS, OType.INTEGER);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.rtNumFreshReviews property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.RTNROTTENREVIEWS, OType.INTEGER);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.rtNumRottenReviews property (may already exist).");
		}
		try {
			movieType.createProperty(MovieProps.RTCONSENSUS, OType.STRING);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Movie.rtConsensus property (may already exist).");
		}


		try {
			personType.createProperty(PersonProps.NAME, OType.STRING);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Person.name property (may already exist).");
		}
		try {
			personType.createIndex(Indexes.PERSON_NAME, "UNIQUE", PersonProps.NAME);
		} catch (OIndexException ose) {
			LOGGER.info("Unable to create Person.name index (may already exist).");
		}


		try {
			userType.createProperty(UserProps.USERNAME, OType.STRING);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create User.username property (may already exist).");
		}
		try {
			userType.createIndex(Indexes.USER_USERNAME, "UNIQUE", UserProps.USERNAME);
		} catch (OIndexException ose) {
			LOGGER.info("Unable to create User.username index (may already exist).");
		}

		try {
			graph.createEdgeType(Edges.ACTED);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Acted class (may already exist).");
		}
		try {
			graph.createEdgeType(Edges.DIRECTED);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Directed class (may already exist).");
		}
		try {
			graph.createEdgeType(Edges.WROTE);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Wrote class (may already exist).");
		}
		try {
			graph.createEdgeType(Edges.RATED);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Rated class (may already exist).");
		}
		try {
			graph.createEdgeType(Edges.RECOMMENDED);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Recommended class (may already exist).");
		}
		try {
			graph.createEdgeType(Edges.QUIZPATH);
		} catch (OSchemaException ose) {
			LOGGER.info("Unable to create Quizpath class (may already exist).");
		}

		try {
			String indexCreationQuery = "CREATE INDEX "
					+ Indexes.MOVIE_INDEXTITLE + " ON " + Vertices.MOVIE
					+ " (" + MovieProps.INDEXTITLE + ") FULLTEXT ENGINE LUCENE";

			graph.command(new OCommandSQL(indexCreationQuery)).execute();
		} catch (OIndexException ose) {
			LOGGER.info("Unable to create Lucene full text index (may already exist).");
		}

		graph.shutdown();
	}

	public void start() throws Exception {
		File dbFile = new File(BaconConfiguration.getOrientConnectionString());

		factory = new OrientGraphFactory("plocal:" + dbFile.getAbsolutePath());

		if (!factory.exists()) {
			ODatabaseHelper.createDatabase(factory.getDatabase(), "plocal:" + dbFile.getAbsolutePath(), "graph");
		}

		factory.setupPool(BaconConfiguration.getDbPoolMin(), BaconConfiguration.getDbPoolMax());

		started = true;

		LOGGER.info("OrientDB Graph connection pool started at " + dbFile.getAbsolutePath() + ".");

		initSchemaTypes();
	}

	public void stop() throws Exception {
		factory.close();
	}
}
