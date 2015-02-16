package io.sirfrancis.bacon.auth;

import com.google.common.base.Optional;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.UserDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPAuthenticator implements Authenticator<BasicCredentials, User> {
	private static final Logger LOGGER = LoggerFactory.getLogger(HTTPAuthenticator.class);

	@Override
	public Optional<User> authenticate(BasicCredentials creds) throws AuthenticationException {
		UserDAO dao = new UserDAO(BaconConfiguration.getFactory());

		String username = creds.getUsername();
		User user = dao.getUser(username);

		if (user != null) {
			SaltedHasher hasher = new SaltedHasher(creds.getPassword(), user.getSalt());

			if (hashEquals(hasher.getHash(), (user.getHash()))) {
				LOGGER.info("Successfully authenticated " + username);
				return Optional.of(user);
			}
		}

		LOGGER.info("Failed to authenticate " + username);
		return Optional.absent();
	}

	public boolean hashEquals(byte[] first, byte[] second) {
		if (first.length != second.length)
			return false;

		for (int i = 0; i < first.length; i++) {
			if (first[i] != second[i])
				return false;
		}

		return true;
	}
}