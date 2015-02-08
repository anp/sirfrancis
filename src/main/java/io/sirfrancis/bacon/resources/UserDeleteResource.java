package io.sirfrancis.bacon.resources;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.UserDAO;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by Adam on 1/19/2015.
 */

@Path("/user/delete/")
@Produces(MediaType.APPLICATION_JSON)
public class UserDeleteResource {
	UserDAO dao;

	public UserDeleteResource(UserDAO dao) {
		this.dao = dao;
	}

	@POST
	@Timed
	public boolean deleteUser(@Auth User user) {
		return dao.deleteUser(user);
	}
}
