package io.sirfrancis.bacon.db;

import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.exception.OTransactionException;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.core.Movie;
import io.sirfrancis.bacon.db.enums.*;
import io.sirfrancis.bacon.tasks.DBUpdateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MovieDAO {
	Logger LOGGER = LoggerFactory.getLogger(MovieDAO.class);
	private String omdbPosterURL;
	private int maxRetries;

	public MovieDAO() {
		this.omdbPosterURL = BaconConfiguration.getOmdbPosterURL();
		this.maxRetries = BaconConfiguration.getMaxRetries();
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

	public List<Movie> searchMovies(String rawSearch, int maxNumberOfResults) {
		OrientGraph graph = GraphConnection.getGraph();
		LinkedList<Movie> searchResults = new LinkedList<>();

		try {
			String luceneSearch = rawSearch.replaceAll("[']", "").replaceAll("[^A-Za-z0-9]", " ").toLowerCase();

			String baseQuery = "select from " + Vertices.MOVIE + " where " + MovieProps.INDEXTITLE + " LUCENE ?";// LIMIT " + maxNumberOfResults;

			Iterable<Vertex> results = graph.command(new OCommandSQL(baseQuery)).execute(luceneSearch);

			for (Vertex v : results) {
				if (searchResults.size() >= maxNumberOfResults) break;

				searchResults.add(buildMovie(v));
			}
		} finally {
			graph.shutdown();
		}

		return searchResults;
	}

	public Movie buildMovie(Vertex movieVertex) {
		OrientGraph graph = GraphConnection.getGraph();
		Movie thisMovie;

		try {
			Vertex freshMovieVertex = graph.getVertex(movieVertex.getId());

			long omdbID = freshMovieVertex.getProperty(MovieProps.OMDBID);
			String imdbID = freshMovieVertex.getProperty(MovieProps.IMDBID);
			String title = freshMovieVertex.getProperty(MovieProps.TITLE);
			String runtime = freshMovieVertex.getProperty(MovieProps.RUNTIME);
			String released = freshMovieVertex.getProperty(MovieProps.RELEASED);
			String language = freshMovieVertex.getProperty(MovieProps.LANGUAGE);
			String country = freshMovieVertex.getProperty(MovieProps.COUNTRY);
			String genres = freshMovieVertex.getProperty(MovieProps.GENRES);
			int year = freshMovieVertex.getProperty(MovieProps.YEAR);
			String awards = freshMovieVertex.getProperty(MovieProps.AWARDS);
			String mpaaRating = freshMovieVertex.getProperty(MovieProps.MPAARATING);
			int metascore = freshMovieVertex.getProperty(MovieProps.METASCORE);
			double imdbRating = freshMovieVertex.getProperty(MovieProps.IMDBRATING);
			int imdbVotes = freshMovieVertex.getProperty(MovieProps.IMDBVOTES);

			double rtRating = freshMovieVertex.getProperty(MovieProps.RTRATING);
			int tomatoMeter = freshMovieVertex.getProperty(MovieProps.RTMETER);
			int rtNumReviews = freshMovieVertex.getProperty(MovieProps.RTNREVIEWS);
			int rtNumFreshReviews = freshMovieVertex.getProperty(MovieProps.RTNFRESHREVIEWS);
			int rtNumRottenReviews = freshMovieVertex.getProperty(MovieProps.RTNROTTENREVIEWS);
			String rtConsensus = freshMovieVertex.getProperty(MovieProps.RTCONSENSUS);

			String updated = freshMovieVertex.getProperty(Vertices.UPDATED);

			thisMovie = new Movie(imdbID, omdbID, title);

			thisMovie.setRuntime(runtime);
			thisMovie.setReleased(released);
			thisMovie.setLanguage(language);
			thisMovie.setGenres(genres);
			thisMovie.setCountry(country);
			thisMovie.setYear(year);
			thisMovie.setAwards(awards);
			thisMovie.setMpaaRating(mpaaRating);
			thisMovie.setMetascore(metascore);
			thisMovie.setImdbRating(imdbRating);
			thisMovie.setImdbVotes(imdbVotes);
			thisMovie.setRottenTomatoRating(rtRating);
			thisMovie.setRottenTomatoMeter(tomatoMeter);
			thisMovie.setRottenTomatoesNumReviews(rtNumReviews);
			thisMovie.setRottenTomatoesNumFreshReviews(rtNumFreshReviews);
			thisMovie.setRottenTomatoesNumRottenReviews(rtNumRottenReviews);
			thisMovie.setRottenTomatoesConsensus(rtConsensus);

			thisMovie.setUpdated(updated);

			thisMovie.setPosterURL(omdbPosterURL.replace("%i", imdbID));

			Set<String> directors = new HashSet<>();
			for (Vertex d : movieVertex.getVertices(Direction.BOTH, Edges.DIRECTED)) {
				directors.add(d.getProperty(PersonProps.NAME));
			}

			Set<String> writers = new HashSet<>();
			for (Vertex w : movieVertex.getVertices(Direction.BOTH, Edges.WROTE)) {
				writers.add(w.getProperty(PersonProps.NAME));
			}

			Set<String> actors = new HashSet<>();
			for (Vertex a : movieVertex.getVertices(Direction.BOTH, Edges.ACTED)) {
				actors.add(a.getProperty(PersonProps.NAME));
			}

			thisMovie.setDirectors(directors);
			thisMovie.setWriters(writers);
			thisMovie.setActors(actors);
			graph.commit();
		} finally {
			graph.shutdown();
		}

		return thisMovie;
	}

	public void writeMovie(Movie movie) {
		OrientGraph graph = GraphConnection.getGraph();
		Vertex movieVertex = null;

		for (int i = 0; i < maxRetries; i++) {
			try {
				movieVertex = graph.getVertexByKey(Indexes.MOVIE_IMDBID, movie.getImdbID());

				if (movieVertex == null) {
					movieVertex = graph.addVertex("class:" + Vertices.MOVIE);
					movieVertex.setProperty(MovieProps.OMDBID, movie.getOmdbID());
					movieVertex.setProperty(MovieProps.IMDBID, movie.getImdbID());
				}

				movieVertex.setProperty(MovieProps.TITLE, movie.getTitle());
				movieVertex.setProperty(MovieProps.INDEXTITLE, movie.getIndexTitle());
				movieVertex.setProperty(MovieProps.RUNTIME, movie.getRuntime());
				movieVertex.setProperty(MovieProps.RELEASED, movie.getReleased());
				movieVertex.setProperty(MovieProps.LANGUAGE, movie.getLanguage());
				movieVertex.setProperty(MovieProps.COUNTRY, movie.getCountry());
				movieVertex.setProperty(MovieProps.GENRES, movie.getGenres());
				movieVertex.setProperty(MovieProps.YEAR, movie.getYear());
				movieVertex.setProperty(MovieProps.AWARDS, movie.getAwards());
				movieVertex.setProperty(MovieProps.MPAARATING, movie.getMpaaRating());
				movieVertex.setProperty(MovieProps.METASCORE, movie.getMetascore());
				movieVertex.setProperty(MovieProps.IMDBRATING, movie.getImdbRating());
				movieVertex.setProperty(MovieProps.IMDBVOTES, movie.getImdbVotes());
				movieVertex.setProperty(MovieProps.RTRATING, movie.getRottenTomatoRating());
				movieVertex.setProperty(MovieProps.RTMETER, movie.getRottenTomatoMeter());
				movieVertex.setProperty(MovieProps.RTNREVIEWS, movie.getRottenTomatoesNumReviews());
				movieVertex.setProperty(MovieProps.RTNFRESHREVIEWS, movie.getRottenTomatoesNumFreshReviews());
				movieVertex.setProperty(MovieProps.RTNROTTENREVIEWS, movie.getRottenTomatoesNumRottenReviews());
				movieVertex.setProperty(MovieProps.RTCONSENSUS, movie.getRottenTomatoesConsensus());

				movieVertex.setProperty(Vertices.UPDATED, DBUpdateTask.currentInitTimestamp);

				for (Edge e : movieVertex.getEdges(Direction.IN)) {
					if ((e.getLabel().equals(Edges.DIRECTED) ||
							e.getLabel().equals(Edges.WROTE) ||
							e.getLabel().equals(Edges.ACTED))) {
						graph.removeEdge(e);
					}
				}

				for (String name : movie.getDirectors()) {
					if (!name.equals("N/A")) {
						getPersonVertex(cleanString(name)).addEdge(Edges.DIRECTED, movieVertex);
					}
				}

				for (String name : movie.getWriters()) {
					if (!name.equals("N/A")) {
						getPersonVertex(cleanString(name)).addEdge(Edges.WROTE, movieVertex);
					}
				}

				for (String name : movie.getActors()) {
					if (!name.equals("N/A")) {
						getPersonVertex(cleanString(name)).addEdge(Edges.ACTED, movieVertex);
					}
				}

				graph.commit();
				break;
			} catch (OTransactionException | OConcurrentModificationException ote) {
				//ignore, will retry
			}
		}
		graph.shutdown();
	}

	private Vertex getPersonVertex(String name) {
		OrientGraph graph = GraphConnection.getGraph();
		Vertex person = null;

		for (int i = 0; i < maxRetries; i++) {
			try {
				person = graph.getVertexByKey(Indexes.PERSON_NAME, name);

				if (person == null) {
					person = graph.addVertex("class:" + Vertices.PERSON);
					person.setProperty(PersonProps.NAME, name);
				}

				person.setProperty(Vertices.UPDATED, DBUpdateTask.currentInitTimestamp);

				graph.commit();
				break;
			} catch (OTransactionException | OConcurrentModificationException | ORecordDuplicatedException ote) {
				//ignore, will retry
			}
		}
		graph.shutdown();
		return person;
	}
}
