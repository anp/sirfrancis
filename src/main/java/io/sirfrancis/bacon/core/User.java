package io.sirfrancis.bacon.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.sirfrancis.bacon.auth.HTTPAuthenticator;

import javax.validation.constraints.NotNull;

public class User {
	@NotNull
	private String username;

	@NotNull
	private byte[] hash;

	@NotNull
	private byte[] salt;
	@NotNull
	private boolean emailConfirmed;

	public User(String username, byte[] salt, byte[] hash) {
		this.username = username;
		this.salt = salt;
		this.hash = hash;
	}

	@JsonProperty
	public boolean isEmailConfirmed() {
		return emailConfirmed;
	}

	@JsonProperty
	public void setEmailConfirmed(boolean emailConfirmed) {
		this.emailConfirmed = emailConfirmed;
	}

	@JsonProperty
	public String getUsername() {
		return username;
	}

	@JsonProperty
	public void setUsername(String username) {
		this.username = username;
	}

	@JsonIgnore
	public byte[] getHash() {
		return hash;
	}

	@JsonIgnore
	public void setHash(byte[] hash) {
		this.hash = hash;
	}

	@JsonIgnore
	public byte[] getSalt() {
		return salt;
	}

	@JsonIgnore
	public void setSalt(byte[] salt) {
		this.salt = salt;
	}

	public boolean equals(Object other) {
		if (other instanceof User) {
			User otherUser = (User) other;

			return username.equals(otherUser.getUsername()) &&
					HTTPAuthenticator.hashEquals(hash, otherUser.getHash()) &&
					HTTPAuthenticator.hashEquals(salt, otherUser.getSalt()) &&
					emailConfirmed == otherUser.isEmailConfirmed();
		}
		return false;
	}
}
