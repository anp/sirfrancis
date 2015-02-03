package io.sirfrancis.bacon.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.UserDAO;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/user/create/new/{email}/{password}")
@Produces(MediaType.APPLICATION_JSON)
public class UserCreateResource {
	UserDAO dao;

	public UserCreateResource(UserDAO dao) {
		this.dao = dao;
	}


	@POST
	@Timed
	public Optional<User> createUser(@PathParam("email") String username,
									 @PathParam("password") String password) {
		return dao.createUser(username,password);
	}
}
