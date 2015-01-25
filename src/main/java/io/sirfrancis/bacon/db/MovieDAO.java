package io.sirfrancis.bacon.db;

import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import io.sirfrancis.bacon.core.Movie;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by adam on 1/24/15.
 */
public class MovieDAO {
	private OrientGraphFactory factory;

	public MovieDAO(OrientGraphFactory factory) {
		this.factory = factory;
	}

	public List<Movie> searchMovies(String rawSearch, int maxNumberOfResults) {
		OrientGraph graph = factory.getTx();
		LinkedList<Movie> searchResults = new LinkedList<>();

		String luceneSearch = rawSearch.replaceAll("[^A-Za-z0-9]", " ").toLowerCase();
		String baseQuery = "select from Movie where indexTitle LUCENE ?";
		Iterable<Vertex> results = graph.command(new OCommandSQL(baseQuery)).execute(luceneSearch);

		for (Vertex v : results) {
			searchResults.add(buildMovie(v));
		}

		searchResults.sort(new IMDBVoteComparator());
		searchResults.subList(15, searchResults.size()).clear();

		return searchResults;
	}

	public Movie buildMovie(Vertex movieVertex) {
		long omdbID = movieVertex.getProperty("omdbID");
		String imdbID = movieVertex.getProperty("imdbID");
		String title = movieVertex.getProperty("title");
		String runtime = movieVertex.getProperty("runtime");
		String released = movieVertex.getProperty("released");
		String language = movieVertex.getProperty("language");
		String country = movieVertex.getProperty("country");
		int year = movieVertex.getProperty("year");
		String awards = movieVertex.getProperty("awards");
		String mpaaRating = movieVertex.getProperty("mpaaRating");
		int metascore = movieVertex.getProperty("metascore");
		double imdbRating = movieVertex.getProperty("imdbRating");
		int imdbVotes = movieVertex.getProperty("imdbVotes");
		String posterURL = movieVertex.getProperty("posterURL");

		//ArrayList<String> genresList = movieVertex.getProperty("genres");
		//String[] genres = genresList.toArray(new String[1]);

		double rtRating = movieVertex.getProperty("rtRating");
		int tomatoMeter = movieVertex.getProperty("rtTomatoMeter");
		int rtNumReviews = movieVertex.getProperty("rtNumReviews");
		int rtNumFreshReviews = movieVertex.getProperty("rtNumFreshReviews");
		int rtNumRottenReviews = movieVertex.getProperty("rtNumRottenReviews");
		String rtConsensus = movieVertex.getProperty("rtConsensus");

		Movie thisMovie = new Movie(imdbID, omdbID, title);

		thisMovie.setRuntime(runtime);
		thisMovie.setReleased(released);
		thisMovie.setLanguage(language);
		thisMovie.setCountry(country);
		thisMovie.setYear(year);
		thisMovie.setAwards(awards);
		thisMovie.setMpaaRating(mpaaRating);
		thisMovie.setMetascore(metascore);
		thisMovie.setImdbRating(imdbRating);
		thisMovie.setImdbVotes(imdbVotes);
		thisMovie.setPosterURL(posterURL);
		//thisMovie.setGenres(genres);
		thisMovie.setRtRating(rtRating);
		thisMovie.setTomatoMeter(tomatoMeter);
		thisMovie.setRtNumReviews(rtNumReviews);
		thisMovie.setRtNumFreshReviews(rtNumFreshReviews);
		thisMovie.setRtNumRottenReviews(rtNumRottenReviews);
		thisMovie.setRtConsensus(rtConsensus);

		return thisMovie;
	}

	private class IMDBVoteComparator implements Comparator<Movie> {
		public int compare(Movie first, Movie second) {
			if (first.getImdbVotes() == second.getImdbVotes()) {
				return first.getTitle().compareTo(second.getTitle());
			}

			return second.getImdbVotes() - first.getImdbVotes();
		}
	}
}
