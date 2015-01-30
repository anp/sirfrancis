package io.sirfrancis.bacon.resources;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import io.sirfrancis.bacon.core.Rating;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.RatingDAO;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by adam on 1/25/15.
 */

@Path("/ratings/ignore/{imdbID}")
@Produces(MediaType.APPLICATION_JSON)
public class RatingIgnoreResource {
	RatingDAO dao;

	public RatingIgnoreResource(RatingDAO dao) {
		this.dao = dao;
	}

	@POST
	@Timed
	public Rating ignoreMovie(@Auth User user, @PathParam("imdbID") String imdbID) {
		return dao.addRating(user, imdbID, 0);
	}
}
