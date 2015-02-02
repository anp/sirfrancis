package io.sirfrancis.bacon;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import io.dropwizard.Configuration;
import ru.vyarus.dropwizard.orient.configuration.HasOrientServerConfiguration;
import ru.vyarus.dropwizard.orient.configuration.OrientServerConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class BaconConfiguration extends Configuration implements HasOrientServerConfiguration {
	private static OrientGraphFactory factory;
	@NotNull
	private static int maxDbRetries;
	@NotNull
	private static int warmupIterations;
	@NotNull
	private String dbLocalPath;
	@NotNull
	private String dbBackupPath;
	@NotNull
	private String dbRemotePath;
	@NotNull
	private String omdbAPIKey;
	@NotNull
	private String omdbDBEmail;
	@NotNull
	@Valid
	private OrientServerConfiguration orientServer;
	@NotNull
	private int dbPoolMin;
	@NotNull
	private int dbPoolMax;
	@NotNull
	private boolean restrictUserCreation;
	@NotNull
	private String amazonPrefix;

	@JsonProperty("max-db-write-retries")
	public static int getMaxDbRetries() {
		return maxDbRetries;
	}

	@JsonProperty("max-db-write-retries")
	public void setMaxDbRetries(int maxDbRetries) {
		BaconConfiguration.maxDbRetries = maxDbRetries;
	}

	@JsonProperty("warmup-iterations")
	public static int getWarmupIterations() {
		return warmupIterations;
	}

	@JsonProperty("warmup-iterations")
	public void setWarmupIterations(int warmupIterations) {
		BaconConfiguration.warmupIterations = warmupIterations;
	}

	public static OrientGraphFactory getFactory() {
		return factory;
	}

	@JsonProperty("omdb-download-url")
	public String getOmdbDBEmail() {
		return omdbDBEmail;
	}

	@JsonProperty("omdb-download-url")
	public void setOmdbDBEmail(String omdbDBEmail) {
		this.omdbDBEmail = omdbDBEmail;
	}

	@JsonProperty("amazon-url-prefix")
	public String getAmazonPrefix() {
		return this.amazonPrefix;
	}

	@JsonProperty("amazon-url-prefix")
	public void setAmazonPrefix(String amazonPrefix) {
		this.amazonPrefix = amazonPrefix;
	}

	@JsonProperty("restrict-create-user")
	public boolean isRestrictUserCreation() {
		return restrictUserCreation;
	}

	@JsonProperty("restrict-create-user")
	public void setRestrictUserCreation(boolean restrictUserCreation) {
		this.restrictUserCreation = restrictUserCreation;
	}

	@JsonProperty("db-pool-min")
	public int getDbPoolMin() {
		return dbPoolMin;
	}

	@JsonProperty("db-pool-min")
	public void setDbPoolMin(int dbPoolMin) {
		this.dbPoolMin = dbPoolMin;
	}

	@JsonProperty("db-pool-max")
	public int getDbPoolMax() {
		return dbPoolMax;
	}

	@JsonProperty("db-pool-max")
	public void setDbPoolMax(int dbPoolMax) {
		this.dbPoolMax = dbPoolMax;
	}

	@JsonProperty("db-path")
	public String getDBPath() {
		return dbLocalPath;
	}

	@JsonProperty("db-path")
	public void setDbLocalPath(String dbLocalPath) {
		this.dbLocalPath = dbLocalPath;
	}

	@JsonProperty("backup-path")
	public String getDbBackupPath() {
		return dbBackupPath;
	}

	@JsonProperty("backup-path")
	public void setDbBackupPath(String dbBackupPath) {
		this.dbBackupPath = dbBackupPath;
	}

	@JsonProperty("omdb-api-key")
	public String getOMDBAPIKey() {
		return omdbAPIKey;
	}

	@JsonProperty("omdb-api-key")
	public void setOmdbAPIKey(String newKey) {
		omdbAPIKey = newKey;
	}

	@Override
	public OrientServerConfiguration getOrientServerConfiguration() {
		return orientServer;
	}

	@JsonProperty("orient-server")
	void setOrientServer(OrientServerConfiguration orientServer) {
		this.orientServer = orientServer;
	}

	@JsonProperty("db-binary-conn-path")
	public String getDbRemotePath() {
		return dbRemotePath;
	}

	@JsonProperty("db-binary-conn-path")
	public void setDbRemotePath(String dbRemotePath) {
		this.dbRemotePath = dbRemotePath;
	}

	public void initFactory() {
		factory = new OrientGraphFactory(dbLocalPath).setupPool(dbPoolMin, dbPoolMax);
	}
}