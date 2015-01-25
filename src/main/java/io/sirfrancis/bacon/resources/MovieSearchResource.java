package io.sirfrancis.bacon.resources;

import com.codahale.metrics.annotation.Timed;
import io.sirfrancis.bacon.core.Movie;
import io.sirfrancis.bacon.db.MovieDAO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * Created by adam on 1/24/15.
 */

@Path("/search/movies/{query}/")
@Produces(MediaType.APPLICATION_JSON)

public class MovieSearchResource {
	MovieDAO dao;

	public MovieSearchResource(MovieDAO dao) {
		this.dao = dao;
	}

	@GET
	@Timed
	public List<Movie> searchMovies(@PathParam("query") String query) {
		System.out.println("Searching for movies that match: " + query);
		return dao.searchMovies(query, 10);
	}
}
