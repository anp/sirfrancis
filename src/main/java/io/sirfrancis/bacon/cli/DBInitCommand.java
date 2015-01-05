package io.sirfrancis.bacon.cli;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

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

	private static OrientGraph graph;

	public DBInitCommand() {
		super("initdb", "Initialize the database with an omdbFull.txt file");
		IGNORED_GENRES.addAll(Arrays.asList(IGNORED_GENRES_ARRAY));
	}

	@Override
	public void configure(Subparser subparser) {
		super.configure(subparser);
		subparser.addArgument("-s", "--source-file")
				.action(Arguments.store())
				.dest("omdb-export-path")
				.help("The full path of the omdbFull.txt file.");

		subparser.addArgument("-d", "--delete-current")
				.action(Arguments.storeTrue())
				.dest("delete-old-db")
				.help("Optional: delete current db files before importing.");

		subparser.addArgument("-u", "--username")
				.action(Arguments.store())
				.dest("admin-user")
				.help("DB admin username for admin ops (default: admin)");

		subparser.addArgument("-p", "--password")
				.action(Arguments.store())
				.dest("admin-pass")
				.help("DB admin password for admin ops (default: admin)");

		subparser.setDefault("delete-old-db", false);
		subparser.setDefault("admin-user", "admin");
		subparser.setDefault("admin-pass", "admin");
	}

	@Override
	protected void run(Bootstrap<BaconConfiguration> bootstrap,
					   Namespace namespace,
					   BaconConfiguration configuration) throws Exception {

		config = configuration;
		String DB_PATH = config.getDBPath();

		if (namespace.getBoolean("delete-old-db")) {
			//TODO delete the old db files
			System.out.println("Deleting old database...");
		}

		String SOURCE_PATH = namespace.getString("omdb-export-path");

		if (SOURCE_PATH == null) {
			System.err.println("ERROR: You must specify the path of the OMDB export file.");
		}

		String ADMIN_USER = namespace.getString("admin-user");
		String ADMIN_PASS = namespace.getString("admin-pass");

		graph = new OrientGraph(DB_PATH, ADMIN_USER, ADMIN_PASS);

		File omdbFile = new File(SOURCE_PATH);
		try {

			configureTypes();
			configureIndices();

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(omdbFile), "UTF8"));
			System.out.println("OMDB reader successfully initialized.");

			String line = reader.readLine();

			while ((line = reader.readLine()) != null) {
				//System.out.println("Parsing movie...");
				parseMovieToDB(line);
				numMovies++;
				if (numMovies % 10000 == 0) {
					System.out.println("Processed " + numMovies + " so far.");
				}
			}
			System.out.println("SUCCESS! Check the database out. " + numMovies + " parsed.");

			System.out.println("deleting orphaned movies...");
			for (Vertex v : graph.getVerticesOfClass("Movie")) {
				if (isOrphanedMovie(v)) {
					//System.out.println(n.getProperty("title") + "," + n.getProperty("imdbID"));
					System.out.println("Deleting orphaned movie: " + v.getProperty("title"));
					graph.removeVertex(v);
				}
				graph.commit();
			}

			System.out.println("deleting unconnected vertices...");
			for (Vertex v : graph.getVertices()) {
				int numEdges = 0;
				for (Edge e : v.getEdges(Direction.BOTH)) {
					numEdges++;
					break;
				}
				if (numEdges == 0) {
					graph.removeVertex(v);
				}
				graph.commit();
			}

			System.out.println("Number of Movies:\t" + graph.countVertices("Movie"));
			System.out.println("Number of People:\t" + graph.countVertices("Person"));
			//System.out.println("Number of Roles:\t" + graph.countEdges("Acted"));
			//System.out.println("Number of Writers:\t" + graph.countEdges("Wrote"));
			//System.out.println("Number of Directors:\t" + graph.countEdges("Directed"));

		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			graph.rollback();
		} finally {
			System.out.println("Shutting DB down...");
			graph.shutdown();
		}

	}

	private static void configureTypes() {
		if (graph.getVertexType("Movie") == null) {
			graph.createVertexType("Movie");
			System.out.println("Creating Movie vertex type...");
		} else {
			System.out.println("Movie vertex type already exists.");
		}

		if (graph.getVertexType("Person") == null) {
			graph.createVertexType("Person");
			System.out.println("Creating Person vertex type...");
		} else {
			System.out.println("Person vertex type already exists.");
		}

		if (graph.getVertexType("User") == null) {
			graph.createVertexType("User");
			System.out.println("Creating User vertex type...");
		} else {
			System.out.println("User vertex type already exists.");
		}

		if (graph.getEdgeType("Acted") == null) {
			graph.createEdgeType("Acted");
			System.out.println("Creating Acted edge type...");
		} else {
			System.out.println("Acted edge type already exists.");
		}

		if (graph.getEdgeType("Directed") == null) {
			graph.createEdgeType("Directed");
			System.out.println("Creating Directed edge type...");
		} else {
			System.out.println("Directed edge type already exists.");
		}

		if (graph.getEdgeType("Wrote") == null) {
			graph.createEdgeType("Wrote");
			System.out.println("Creating Wrote edge type...");
		} else {
			System.out.println("Wrote edge type already exists.");
		}


	}

	private static void configureIndices() {
		try {
			graph.createKeyIndex("imdbID", Vertex.class,
					new Parameter("type", "UNIQUE"), new Parameter("class", "Movie"));
			System.out.println("Creating imdbid index...");
		} catch (Exception e) {
			System.out.println("Movie index on imdbid already exists.");
		}

		try {
			graph.createKeyIndex("title", Vertex.class,
					new Parameter("class", "Movie"));
			System.out.println("Creating movie title index...");
		} catch (Exception e) {
			System.out.println("Movie index on title already exists.");
		}

		try {
			graph.createKeyIndex("name", Vertex.class,
					new Parameter("type", "UNIQUE"), new Parameter("class", "Person"));
			System.out.println("Creating person name index...");
		} catch (Exception e) {
			System.out.println("Person index on name already exists.");
		}
	}

	private static void parseMovieToDB(String line) {
		String[] fields = line.split("\\t");
		if (fields.length == 22 && fields[21].equals("movie")) {
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

			//props.put("freebaseID", -1000);

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

			props.put("genres", genres);

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

			//reset edges to be re-created
			//System.out.println("Cleaning old relationships on " + title);
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

	private static OrientVertex getPersonNode(String name) {
		Vertex personVanilla = graph.getVertexByKey("Person.name", name);
		OrientVertex person;

		if (personVanilla == null) {
			HashMap<String, String> nameMap = new HashMap<>();
			nameMap.put("name", name);
			person = graph.addVertex("class:Person", nameMap);
		} else {
			person = graph.getVertex(personVanilla.getId());
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
}
