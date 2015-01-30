package io.sirfrancis.bacon.cli;

import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.*;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import io.sirfrancis.bacon.BaconConfiguration;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.StreamSupport;

public class DBInitCommand extends ConfiguredCommand<BaconConfiguration> {
	private static final Logger LOGGER = LoggerFactory.getLogger(DBInitCommand.class);
	private static final String[] IGNORED_GENRES_ARRAY =
			{ "Short", "Talk-Show", "Reality-TV", "Game-Show", "Adult", "News" };
	private static final HashSet<String> IGNORED_GENRES = new HashSet<>();
	private static BaconConfiguration config;
	private static int numMovies = 0;

	private static String DB_PATH;
	private static String BACKUP_PATH;
	private static String ADMIN_USER;
	private static String ADMIN_PASS;

	private static OrientGraph graph;

	private static String currentInitTimestamp;

	private static String OMDB_IMG_API_KEY;


	public DBInitCommand() {
		super("db", "Initialize the database with an omdb.txt file");
		IGNORED_GENRES.addAll(Arrays.asList(IGNORED_GENRES_ARRAY));
	}

	private static void parseMovieToDB(String line) {
		String[] fields = line.split("\\t");
		if (fields.length == 21 || (fields.length == 22 && fields[21].equalsIgnoreCase("movie"))) {
			String omdbID = fields[0];
			String imdbID = fields[1];
			String title = fields[2];
			String indexTitle = QueryParserUtil.escape(fields[2]).toLowerCase();
			String runtime = fields[5];
			String released = fields[7];
			String language = fields[17];
			String country = fields[18];
			String yearStr = fields[3];
			String awards = fields[19];
			String mpaaRating = fields[4];
			String metascoreStr = fields[11];
			String imdbRatingStr = fields[12];
			String imdbVotesStr = fields[13].replaceAll("[^0-9]+", "");
			String posterURL = "http://img.omdbapi.com/?i=" + imdbID + "&apikey=" + OMDB_IMG_API_KEY;

			String genres = fields[6];
			for (String g : genres.split(", ")) {
				if (IGNORED_GENRES.contains(g)) {
					return;
				}
			}

			String[] currentDirectors = fields[8].split(", ");
			String[] currentWriters = fields[9].split(", ");
			String[] currentCast = fields[10].split(", ");

			HashMap<String, Object> props = new HashMap<>();

			long id = 0;
			try {
				id = Integer.parseInt(omdbID);
			} catch (Exception e) {
				System.err.println(e.getLocalizedMessage());
				id = (long) (Math.random() * -1000000);
			} finally {
				props.put("omdbID", id);
			}

			props.put("title", title);
			props.put("indexTitle", indexTitle);
			props.put("runtime", runtime);
			props.put("released", released);
			props.put("language", language);
			props.put("country", country);

			int imdbVotes;
			try {
				imdbVotes = Integer.parseInt(imdbVotesStr);
			} catch (Exception e) {
				imdbVotes = 0;
			}

			int metascore;
			try {
				metascore = Integer.parseInt(metascoreStr);
			} catch (Exception e) {
				metascore = 0;
			}

			double imdbRating;
			try {
				imdbRating = Double.parseDouble(imdbRatingStr);
			} catch (Exception e) {
				imdbRating = 0.0;
			}

			int year;
			try {
				year = Integer.parseInt(yearStr);
			} catch (Exception e) {
				year = 0;
			}

			//awards & ratings

			props.put("awards", awards);
			props.put("year", year);
			props.put("mpaaRating", mpaaRating);
			props.put("metascore", metascore);
			props.put("imdbRating", imdbRating);
			props.put("imdbVotes", imdbVotes);
			props.put("posterURL", posterURL);
			props.put("genres", genres);
			props.put("updated", currentInitTimestamp);

			Vertex currentMovieVanilla = graph.getVertexByKey("Movie.imdbID", imdbID);
			OrientVertex currentMovie;

			//cut down on duplicate movies
			if (currentMovieVanilla == null) {
				currentMovie = graph.addVertex("class:Movie");
				currentMovie.setProperty("imdbID", imdbID);
			} else {
				currentMovie = graph.getVertex(currentMovieVanilla.getId());
			}

			currentMovie.setProperties(props);

			for (Edge e : currentMovie.getEdges(Direction.IN)) {
				OrientEdge e2 = graph.getEdge(e.getId());
				if ((e2.getLabel().equals("Directed") || e2.getLabel().equals("Wrote") | e2.getLabel().equals("Acted"))
						&& e2.getProperty("source").equals("omdb"))
					graph.removeEdge(e);
			}

			for (String name : currentDirectors) {
				if (!name.equals("N/A")) {
					OrientVertex director = getPersonNode(cleanString(name));
					OrientEdge e = director.addEdge("Directed", currentMovie, "Directed");
					e.setProperty("source", "omdb");
				}
			}

			for (String name : currentWriters) {
				if (!name.equals("N/A")) {
					OrientVertex writer = getPersonNode(cleanString(name));
					OrientEdge e = writer.addEdge("Wrote", currentMovie, "Wrote");
					e.setProperty("source", "omdb");
				}
			}

			for (String name : currentCast) {
				if (!name.equals("N/A")) {
					OrientVertex actor = getPersonNode(cleanString(name));
					OrientEdge e = actor.addEdge("Acted", currentMovie, "Acted");
					e.setProperty("source", "omdb");
				}
			}
		}
	}

	private static void parseRTRatingsToDB(String line) {
		String[] fields = line.split("\\t");
		String omdbID = fields[0];

		long id;
		try {
			id = Integer.parseInt(omdbID);
		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
			id = (long) (Math.random() * -1000000);
		}

		Vertex currentMovieVanilla = graph.getVertexByKey("Movie.omdbID", id);
		OrientVertex currentMovie;

		//cut down on duplicate movies
		if (currentMovieVanilla == null) {
			return;
		}

		String rating = fields[2];
		String meter = fields[3];
		String reviews = fields[4];
		String freshReviews = fields[5];
		String rottenReviews = fields[6];
		String consensus = fields[7];

		int rtTomatoMeter;
		try {
			rtTomatoMeter = Integer.parseInt(meter);
		} catch (Exception e) {
			rtTomatoMeter = 0;
		}

		int rtNumReviews;
		try {
			rtNumReviews = Integer.parseInt(reviews);
		} catch (Exception e) {
			rtNumReviews = 0;
		}

		int rtNumFreshReviews;
		try {
			rtNumFreshReviews = Integer.parseInt(freshReviews);
		} catch (Exception e) {
			rtNumFreshReviews = 0;
		}

		int rtNumRottenReviews;
		try {
			rtNumRottenReviews = Integer.parseInt(rottenReviews);
		} catch (Exception e) {
			rtNumRottenReviews = 0;
		}

		double rtRating;
		try {
			rtRating = Double.parseDouble(rating);
		} catch (Exception e) {
			rtRating = 0.0;
		}

		currentMovie = graph.getVertex(currentMovieVanilla.getId());

		currentMovie.setProperty("rtRating", rtRating);
		currentMovie.setProperty("rtTomatoMeter", rtTomatoMeter);
		currentMovie.setProperty("rtNumReviews", rtNumReviews);
		currentMovie.setProperty("rtNumFreshReviews", rtNumFreshReviews);
		currentMovie.setProperty("rtNumRottenReviews", rtNumRottenReviews);
		currentMovie.setProperty("rtConsensus", consensus);

		graph.commit();
	}

	private static void setupTypesAndIndices() {
		OrientGraphNoTx graph = new OrientGraphNoTx(DB_PATH);

		OrientVertexType movieType = graph.createVertexType("Movie");

		movieType.createProperty("title", OType.STRING);
		movieType.createProperty("indexTitle", OType.STRING);
		movieType.createProperty("imdbID", OType.STRING);
		movieType.createProperty("omdbID", OType.LONG);
		movieType.createProperty("year", OType.INTEGER);

		movieType.createIndex("Movie.imdbID", "UNIQUE", "imdbID");
		movieType.createIndex("Movie.omdbID", "UNIQUE", "omdbID");
		movieType.createIndex("Movie.indexTitle", "FULLTEXT", null, null, "LUCENE", new String[]{ "indexTitle" });

		movieType.createProperty("runtime", OType.STRING);
		movieType.createProperty("released", OType.STRING);
		movieType.createProperty("language", OType.STRING);
		movieType.createProperty("country", OType.STRING);
		movieType.createProperty("awards", OType.STRING);
		movieType.createProperty("mpaaRating", OType.STRING);
		movieType.createProperty("metascore", OType.INTEGER);
		movieType.createProperty("imdbRating", OType.DOUBLE);
		movieType.createProperty("imdbVotes", OType.INTEGER);
		movieType.createProperty("posterURL", OType.STRING);
		movieType.createProperty("genres", OType.STRING);
		movieType.createProperty("rtRating", OType.DOUBLE);
		movieType.createProperty("rtTomatoMeter", OType.INTEGER);
		movieType.createProperty("rtNumReviews", OType.INTEGER);
		movieType.createProperty("rtNumFreshReviews", OType.INTEGER);
		movieType.createProperty("rtNumRottenReviews", OType.INTEGER);
		movieType.createProperty("rtConsensus", OType.STRING);

		OrientVertexType personType = graph.createVertexType("Person");
		personType.createProperty("name", OType.STRING);
		personType.createIndex("Person.name", "UNIQUE", "name");

		OrientVertexType userType = graph.createVertexType("User");
		userType.createProperty("username", OType.STRING);
		userType.createIndex("User.username", "UNIQUE", "username");

		graph.createEdgeType("Acted");
		graph.createEdgeType("Directed");
		graph.createEdgeType("Wrote");

		graph.shutdown();
	}

	private static OrientVertex getPersonNode(String name) {
		Vertex personVanilla = graph.getVertexByKey("Person.name", name);
		OrientVertex person;

		if (personVanilla == null) {
			HashMap<String, String> nameMap = new HashMap<>();
			nameMap.put("name", name);
			nameMap.put("updated", currentInitTimestamp);
			person = graph.addVertex("class:Person", nameMap);
		} else {
			person = graph.getVertex(personVanilla.getId());
			person.setProperty("updated", currentInitTimestamp);
		}
		return person;
	}

	private static String cleanString(String in) {
		String NAME_TERMINATOR = " (";

		String out = in;
		out = out.replace("\\\\", "");
		out = out.replace("\"", "\\\"");
		out = out.replace("\'", "\\\'");
		if (out.contains(NAME_TERMINATOR)) {
			out = out.substring(0, out.indexOf(NAME_TERMINATOR));
		}
		return out;
	}

	private static boolean isOrphanedMovie(Vertex vertex) {
		for (Vertex v : vertex.getVertices(Direction.IN)) {
			int others = 0;
			for (Vertex other : v.getVertices(Direction.OUT)) {
				others++;
				if (others > 1) return false;
			}
		}
		return true;
	}

	private static String getTimestamp() {
		return new SimpleDateFormat("yyyyMMdd.HHmmss").format(new Date());
	}

	@Override
	public void configure(Subparser subparser) {
		super.configure(subparser);
		subparser.addArgument("-s", "--source-file")
				.action(Arguments.store())
				.dest("omdb-export-path")
				.help("The full path of the omdbFull.txt file.");

		subparser.addArgument("-u", "--username")
				.action(Arguments.store())
				.dest("admin-user")
				.help("DB admin username for admin ops (default: admin)");

		subparser.addArgument("-p", "--password")
				.action(Arguments.store())
				.dest("admin-pass")
				.help("DB admin password for admin ops (default: admin)");

		subparser.addArgument("-b", "--backup")
				.action(Arguments.storeTrue())
				.dest("backup")
				.help("Create backup of current DB path before making any changes.");

		subparser.addArgument("-t", "--rotten-tomatoes")
				.action(Arguments.store())
				.dest("rotten-export-path")
				.help("The full path of the tomatoes.txt file.");

		subparser.addArgument("-f", "--first-run")
				.action(Arguments.storeTrue())
				.dest("init-classes-indexes")
				.help("Performs first-run configuration of database schema.");

		subparser.setDefault("admin-user", "admin");
		subparser.setDefault("admin-pass", "admin");
	}

	@Override
	protected void run(Bootstrap<BaconConfiguration> bootstrap,
					   Namespace namespace,
					   BaconConfiguration configuration) throws Exception {

		currentInitTimestamp = getTimestamp();

		config = configuration;
		DB_PATH = config.getDBPath();
		BACKUP_PATH = config.getDbBackupPath();
		OMDB_IMG_API_KEY = config.getOMDBAPIKey();
		ADMIN_USER = namespace.getString("admin-user");
		ADMIN_PASS = namespace.getString("admin-pass");

		String SOURCE_PATH = namespace.getString("omdb-export-path");

		if (SOURCE_PATH == null) {
			LOGGER.error("You must specify a valid path of the OMDB export file.");
			return;
		}

		//backup database
		if (namespace.getBoolean("backup")) {
			File backupFolder = new File(BACKUP_PATH);
			if (!backupFolder.exists()) {
				boolean successful = backupFolder.mkdir();
				if (!successful) {
					throw new IOException("Unable to create backup directory.");
				}
			} else if (!backupFolder.isDirectory()) {
				LOGGER.error("Path to backup folder doesn't point to a folder. Check configuration.");
				return;
			}

			ODatabaseDocumentTx db = new ODatabaseDocumentTx(DB_PATH);
			db.open(ADMIN_USER, ADMIN_PASS);
			try {
				OCommandOutputListener listener = LOGGER::info;

				File backupFile = new File(backupFolder.getCanonicalPath()
						+ File.separator + db.getName()
						+ "_backup_" + getTimestamp() + ".zip");
				OutputStream out = new FileOutputStream(backupFile);

				db.backup(out, null, null, listener, 9, 2048);
			} catch (Exception e) {
				LOGGER.error("Problem backing up the database.", e);
			} finally {
				db.close();
			}
		}

		File omdbFile = new File(SOURCE_PATH);

		graph = new OrientGraph(DB_PATH, ADMIN_USER, ADMIN_PASS);

		try {
			if (namespace.getBoolean("init-classes-indexes")) {
				setupTypesAndIndices();
				LOGGER.info("First run configuration complete.");
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}


		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(omdbFile), "UTF8"));

			//ignore headers
			String line = reader.readLine();
			LOGGER.info("OMDB file reader successfully initialized.");

			graph.declareIntent(new OIntentMassiveInsert());

			while ((line = reader.readLine()) != null) {
				parseMovieToDB(line);
				numMovies++;
				if (numMovies % 10000 == 0) {
					System.out.println("Processed " + numMovies + " so far.");
				}
				if (numMovies % 50 == 0) {
					graph.commit();
				}
			}
			LOGGER.info("Parsed " + numMovies + " total lines.");


			List<Vertex> outOfDate = new ArrayList<>();
			StreamSupport.stream(graph.getVerticesOfClass("Movie").spliterator(), false)
					.filter(v -> !v.getProperty("updated").equals(currentInitTimestamp))
					.forEach(outOfDate::add);

			StreamSupport.stream(graph.getVerticesOfClass("Person").spliterator(), false)
					.filter(v -> !v.getProperty("updated").equals(currentInitTimestamp))
					.forEach(outOfDate::add);

			LOGGER.info("Deleting " + outOfDate.size() + " out of date vertices.");
			outOfDate.stream().forEach(graph::removeVertex);

			int numOrphanedMovies = 0;
			for (Vertex v : graph.getVerticesOfClass("Movie")) {
				if (isOrphanedMovie(v)) {
					numOrphanedMovies++;
					graph.removeVertex(v);
				}
				graph.commit();
			}
			LOGGER.info("Deleted " + numOrphanedMovies + " orphaned movies.");

			int numUnconnected = 0;
			for (Vertex v : graph.getVertices()) {
				//check to see if any edges are connected at all
				boolean delete = v.getEdges(Direction.BOTH).iterator().hasNext();

				if (delete) {
					OrientVertex v2 = graph.getVertex(v.getId());
					if (!v2.getLabel().equals("Movie") || !v2.getLabel().equals("Person")) {
						continue;
					}

					graph.removeVertex(v);
					numUnconnected++;
				}
				graph.commit();
			}
			LOGGER.info("Deleted " + numUnconnected + " unconnected vertices.");

			LOGGER.info("Number of Movies:\t" + graph.countVertices("Movie"));
			LOGGER.info("Number of People:\t" + graph.countVertices("Person"));
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			LOGGER.error("Rolling back in progress changes.");
			graph.rollback();
		} finally {
			LOGGER.info("Done importing. Shutting DB connection down.");
			graph.shutdown();
		}

		String rottenFilePath = namespace.getString("rotten-export-path");
		if (rottenFilePath == null) return;

		LOGGER.info("Parsing Rotten Tomatoes information.");

		graph = new OrientGraph(DB_PATH, ADMIN_USER, ADMIN_PASS);
		LOGGER.info("Database back online.");

		try {

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(new File(rottenFilePath)), "UTF8"));

			String line = reader.readLine();
			LOGGER.info("Tomatoes file reader successfully initialized.");

			int tomatoRatings = 0;
			while ((line = reader.readLine()) != null) {
				parseRTRatingsToDB(line);
				tomatoRatings++;
			}
			LOGGER.info("Parsed " + tomatoRatings + " RT ratings into database.");

			for (Vertex v : graph.getVertices()) {
				if (v.getProperty("rtRating") == null)
					v.setProperty("rtRating", 0.0);

				if (v.getProperty("rtTomatoMeter") == null)
					v.setProperty("rtTomatoMeter", 0);

				if (v.getProperty("rtNumReviews") == null)
					v.setProperty("rtNumReviews", 0);

				if (v.getProperty("rtNumFreshReviews") == null)
					v.setProperty("rtNumFreshReviews", 0);

				if (v.getProperty("rtNumRottenReviews") == null)
					v.setProperty("rtNumRottenReviews", 0);

				if (v.getProperty("rtConsensus") == null)
					v.setProperty("rtConsensus", "N/A");

				graph.commit();
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			LOGGER.error("Rolling back in progress changes.");
			graph.rollback();
		} finally {
			LOGGER.info("Done importing Rotten Tomatoes information. Shutting DB connection down.");
			graph.shutdown();
		}
	}
}
