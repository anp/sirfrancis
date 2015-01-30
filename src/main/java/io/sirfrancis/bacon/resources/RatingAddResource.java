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

@Path("/ratings/add/{imdbID}/{rating}")
@Produces(MediaType.APPLICATION_JSON)
public class RatingAddResource {
	RatingDAO dao;

	public RatingAddResource(RatingDAO dao) {
		this.dao = dao;
	}

	@POST
	@Timed
	public Rating addRating(@Auth User user, @PathParam("imdbID") String imdbID, @PathParam("rating") int rating) {
		return dao.addRating(user, imdbID, rating);
	}
}
