package io.sirfrancis.bacon.auth;

import com.google.common.base.Optional;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.UserDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPAuthenticator implements Authenticator<BasicCredentials, User> {
	private static final Logger LOGGER = LoggerFactory.getLogger(HTTPAuthenticator.class);
	private UserDAO dao;

	public HTTPAuthenticator(UserDAO dao) {
		this.dao = dao;
	}

	public static boolean hashEquals(byte[] first, byte[] second) {
		if (first == null || second == null)
			return false;

		if (first.length != second.length)
			return false;

		for (int i = 0; i < first.length; i++) {
			if (first[i] != second[i])
				return false;
		}

		return true;
	}

	@Override
	public Optional<User> authenticate(BasicCredentials creds) throws AuthenticationException {

		String username = creds.getUsername();
		User user = dao.getUser(username);
		Optional<User> returned = Optional.absent();

		if (user != null) {
			LOGGER.debug("Authenticator has found account for " + username);
			SaltedHasher hasher = new SaltedHasher(creds.getPassword(), user.getSalt());

			if (hashEquals(hasher.getHash(), (user.getHash()))) {
				LOGGER.info("Successfully authenticated password for " + username);
				returned = Optional.of(user);
			} else {
				LOGGER.info("Failed to authenticate " + username);
			}
		} else {
			LOGGER.debug("Failed to locate user " + username);
		}
		return returned;
	}
}
