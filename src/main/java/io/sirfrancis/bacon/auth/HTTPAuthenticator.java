package io.sirfrancis.bacon.auth;

import com.google.common.base.Optional;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.core.User;
import io.sirfrancis.bacon.db.UserDAO;

public class HTTPAuthenticator implements Authenticator<BasicCredentials, User> {


	@Override
	public Optional<User> authenticate(BasicCredentials creds) throws AuthenticationException {
		UserDAO dao = new UserDAO(BaconConfiguration.getFactory(),
				BaconConfiguration.getMaxDbRetries());

		User user = dao.getUser(creds.getUsername());

		if (user != null) {
			SaltedHasher hasher = new SaltedHasher(creds.getPassword(), user.getSalt());

			if (hashEquals(hasher.getHash(), (user.getHash()))) {
				return Optional.of(user);
			}
		}
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