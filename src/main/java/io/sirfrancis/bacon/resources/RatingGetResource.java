package io.sirfrancis.bacon.resources;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import io.sirfrancis.bacon.core.Rating;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.RatingDAO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * Created by adam on 1/25/15.
 */

@Path("/ratings/")
@Produces(MediaType.APPLICATION_JSON)
public class RatingGetResource {
	RatingDAO dao;

	public RatingGetResource(RatingDAO dao) {
		this.dao = dao;
	}

	@GET
	@Timed
	public List<Rating> getRatings(@Auth User user) {
		return dao.getRatings(user);
	}
}
