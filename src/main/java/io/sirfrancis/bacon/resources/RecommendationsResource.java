package io.sirfrancis.bacon.resources;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import io.sirfrancis.bacon.core.Recommendation;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.RecommendationsDAO;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * Created by adam on 1/26/15.
 */

@Path("/recommendations/{numReturned}")
@Produces(MediaType.APPLICATION_JSON)
public class RecommendationsResource {
	private RecommendationsDAO dao;

	public RecommendationsResource(RecommendationsDAO dao) {
		this.dao = dao;
	}

	@GET
	@Timed
	public List<Recommendation> getRecommendations(@Auth User user) {
		return dao.getRecommendations(user);
	}

	@POST
	@Timed
	public List<Recommendation> buildRecommedations(@PathParam("numReturned") int numRecommendations,
													@Auth User user) {
		dao.buildRecommendations(user, numRecommendations);
		return dao.getRecommendations(user);
	}

}
