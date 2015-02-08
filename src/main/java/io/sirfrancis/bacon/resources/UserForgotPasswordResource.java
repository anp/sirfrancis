package io.sirfrancis.bacon.resources;

import com.codahale.metrics.annotation.Timed;
import io.sirfrancis.bacon.db.UserDAO;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/user/password/forgot/{email}/")
public class UserForgotPasswordResource {
	private UserDAO dao;

	public UserForgotPasswordResource(UserDAO dao) {
		this.dao = dao;
	}

	@POST
	@Timed
	public void forgotPassword(@PathParam("email") String email) {
		dao.forgotPassword(email);
	}
}
