package io.sirfrancis.bacon.resources;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import io.sirfrancis.bacon.core.Movie;
import io.sirfrancis.bacon.core.User;
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

@Path("/search/movies/{query}/{numResults}")
@Produces(MediaType.APPLICATION_JSON)

public class MovieSearchResource {
	MovieDAO dao;

	public MovieSearchResource(MovieDAO dao) {
		this.dao = dao;
	}

	@GET
	@Timed
	public List<Movie> searchMovies(@PathParam("query") String query,
									@PathParam("numResults") int numResults,
									@Auth User user) {
		return dao.searchMovies(query, numResults);
	}
}
