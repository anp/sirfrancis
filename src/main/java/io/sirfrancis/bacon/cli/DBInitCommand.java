package io.sirfrancis.bacon.cli;

import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Parameter;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import io.sirfrancis.bacon.BaconConfiguration;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.StreamSupport;

/**
 * Created by Adam on 1/4/2015.
 */
public class DBInitCommand extends ConfiguredCommand<BaconConfiguration> {
	private static final Logger LOGGER = LoggerFactory.getLogger(DBInitCommand.class);
	private static BaconConfiguration config;

	private static final String[] IGNORED_GENRES_ARRAY =
			{ "Short", "Talk-Show", "Reality-TV", "Game-Show", "Adult", "News" };
	private static final HashSet<String> IGNORED_GENRES = new HashSet<>();

	private static int numMovies = 0;

	private static String DB_PATH;
	private static String BACKUP_PATH;
	private static OrientGraph graph;

	private static String currentInitTimestamp;

	private static String OMDB_IMG_API_KEY = "2af3daf2";


	public DBInitCommand() {
		super("db", "Initialize the database with an omdbFull.txt file");
		IGNORED_GENRES.addAll(Arrays.asList(IGNORED_GENRES_ARRAY));
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
				.help("Create backup of current DB path before making any changes. NOT THREAD SAFE.");

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
		String ADMIN_USER = namespace.getString("admin-user");
		String ADMIN_PASS = namespace.getString("admin-pass");

		String SOURCE_PATH = namespace.getString("omdb-export-path");

		if (SOURCE_PATH == null) {
			LOGGER.error("You must specify a valid path of the OMDB export file.");
			return;
		}

		//backup database
		if (namespace.getBoolean("backup")) {
			File backupFolder = new File(BACKUP_PATH);
			if (!backupFolder.exists()) {
				backupFolder.mkdir();
			} else if (!backupFolder.isDirectory()) {
				LOGGER.error("Path to backup folder doesn't point to a folder. Check configuration.");
				return;
			}

			ODatabaseDocumentTx db = new ODatabaseDocumentTx(DB_PATH);
			db.open(ADMIN_USER, ADMIN_PASS);
			try {
				OCommandOutputListener listener = new OCommandOutputListener() {
					@Override
					public void onMessage(String iText) {
						LOGGER.info(iText);
					}
				};

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
				configureTypes();
				configureIndices();
			}

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(omdbFile), "UTF8"));

			String line = reader.readLine();
			LOGGER.info("OMDB file reader successfully initialized.");

			while ((line = reader.readLine()) != null) {
				parseMovieToDB(line);
				numMovies++;
				if (numMovies % 10000 == 0) {
					System.out.println("Processed " + numMovies + " so far.");
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
				boolean delete = true;
				for (Edge e : v.getEdges(Direction.BOTH)) {
					delete = false;
					break;
				}
				//TODO don't delete users
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
					new InputStreamReader(new FileInputStream(omdbFile), "UTF8"));

			String line = reader.readLine();
			LOGGER.info("Tomatoes file reader successfully initialized.");

			int tomatoRatings = 0;
			while ((line = reader.readLine()) != null) {
				parseRTRatingsToDB(line);
				tomatoRatings++;
			}
			LOGGER.info("Parsed " + tomatoRatings + " RT ratings into database.");

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			LOGGER.error("Rolling back in progress changes.");
			graph.rollback();
		} finally {
			LOGGER.info("Done importing Rotten Tomatoes information. Shutting DB connection down.");
			graph.shutdown();
		}
	}

	private static void parseMovieToDB(String line) {
		String[] fields = line.split("\\t");
		if (fields.length == 21 || (fields.length == 22 && fields[21].equalsIgnoreCase("movie"))) {
			String omdbID = fields[0];
			String imdbID = fields[1];
			String title = fields[2];
			String runtime = fields[5];
			String released = fields[7];
			String language = fields[17];
			String country = fields[18];
			String yearStr = fields[3];
			String awards = fields[19];
			String omdbRating = fields[4];
			String metascore = fields[11];
			String imdbRating = fields[12];
			String imdbVotes = fields[13];
			String posterURL = "http://img.omdbapi.com/?i=" + imdbID + "&apikey=" + OMDB_IMG_API_KEY;
			String[] genres = fields[6].split(", ");
			for (String g : genres) {
				if (IGNORED_GENRES.contains(g)) {
					return;
				}
			}

			String[] currentDirectors = fields[8].split(", ");
			String[] currentWriters = fields[9].split(", ");
			String[] currentCast = fields[10].split(", ");

			HashMap<String, Object> props = new HashMap<>();

			int id = 0;
			try {
				id = Integer.parseInt(omdbID);
			} catch (Exception e) {
				System.err.println(e.getLocalizedMessage());
				id = (int) (Math.random() * -1000000);
			} finally {
				props.put("omdbID", id);
			}

			//"intrinsic" info

			props.put("title", title);
			props.put("runtime", runtime);
			props.put("released", released);
			props.put("language", language);
			props.put("country", country);


			try {
				int year = Integer.parseInt(yearStr);
				props.put("year", year);
			} catch (Exception e) {
				props.put("year", yearStr);
			}

			//awards & ratings

			props.put("awards", awards);

			//TODO parse into primitives
			props.put("omdbRating", omdbRating);
			props.put("metascore", metascore);
			props.put("imdbRating", imdbRating);
			props.put("imdbVotes", imdbVotes);
			props.put("poserURL", posterURL);
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
				if ((e2.getLabel() == "Directed" || e2.getLabel() == "Wrote" | e2.getLabel() == "Acted")
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

			graph.commit();
		}
	}

	private static void parseRTRatingsToDB(String line) {
		String[] fields = line.split("\\t");
		String omdbID = fields[0];

		int id;
		try {
			id = Integer.parseInt(omdbID);
		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
			id = (int) (Math.random() * -1000000);
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

		currentMovie = graph.getVertex(currentMovieVanilla.getId());

		currentMovie.setProperty("rtRating", rating);
		currentMovie.setProperty("rtTomatoMeter", meter);
		currentMovie.setProperty("rtNumReviews", reviews);
		currentMovie.setProperty("rtNumFreshReviews", freshReviews);
		currentMovie.setProperty("rtNumRottenReviews", rottenReviews);
		currentMovie.setProperty("rtConsensus", consensus);

		graph.commit();
	}

	private static void configureTypes() {
		if (graph.getVertexType("Movie") == null) {
			graph.commit();
			graph.createVertexType("Movie");
			System.out.println("Creating Movie vertex type...");
		} else {
			System.out.println("Movie vertex type already exists.");
		}

		if (graph.getVertexType("Person") == null) {
			graph.commit();
			graph.createVertexType("Person");
			System.out.println("Creating Person vertex type...");
		} else {
			System.out.println("Person vertex type already exists.");
		}

		if (graph.getVertexType("User") == null) {
			graph.commit();
			graph.createVertexType("User");
			System.out.println("Creating User vertex type...");
		} else {
			System.out.println("User vertex type already exists.");
		}

		if (graph.getEdgeType("Acted") == null) {
			graph.commit();
			graph.createEdgeType("Acted");
			System.out.println("Creating Acted edge type...");
		} else {
			System.out.println("Acted edge type already exists.");
		}

		if (graph.getEdgeType("Directed") == null) {
			graph.commit();
			graph.createEdgeType("Directed");
			System.out.println("Creating Directed edge type...");
		} else {
			System.out.println("Directed edge type already exists.");
		}

		if (graph.getEdgeType("Wrote") == null) {
			graph.commit();
			graph.createEdgeType("Wrote");
			System.out.println("Creating Wrote edge type...");
		} else {
			System.out.println("Wrote edge type already exists.");
		}
	}

	private static void configureIndices() {
		try {
			graph.commit();
			graph.createKeyIndex("imdbID", Vertex.class,
					new Parameter("type", "UNIQUE"), new Parameter("class", "Movie"));
			System.out.println("Creating imdbid index...");
		} catch (Exception e) {
			System.out.println("Movie index on imdbid already exists.");
		}

		try {
			graph.commit();
			graph.createKeyIndex("omdbID", Vertex.class,
					new Parameter("type", "UNIQUE"), new Parameter("class", "Movie"));
			System.out.println("Creating omdbid index...");
		} catch (Exception e) {
			System.out.println("Movie index on omdbid already exists.");
		}

		try {
			graph.commit();
			graph.createKeyIndex("title", Vertex.class,
					new Parameter("class", "Movie"));
			System.out.println("Creating movie title index...");
		} catch (Exception e) {
			System.out.println("Movie index on title already exists.");
		}

		try {
			graph.commit();
			graph.createKeyIndex("name", Vertex.class,
					new Parameter("type", "UNIQUE"), new Parameter("class", "Person"));
			System.out.println("Creating person name index...");
		} catch (Exception e) {
			System.out.println("Person index on name already exists.");
		}
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
}
