package io.sirfrancis.bacon.db;

import com.google.common.base.Optional;
import com.orientechnologies.orient.core.exception.OTransactionException;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.auth.SaltedHasher;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.mailers.ChangePasswordMailer;
import io.sirfrancis.bacon.mailers.NewUserMailer;
import io.sirfrancis.bacon.util.StringRandomizer;

import java.security.SecureRandom;
import java.util.HashMap;

public class UserDAO {
	private OrientGraphFactory factory;
	private int maxRetries;
	private SecureRandom random;
	private StringRandomizer randomizer;
	private NewUserMailer newUserMailer;
	private ChangePasswordMailer changePasswordMailer;

	public UserDAO(OrientGraphFactory factory, int maxRetries) {
		String sendgridUsername = BaconConfiguration.getSendgridUsername();
		String sendgridPassword = BaconConfiguration.getSendgridPassword();

		this.factory = factory;
		this.maxRetries = maxRetries;
		random = new SecureRandom();
		randomizer = new StringRandomizer(30);
		newUserMailer = new NewUserMailer(sendgridUsername,
				sendgridPassword,
				BaconConfiguration.getAccountCreationConfirmURL());

		changePasswordMailer = new ChangePasswordMailer(sendgridUsername, sendgridPassword,
				BaconConfiguration.getPasswordChangeConfirmURL());
	}

	public Optional<User> createUser(String email, String password) {
		OrientGraph graph = factory.getTx();
		Optional<User> returned = Optional.absent();

		for (int i = 0; i < maxRetries; i++) {
			try {
				Vertex userVertex = graph.getVertexByKey("User.username", email);
				if (userVertex == null) {
					if (password.length() < 8) throw new IllegalArgumentException("Password too short.");
					byte[] salt = new byte[16];
					random.nextBytes(salt);

					SaltedHasher hasher = new SaltedHasher(password, salt);

					HashMap<String, Object> userProps = new HashMap<>();
					userProps.put("username", email);
					userProps.put("email", email);
					userProps.put("salt", hasher.getSalt());
					userProps.put("hash", hasher.getHash());

					String confirmationKey = randomizer.nextString();
					userProps.put("emailConfirmed", false);
					userProps.put("emailConfirmKey", confirmationKey);

					newUserMailer.sendAccountCreationConfirmationEmail(email, confirmationKey);

					userVertex = graph.addVertex("class:User", userProps);

					User user = buildUser(userVertex);
					returned = Optional.of(user);
					graph.commit();
					break;
				} else {
					break;
				}
			} catch (OTransactionException e) {
				//MVCC handling will be restarted if there's an issue
			} catch (RuntimeException r) {
				break;
			}
		}

		graph.shutdown();
		return returned;
	}

	public boolean deleteUser(User user) {
		OrientGraph graph = factory.getTx();
		boolean deleted = false;
		try {
			for (int i = 0; i < maxRetries; i++) {
				Vertex userVertex = graph.getVertexByKey("User.username", user.getUsername());
				if (userVertex != null) {
					graph.removeVertex(userVertex);
					deleted = true;
				}

				graph.commit();
			}
		} catch (OTransactionException e) {
			//let retry loop try this again
		} finally {
			graph.shutdown();
		}
		return deleted;
	}

	public User getUser(String username) {
		OrientGraph graph = factory.getTx();
		User returnedUser = null;
		try {
			Vertex userVertex = graph.getVertexByKey("User.username", username);
			if (userVertex != null) {
				boolean confirmed = userVertex.getProperty("emailConfirmed");

				if (confirmed)
					returnedUser = buildUser(userVertex);
			}
		} finally {
			graph.shutdown();
		}
		return returnedUser;
	}

	public User confirmUserCreation(String email, String confirmKey) {
		OrientGraph graph = factory.getTx();
		try {
			Vertex userVertex = graph.getVertexByKey("User.username", email);
			if (userVertex != null) {
				String storedConfirmKey = userVertex.getProperty("emailConfirmKey");
				if (storedConfirmKey.equals(confirmKey)) {
					userVertex.setProperty("emailConfirmed", true);
				}
			}
		} finally {
			graph.shutdown();
		}
		return getUser(email);
	}

	public User forgotPassword(String email) {
		OrientGraph graph = factory.getTx();
		try {
			Vertex userVertex = graph.getVertexByKey("User.username", email);

			String confirmationKey = randomizer.nextString();
			userVertex.setProperty("passwordChangeKey", confirmationKey);

			changePasswordMailer.sendPasswordChangeConfirmationEmail(email, confirmationKey);
		} finally {
			graph.shutdown();
		}
		return getUser(email);
	}

	public User confirmPasswordChange(String email, String newPassword, String confirmKey) {
		OrientGraph graph = factory.getTx();
		try {
			Vertex userVertex = graph.getVertexByKey("User.username", email);
			if (userVertex != null) {
				String storedConfirmKey = userVertex.getProperty("passwordChangeKey");
				if (storedConfirmKey.equals(confirmKey)) {
					byte[] salt = userVertex.getProperty("salt");
					SaltedHasher newHasher = new SaltedHasher(newPassword, salt);
					userVertex.setProperty("hash", newHasher.getHash());
				}
			}
		} finally {
			graph.shutdown();
		}
		return getUser(email);
	}

	public User buildUser(Vertex userVertex) {
		String unameFromDB = userVertex.getProperty("username");
		byte[] saltFromDB = userVertex.getProperty("salt");
		byte[] hashFromDB = userVertex.getProperty("hash");
		User built = new User(unameFromDB, saltFromDB, hashFromDB);
		built.setEmailConfirmed(userVertex.getProperty("emailConfirmed"));
		return built;
	}
}
