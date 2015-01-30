/**
 * Created by Adam on 1/4/2015.
 */

package io.sirfrancis.bacon;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import ru.vyarus.dropwizard.orient.configuration.HasOrientServerConfiguration;
import ru.vyarus.dropwizard.orient.configuration.OrientServerConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class BaconConfiguration extends Configuration implements HasOrientServerConfiguration{
	private static OrientGraphFactory factory;
	@NotNull
	private String dbPath;
	@NotNull
	private String dbBackupPath;
	@NotNull
	private String staticContentPath;
	@NotNull
	private String omdbAPIKey;
	@NotNull
	private String dbRemotePath;
	@NotNull
	@Valid
	private OrientServerConfiguration orientServer;
	@Valid
	@NotNull
	@JsonProperty
	private JerseyClientConfiguration httpClient = new JerseyClientConfiguration();
	@NotNull
	private int dbPoolMin;
	@NotNull
	private int dbPoolMax;

	public static OrientGraphFactory getFactory() {
		return factory;
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
		return dbPath;
	}

	@JsonProperty("db-path")
	public void setDbPath(String dbPath) {
		this.dbPath = dbPath;
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

	@JsonProperty("static-content-path")
	public String getStaticContentPath() {
		return staticContentPath;
	}

	@JsonProperty("static-content-path")
	public void setStaticContentPath(String staticContentPath) {
		this.staticContentPath = staticContentPath;
	}

	@JsonProperty("db-binary-conn-path")
	public String getDbRemotePath() {
		return dbRemotePath;
	}

	@JsonProperty("db-binary-conn-path")
	public void setDbRemotePath(String dbRemotePath) {
		this.dbRemotePath = dbRemotePath;
	}

	public JerseyClientConfiguration getJerseyClientConfiguration() {
		return httpClient;
	}

	public void initFactory() {
		factory = new OrientGraphFactory(dbPath).setupPool(dbPoolMin, dbPoolMax);
	}
}