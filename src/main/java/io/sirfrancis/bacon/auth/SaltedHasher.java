package io.sirfrancis.bacon.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * Created by Adam on 1/19/2015.
 */
public class SaltedHasher {
	private static SecretKeyFactory f = null;
	private byte[] salt;
	private byte[] hash;

	public SaltedHasher(String password, byte[] salt) {
		this.salt = salt;

		if (f == null) {
			setSecretKeyFactory();
		}

		KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);

		try {
			hash = f.generateSecret(spec).getEncoded();
		} catch (InvalidKeySpecException e) {
			hash = new byte[10];
		}
	}

	private void setSecretKeyFactory() {
		try {
			f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			return;
		} catch (NoSuchAlgorithmException nsae) {
			System.err.println("NO SHA512 PBKDF2 algo.");
		}
	}

	public byte[] getSalt() {
		return salt;
	}

	public byte[] getHash() {
		return hash;
	}
}
