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

	@JsonProperty("dbpath")
	public String getDBPath() {
		return dbPath;
	}

	@JsonProperty("dbpath")
	public void setDbPath(String dbPath) {
		this.dbPath = dbPath;
	}
}
