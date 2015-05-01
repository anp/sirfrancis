package io.sirfrancis.bacon.auth;

import org.junit.Test;

import java.security.SecureRandom;

import static org.junit.Assert.assertFalse;

/**
 * Created by adam on 4/30/15.
 */
public class SaltedHasherTest {

	@Test
	public void testSetSecretKeyFactory() throws Exception {
		SaltedHasher.setSecretKeyFactory();

		SecureRandom random = new SecureRandom();
		byte[] salt = new byte[16];
		random.nextBytes(salt);

		SaltedHasher salter = new SaltedHasher("firstTestPassword", salt);

		//the hasher default to all 0's for the hash if the keyspec is bad
		for (byte b : salter.getHash()) {
			assertFalse(b == 0);
		}
	}
}
