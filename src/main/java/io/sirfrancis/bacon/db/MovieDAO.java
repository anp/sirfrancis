package io.sirfrancis.bacon.db;

import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import io.sirfrancis.bacon.core.Movie;

import java.util.LinkedList;
import java.util.List;

public class MovieDAO {
	private OrientGraphFactory factory;

	public MovieDAO(OrientGraphFactory factory) {
		this.factory = factory;
	}

	public List<Movie> searchMovies(String rawSearch, int maxNumberOfResults) {
		OrientGraph graph = factory.getTx();
		LinkedList<Movie> searchResults = new LinkedList<>();

		try {
			String luceneSearch = rawSearch.replaceAll("[']", "").replaceAll("[^A-Za-z0-9]", " ").toLowerCase();

			String baseQuery = "select from Movie where indexTitle LUCENE ? LIMIT " + maxNumberOfResults;
			Iterable<Vertex> results = graph.command(new OCommandSQL(baseQuery))
					.execute(luceneSearch);

			for (Vertex v : results) {
				searchResults.add(buildMovie(v));
			}
		} finally {
			graph.shutdown();
		}

		return searchResults;
	}

	public Movie buildMovie(Vertex movieVertex) {
		OrientGraph graph = factory.getTx();
		Movie thisMovie;
		try {
			Vertex freshMovieVertex = graph.getVertex(movieVertex.getId());

			long omdbID = freshMovieVertex.getProperty("omdbID");
			String imdbID = freshMovieVertex.getProperty("imdbID");
			String title = freshMovieVertex.getProperty("title");
			String runtime = freshMovieVertex.getProperty("runtime");
			String released = freshMovieVertex.getProperty("released");
			String language = freshMovieVertex.getProperty("language");
			String country = freshMovieVertex.getProperty("country");
			String genres = freshMovieVertex.getProperty("genres");
			int year = freshMovieVertex.getProperty("year");
			String awards = freshMovieVertex.getProperty("awards");
			String mpaaRating = freshMovieVertex.getProperty("mpaaRating");
			int metascore = freshMovieVertex.getProperty("metascore");
			double imdbRating = freshMovieVertex.getProperty("imdbRating");
			int imdbVotes = freshMovieVertex.getProperty("imdbVotes");
			String posterURL = freshMovieVertex.getProperty("posterURL");

			double rtRating = freshMovieVertex.getProperty("rtRating");
			int tomatoMeter = freshMovieVertex.getProperty("rtTomatoMeter");
			int rtNumReviews = freshMovieVertex.getProperty("rtNumReviews");
			int rtNumFreshReviews = freshMovieVertex.getProperty("rtNumFreshReviews");
			int rtNumRottenReviews = freshMovieVertex.getProperty("rtNumRottenReviews");
			String rtConsensus = freshMovieVertex.getProperty("rtConsensus");

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
			thisMovie.setPosterURL(posterURL);
			thisMovie.setRtRating(rtRating);
			thisMovie.setTomatoMeter(tomatoMeter);
			thisMovie.setRtNumReviews(rtNumReviews);
			thisMovie.setRtNumFreshReviews(rtNumFreshReviews);
			thisMovie.setRtNumRottenReviews(rtNumRottenReviews);
			thisMovie.setRtConsensus(rtConsensus);
		} finally {
			graph.shutdown();
		}

		return thisMovie;
	}
}
