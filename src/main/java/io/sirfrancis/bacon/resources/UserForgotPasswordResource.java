package io.sirfrancis.bacon.resources;

import com.codahale.metrics.annotation.Timed;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.db.UserDAO;
import io.sirfrancis.bacon.mailers.ChangePasswordMailer;
import io.sirfrancis.bacon.util.StringRandomizer;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/user/password/forgot/{email}/")
public class UserForgotPasswordResource {
	private UserDAO dao;
	private ChangePasswordMailer changePasswordMailer;
	private StringRandomizer randomizer;
	public UserForgotPasswordResource(UserDAO dao) {
		randomizer = new StringRandomizer(30);

		this.dao = dao;
		changePasswordMailer = new ChangePasswordMailer(BaconConfiguration.buildSendGrid(),
				BaconConfiguration.getPasswordChangeConfirmURL());
	}

	@POST
	@Timed
	public void forgotPassword(@PathParam("email") String email) {
		String confirmationKey = randomizer.nextString();
		dao.forgotPassword(email, confirmationKey);

		changePasswordMailer.sendPasswordChangeConfirmationEmail(email, confirmationKey);
	}
}
