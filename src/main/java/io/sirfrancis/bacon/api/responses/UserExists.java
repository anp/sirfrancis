package io.sirfrancis.bacon.api.responses;

public class UserExists {
	private boolean exists;
	private String username;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public boolean exists() {
		return exists;
	}

	public void setExists(boolean exists) {
		this.exists = exists;
	}
}
