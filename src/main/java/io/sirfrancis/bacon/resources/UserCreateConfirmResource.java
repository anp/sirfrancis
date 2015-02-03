package io.sirfrancis.bacon.resources;

import com.codahale.metrics.annotation.Timed;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.UserDAO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/user/create/confirm/{email}/{confirmationKey}")
@Produces("application/json")
public class UserCreateConfirmResource {
	private UserDAO dao;

	public UserCreateConfirmResource(UserDAO dao) {
		this.dao = dao;
	}

	@GET
	@Timed
	public User confirmUserCreation(@PathParam("email") String email,
									@PathParam("confirmationKey") String confirmKey) {
		return dao.confirmUserCreation(email, confirmKey);
	}
}
