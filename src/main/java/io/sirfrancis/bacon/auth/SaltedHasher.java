package io.sirfrancis.bacon.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * Created by Adam on 1/19/2015.
 */
public class SaltedHasher {
	private static Logger log = LoggerFactory.getLogger(SaltedHasher.class);
	private static SecretKeyFactory f = null;
	private byte[] salt;
	private byte[] hash;

	public SaltedHasher(String password, byte[] salt) {
		this.salt = salt;

		KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);

		try {
			hash = f.generateSecret(spec).getEncoded();
		} catch (InvalidKeySpecException e) {
			hash = new byte[10];
		}
	}

	public static void setSecretKeyFactory() throws NoSuchAlgorithmException {
		f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
	}

	public byte[] getSalt() {
		return salt;
	}

	public byte[] getHash() {
		return hash;
	}
}
