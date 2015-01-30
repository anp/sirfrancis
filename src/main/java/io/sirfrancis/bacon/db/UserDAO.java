package io.sirfrancis.bacon.db;

import com.google.common.base.Optional;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import io.sirfrancis.bacon.auth.SaltedHasher;
import io.sirfrancis.bacon.core.User;

import java.security.SecureRandom;
import java.util.HashMap;

/**
 * Created by Adam on 1/19/2015.
 */
public class UserDAO {
	private OrientGraphFactory factory;

	public UserDAO(OrientGraphFactory factory) {
		this.factory = factory;
	}

	public Optional<User> createUser(String username, String password) {
		OrientGraph graph = factory.getTx();
		Optional<User> returned = Optional.absent();
		try {
			Vertex userVertex = graph.getVertexByKey("User.username", username);
			if (userVertex == null) {
				SecureRandom random = new SecureRandom();

				byte[] salt = new byte[16];
				random.nextBytes(salt);

				SaltedHasher hasher = new SaltedHasher(password, salt);

				HashMap<String, Object> userProps = new HashMap<>();
				userProps.put("username", username);
				userProps.put("salt", hasher.getSalt());
				userProps.put("hash", hasher.getHash());

				userVertex = graph.addVertex("class:User", userProps);

				User user = buildUser(userVertex);
				returned = Optional.of(user);
				graph.commit();
			}
		} finally {
			graph.shutdown();
			return returned;
		}
	}

	public boolean deleteUser(User user) {
		OrientGraph graph = factory.getTx();
		boolean deleted = false;
		try {
			Vertex userVertex = graph.getVertexByKey("User.username", user.getUsername());
			if (userVertex != null) {
				System.out.println("Deleting user.");
				graph.removeVertex(userVertex);
				deleted = true;
			}

			graph.commit();
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
				returnedUser = buildUser(userVertex);
			}
		} finally {
			graph.shutdown();
		}
		return returnedUser;
	}

	public User buildUser(Vertex userVertex) {
		String unameFromDB = userVertex.getProperty("username");
		byte[] saltFromDB = userVertex.getProperty("salt");
		byte[] hashFromDB = userVertex.getProperty("hash");

		return new User(unameFromDB, saltFromDB, hashFromDB);
	}
}
