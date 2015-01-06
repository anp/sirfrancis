/**
 * Created by Adam on 1/4/2015.
 */

package io.sirfrancis.bacon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.constraints.NotNull;

public class BaconConfiguration extends Configuration{
	@NotNull
	private String dbPath;

	@NotNull
	private String dbBackupPath;

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
}
