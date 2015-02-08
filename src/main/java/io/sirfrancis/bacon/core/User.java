package io.sirfrancis.bacon.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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

}