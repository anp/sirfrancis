package io.sirfrancis.bacon.resources;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import io.sirfrancis.bacon.core.Movie;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.QuizDAO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/quiz/{perPage}/{pageNumber}")
@Produces(MediaType.APPLICATION_JSON)
public class QuizResource {
	QuizDAO dao;

	public QuizResource(QuizDAO dao) {
		this.dao = dao;
	}

	@GET
	@Timed
	public List<Movie> getQuizPage(@Auth User user,
								   @PathParam("perPage") int perPage,
								   @PathParam("pageNumber") int pageNumber) {

		if (perPage < 1 || pageNumber < 0) throw new IllegalArgumentException("Invalid parameters");

		return dao.getQuizMovies(perPage, pageNumber, user);
	}
}
