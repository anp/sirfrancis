package io.sirfrancis.bacon;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.cache.CacheBuilderSpec;
import io.dropwizard.Configuration;

import javax.validation.constraints.NotNull;

public class BaconConfiguration extends Configuration {


	@NotNull
	private static String accountCreationConfirmURL;
	@NotNull
	private static String passwordChangeConfirmURL;
	@NotNull
	private static String sendgridUsername;
	@NotNull
	private static String sendgridPassword;
	@NotNull
	private static String omdbAPIKey;
	@NotNull
	private static String omdbDownloadURL;
	@NotNull
	private static String orientConnectionString;
	@NotNull
	private static String orientUsername;
	@NotNull
	private static String orientPassword;
	@NotNull
	private static int dbPoolMin;
	@NotNull
	private static int dbPoolMax;
	@NotNull
	private static int maxRetries;
	@NotNull
	private static String omdbPosterURL;
	@NotNull
	private CacheBuilderSpec authenticationCachePolicy;

	@JsonProperty("orient-username")
	public static String getOrientUsername() {
		return orientUsername;
	}

	@JsonProperty("orient-username")
	public void setOrientUsername(String orientUsername) {
		BaconConfiguration.orientUsername = orientUsername;
	}

	@JsonProperty("orient-password")
	public static String getOrientPassword() {
		return orientPassword;
	}

	@JsonProperty("orient-password")
	public void setOrientPassword(String orientPassword) {
		BaconConfiguration.orientPassword = orientPassword;
	}

	@JsonProperty("omdb-poster-url")
	public static String getOmdbPosterURL() {
		return omdbPosterURL.replace("%k", omdbAPIKey);
	}

	@JsonProperty("omdb-poster-url")
	public void setOmdbPosterURL(String omdbPosterURL) {
		BaconConfiguration.omdbPosterURL = omdbPosterURL;
	}

	@JsonProperty("max-db-retries")
	public static int getMaxRetries() {
		return maxRetries;
	}

	@JsonProperty("max-db-retries")
	public void setMaxRetries(int maxRetries) {
		BaconConfiguration.maxRetries = maxRetries;
	}

	@JsonProperty("db-pool-min")
	public static int getDbPoolMax() {
		return dbPoolMax;
	}

	@JsonProperty("db-pool-max")
	public void setDbPoolMax(int dbPoolMax) {
		BaconConfiguration.dbPoolMax = dbPoolMax;
	}

	@JsonProperty("db-pool-min")
	public static int getDbPoolMin() {
		return dbPoolMin;
	}

	@JsonProperty("db-pool-min")
	public void setDbPoolMin(int dbPoolMin) {
		BaconConfiguration.dbPoolMin = dbPoolMin;
	}

	@JsonProperty("orient-conn-string")
	public static String getOrientConnectionString() {
		return orientConnectionString;
	}

	@JsonProperty("orient-conn-string")
	public void setOrientConnectionString(String orientConnectionString) {
		BaconConfiguration.orientConnectionString = orientConnectionString;
	}

	@JsonProperty("password-change-confirm-url")
	public static String getPasswordChangeConfirmURL() {
		return passwordChangeConfirmURL;
	}

	@JsonProperty("password-change-confirm-url")
	public void setPasswordChangeConfirmURL(String passwordChangeConfirmURL) {
		BaconConfiguration.passwordChangeConfirmURL = passwordChangeConfirmURL;
	}

	@JsonProperty("sendgrid-username")
	public static String getSendgridUsername() {
		return sendgridUsername;
	}

	@JsonProperty("sendgrid-username")
	public void setSendgridUsername(String sendgridUsername) {
		BaconConfiguration.sendgridUsername = sendgridUsername;
	}

	@JsonProperty("sendgrid-password")
	public static String getSendgridPassword() {
		return sendgridPassword;
	}

	@JsonProperty("sendgrid-password")
	public void setSendgridPassword(String sendgridPassword) {
		BaconConfiguration.sendgridPassword = sendgridPassword;
	}

	@JsonProperty("account-creation-confirm-url")
	public static String getAccountCreationConfirmURL() {
		return accountCreationConfirmURL;
	}

	@JsonProperty("account-creation-confirm-url")
	public void setAccountCreationConfirmURL(String accountCreationConfirmURL) {
		BaconConfiguration.accountCreationConfirmURL = accountCreationConfirmURL;
	}

	@JsonProperty("omdb-download-url")
	public static String getOMDBDownloadURL() {
		return omdbDownloadURL;
	}

	@JsonProperty("omdb-download-url")
	public void setOMDBDownloadURL(String omdbDownloadURL) {
		BaconConfiguration.omdbDownloadURL = omdbDownloadURL;
	}

	@JsonProperty("authenticationCachePolicy")
	public CacheBuilderSpec getAuthenticationCachePolicy() {
		return authenticationCachePolicy;
	}

	@JsonProperty("authenticationCachePolicy")
	public void setAuthenticationCachePolicy(CacheBuilderSpec authenticationCachePolicy) {
		this.authenticationCachePolicy = authenticationCachePolicy;
	}

	@JsonProperty("omdb-api-key")
	public void setOmdbAPIKey(String newKey) {
		omdbAPIKey = newKey;
	}
}