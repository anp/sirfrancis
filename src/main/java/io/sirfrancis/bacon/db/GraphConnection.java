package io.sirfrancis.bacon.db;

import com.orientechnologies.orient.client.db.ODatabaseHelper;
import com.orientechnologies.orient.core.index.OIndexException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.impls.orient.*;
import io.dropwizard.lifecycle.Managed;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.db.enums.*;
import org.slf4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GraphConnection implements Managed {
	private static OrientGraphFactory factory;
	private static boolean started = false;
	private static Logger LOGGER = org.slf4j.LoggerFactory.getLogger(GraphConnection.class);

	public static OrientGraph getGraph() {
		while (factory == null) {
		}
		return factory.getTx();
	}

	public static boolean started() {
		return factory.exists() && started;
	}

	public static void initSchemaTypes() {
		OrientGraphNoTx graph = factory.getNoTx();

		OrientVertexType movieType = graph.getVertexType(Vertices.MOVIE);
		if (movieType == null) {
			movieType = graph.createVertexType(Vertices.MOVIE);
		}

		Map<String, OType> movieProps = new HashMap<>();
		movieProps.put(MovieProps.TITLE, OType.STRING);
		movieProps.put(MovieProps.INDEXTITLE, OType.STRING);
		movieProps.put(MovieProps.IMDBID, OType.STRING);
		movieProps.put(MovieProps.OMDBID, OType.LONG);
		movieProps.put(MovieProps.RUNTIME, OType.STRING);
		movieProps.put(MovieProps.RELEASED, OType.STRING);
		movieProps.put(MovieProps.LANGUAGE, OType.STRING);
		movieProps.put(MovieProps.COUNTRY, OType.STRING);
		movieProps.put(MovieProps.AWARDS, OType.STRING);
		movieProps.put(MovieProps.MPAARATING, OType.STRING);
		movieProps.put(MovieProps.METASCORE, OType.INTEGER);
		movieProps.put(MovieProps.IMDBRATING, OType.DOUBLE);
		movieProps.put(MovieProps.IMDBVOTES, OType.INTEGER);
		movieProps.put(MovieProps.GENRES, OType.STRING);
		movieProps.put(MovieProps.RTRATING, OType.DOUBLE);
		movieProps.put(MovieProps.RTMETER, OType.INTEGER);
		movieProps.put(MovieProps.RTNREVIEWS, OType.INTEGER);
		movieProps.put(MovieProps.RTNFRESHREVIEWS, OType.INTEGER);
		movieProps.put(MovieProps.RTNROTTENREVIEWS, OType.INTEGER);
		movieProps.put(MovieProps.RTCONSENSUS, OType.STRING);

		for (Map.Entry<String, OType> entry : movieProps.entrySet()) {
			if (movieType.getProperty(entry.getKey()) == null)
				movieType.createProperty(entry.getKey(), entry.getValue());
		}

		OrientVertexType personType = graph.getVertexType(Vertices.PERSON);
		if (personType == null) {
			personType = graph.createVertexType(Vertices.PERSON);
		}
		Map<String, OType> personProps = new HashMap<>();
		personProps.put(PersonProps.NAME, OType.STRING);

		for (Map.Entry<String, OType> entry : personProps.entrySet()) {
			if (personType.getProperty(entry.getKey()) == null)
				personType.createProperty(entry.getKey(), entry.getValue());
		}


		OrientVertexType userType = graph.getVertexType(Vertices.USER);
		if (userType == null) {
			userType = graph.createVertexType(Vertices.USER);
		}

		Map<String, OType> userProps = new HashMap<>();
		userProps.put(UserProps.USERNAME, OType.STRING);

		for (Map.Entry<String, OType> entry : userProps.entrySet()) {
			if (userType.getProperty(entry.getKey()) == null)
				userType.createProperty(entry.getKey(), entry.getValue());
		}


		OrientVertexType quiztype = graph.getVertexType(Vertices.QUIZSTART);
		if (quiztype == null) {
			graph.createVertexType(Vertices.QUIZSTART);
		}

		OrientEdgeType actedEdge = graph.getEdgeType(Edges.ACTED);
		if (actedEdge == null)
			graph.createEdgeType(Edges.ACTED);

		OrientEdgeType directedEdge = graph.getEdgeType(Edges.DIRECTED);
		if (directedEdge == null)
			graph.createEdgeType(Edges.DIRECTED);

		OrientEdgeType wroteEdge = graph.getEdgeType(Edges.WROTE);
		if (wroteEdge == null)
			graph.createEdgeType(Edges.WROTE);

		OrientEdgeType ratedEdge = graph.getEdgeType(Edges.RATED);
		if (ratedEdge == null)
			graph.createEdgeType(Edges.RATED);

		OrientEdgeType recommendedEdge = graph.getEdgeType(Edges.RECOMMENDED);
		if (recommendedEdge == null)
			graph.createEdgeType(Edges.RECOMMENDED);


		/*OrientEdgeType quizPathEdge = graph.getEdgeType(Edges.QUIZPATH);
		if (quizPathEdge == null)
			graph.createEdgeType(Edges.QUIZPATH);

		boolean hasQuizStart = false;
		for (Vertex v : graph.getVerticesOfClass(Vertices.QUIZSTART)) {
			hasQuizStart = true;
		}
		if (!hasQuizStart) {
			graph.addVertex("class:"+Vertices.QUIZSTART);
		}*/

		try {
			movieType.createIndex(Indexes.MOVIE_IMDBID, OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, MovieProps.IMDBID);
		} catch (OIndexException oie) {
		}

		try {
			movieType.createIndex(Indexes.MOVIE_OMDBID, OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, MovieProps.OMDBID);
		} catch (OIndexException oie) {
		}

		try {
			movieType.createIndex(Indexes.MOVIE_QUIZORDER, OClass.INDEX_TYPE.NOTUNIQUE, MovieProps.QUIZ_ORDER);
		} catch (OIndexException oie) {
		}

		try {
			personType.createIndex(Indexes.PERSON_NAME, OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, PersonProps.NAME);
		} catch (OIndexException oie) {
		}

		try {
			userType.createIndex(Indexes.USER_USERNAME, OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, UserProps.USERNAME);
		} catch (OIndexException oie) {
		}


		try {
			String indexCreationQuery = "CREATE INDEX " + Indexes.MOVIE_INDEXTITLE
					+ " ON " + Vertices.MOVIE + " (" + MovieProps.INDEXTITLE + ") FULLTEXT ENGINE LUCENE";
			graph.command(new OCommandSQL(indexCreationQuery)).execute();
		} catch (Exception oie) {
			LOGGER.info("Fulltext index already exists.");
		}

		graph.shutdown();
	}

	public void start() throws Exception {
		File dbFile = new File(BaconConfiguration.getOrientConnectionString());

		factory = new OrientGraphFactory("plocal:" + dbFile.getAbsolutePath());

		for (int i = 0; i < 3; i++) {
			try {
				if (!factory.exists()) {
					ODatabaseHelper.createDatabase(factory.getDatabase(), "plocal:" + dbFile.getAbsolutePath(), "graph");
					break;
				}
			} catch (Exception e) {
			}
		}
		factory.setupPool(BaconConfiguration.getDbPoolMin(), BaconConfiguration.getDbPoolMax());

		started = true;

		LOGGER.info("OrientDB Graph connection pool started at " + dbFile.getAbsolutePath() + ".");

		initSchemaTypes();

		new QuizDAO().initQuiz();
	}

	public void stop() throws Exception {
		factory.close();
	}
}
