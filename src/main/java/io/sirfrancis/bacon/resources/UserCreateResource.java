package io.sirfrancis.bacon.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.sendgrid.SendGridException;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.UserDAO;
import io.sirfrancis.bacon.mailers.NewUserMailer;
import io.sirfrancis.bacon.util.StringRandomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/user/create/new/{email}/{password}")
@Produces(MediaType.APPLICATION_JSON)
public class UserCreateResource {
	private static Logger log = LoggerFactory.getLogger(UserCreateResource.class);

	private NewUserMailer newUserMailer;
	private UserDAO dao;
	private StringRandomizer randomizer;


	public UserCreateResource(UserDAO dao) {
		newUserMailer = new NewUserMailer(BaconConfiguration.buildSendGrid(),
				BaconConfiguration.getAccountCreationConfirmURL());

		this.dao = dao;

		randomizer = new StringRandomizer(30);
	}


	@POST
	@Timed
	public Optional<User> createUser(@PathParam("email") String email,
									 @PathParam("password") String password) {

		if (password.length() < 6) throw new IllegalArgumentException("Password too short.");

		String confirmationKey = randomizer.nextString();

		Optional<User> createdUser = dao.createUser(email, password, confirmationKey);

		for (int j = 0; j < 3; j++) {
			try {
				newUserMailer.sendAccountCreationConfirmationEmail(email, confirmationKey);
			} catch (SendGridException sge) {
				if (j == 2) {
					log.error("Unable to send new user confirmation email for " + email + ": " + sge.getMessage(), sge);
				}
			}
		}
		return createdUser;
	}
}
