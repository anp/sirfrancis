package io.sirfrancis.bacon.tasks;

import com.google.common.collect.ImmutableMultimap;
import io.dropwizard.servlets.tasks.Task;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.core.Movie;
import io.sirfrancis.bacon.db.GraphCleaner;
import io.sirfrancis.bacon.db.MovieDAO;
import io.sirfrancis.bacon.db.QuizDAO;
import io.sirfrancis.bacon.util.Timestamper;
import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.ZipFile;

public class DBUpdateTask extends Task {
	private static final Logger LOGGER = LoggerFactory.getLogger(DBUpdateTask.class);
	private static final String[] IGNORED_GENRES_ARRAY =
			{ "Short", "Talk-Show", "Reality-TV", "Game-Show", "Adult", "News", "Documentary" };
	private static final HashSet<String> IGNORED_GENRES = new HashSet<>();
	public static String currentInitTimestamp;
	private MovieDAO dao;


	public DBUpdateTask() {
		super("update-db");
		this.dao = new MovieDAO();

		IGNORED_GENRES.addAll(Arrays.asList(IGNORED_GENRES_ARRAY));
		currentInitTimestamp = Timestamper.getTimestamp();
	}

	private static void parseRTRatingsToDB(String line, Map<String, Movie> movies, Map<Long, String> omdbMap) {

		String[] fields = line.split("\\t");
		String omdbID = fields[0];

		long id;
		try {
			id = Long.parseLong(omdbID);
		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
			id = (long) (Math.random() * -1000000);
		}

		Movie movie = movies.get(omdbMap.get(id));
		if (movie == null) return;

		String rating = fields[2];
		String meter = fields[3];
		String reviews = fields[4];
		String freshReviews = fields[5];
		String rottenReviews = fields[6];
		String consensus = fields[7];

		int rtTomatoMeter = 0;
		try {
			rtTomatoMeter = Integer.parseInt(meter);
		} catch (Exception e) {
		}

		int rtNumReviews = 0;
		try {
			rtNumReviews = Integer.parseInt(reviews);
		} catch (Exception e) {
		}

		int rtNumFreshReviews = 0;
		try {
			rtNumFreshReviews = Integer.parseInt(freshReviews);
		} catch (Exception e) {
		}

		int rtNumRottenReviews = 0;
		try {
			rtNumRottenReviews = Integer.parseInt(rottenReviews);
		} catch (Exception e) {
		}

		double rtRating = 0.0;
		try {
			rtRating = Double.parseDouble(rating);
		} catch (Exception e) {
		}

		movie.setRottenTomatoRating(rtRating);
		movie.setRottenTomatoMeter(rtTomatoMeter);
		movie.setRottenTomatoesNumReviews(rtNumReviews);
		movie.setRottenTomatoesNumFreshReviews(rtNumFreshReviews);
		movie.setRottenTomatoesNumRottenReviews(rtNumRottenReviews);
		movie.setRottenTomatoesConsensus(consensus);
	}


	private void parseOMDBLineToMovie(String line, Map<String, Movie> movies, Map<Long, String> omdbMap) {
		String[] fields = line.split("\\t");
		if (fields.length == 21 || (fields.length == 22 && fields[21].equalsIgnoreCase("movie"))) {
			String omdbIDStr = fields[0];
			String imdbID = fields[1];
			String title = fields[2];
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

			String genres = fields[6];
			for (String g : genres.split(", ")) {
				if (IGNORED_GENRES.contains(g)) {
					return;
				}
			}

			long omdbID = 0;
			try {
				omdbID = Integer.parseInt(omdbIDStr);
			} catch (Exception e) {
				System.err.println(e.getLocalizedMessage());
				omdbID = (long) (Math.random() * -1000000);
			}


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

			String[] currentDirectors = fields[8].split(", ");
			HashSet<String> directors = new HashSet<>();
			for (String d : currentDirectors) {
				directors.add(d);
			}


			String[] currentWriters = fields[9].split(", ");
			HashSet<String> writers = new HashSet<>();
			for (String w : currentWriters) {
				writers.add(w);
			}


			String[] currentCast = fields[10].split(", ");
			HashSet<String> actors = new HashSet<>();
			for (String a : currentCast) {
				actors.add(a);
			}

			Movie movie = new Movie(imdbID, omdbID, title);
			if (movies.containsKey(imdbID)) {
				movie = movies.get(imdbID);
			}

			movie.setIndexTitle(QueryParserUtil.escape(title).toLowerCase());

			movie.setRuntime(runtime);
			movie.setReleased(released);
			movie.setLanguage(language);
			movie.setGenres(genres);
			movie.setCountry(country);
			movie.setYear(year);
			movie.setAwards(awards);
			movie.setMpaaRating(mpaaRating);
			movie.setMetascore(metascore);
			movie.setImdbRating(imdbRating);
			movie.setImdbVotes(imdbVotes);

			movie.setDirectors(directors);
			movie.setWriters(writers);
			movie.setActors(actors);

			movie.setUpdated(currentInitTimestamp);

			omdbMap.put(omdbID, imdbID);
			movies.put(imdbID, movie);
		}
	}

	@Override
	public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {

		LOGGER.info("Updating database from OMDB export.");

		File currentZip = new File(BaconConfiguration.getOmdbExportPath());
		if (!currentZip.exists()) {
			LOGGER.error("OMDB export file does not exist. Try running the \"download-omdb\" task first.");
			return;
		}

		currentInitTimestamp = Timestamper.getTimestamp();

		ZipFile omdbZip = new ZipFile(currentZip);

		Map<String, Movie> movies = new HashMap<>();
		Map<Long, String> omdbMap = new HashMap<>();

		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(
							omdbZip.getInputStream(
									omdbZip.getEntry(
											BaconConfiguration.getOmdbMoviesFilename())), "UTF8"));

			//ignore headers
			String line;
			reader.readLine();
			LOGGER.info("OMDB file reader successfully initialized.");

			int numMovies = 0;
			while ((line = reader.readLine()) != null) {

				parseOMDBLineToMovie(line, movies, omdbMap);
				numMovies++;
			}
			LOGGER.info("Parsed " + numMovies + " total lines.");


			reader = new BufferedReader(
					new InputStreamReader(
							omdbZip.getInputStream(
									omdbZip.getEntry(
											BaconConfiguration.getOmdbTomatoesFilename())), "UTF8"));

			reader.readLine();
			LOGGER.info("Tomatoes file reader successfully initialized.");

			int tomatoRatings = 0;
			while ((line = reader.readLine()) != null) {
				parseRTRatingsToDB(line, movies, omdbMap);
				tomatoRatings++;
			}
			LOGGER.info("Parsed " + tomatoRatings + " RT ratings into movies.");

			movies.entrySet().stream()
					.map(Map.Entry::getValue)
					.filter(m -> m.getRottenTomatoesConsensus() == null)
					.forEach(m -> m.setRottenTomatoesConsensus("N/A"));

			reader.close();

			LOGGER.info("Writing updated movie info to DB.");

			movies.entrySet().stream()
					.map(Map.Entry::getValue)
					.forEach(dao::writeMovie);

			LOGGER.info("Finished writing movies to DB.");
			LOGGER.info("Cleaning up graph.");

			GraphCleaner cleaner = new GraphCleaner(currentInitTimestamp);
			cleaner.cleanGraph();

			LOGGER.info("Done cleaning up graph.");

			new QuizDAO().initQuiz();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}
