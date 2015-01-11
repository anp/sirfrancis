package io.sirfrancis.bacon.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by anp68 on 1/10/2015.
 */
public class TraktRating {
	@NotNull
	@NotEmpty
	@Valid
	private String imdbID;

	@NotNull
	@NotEmpty
	@Valid
	private int rating;

	@JsonProperty
	public String getImdbID() {
		return getImdbID();
	}

	@JsonProperty
	public int getRating() {
		return rating;
	}

	@JsonProperty
	public void setImdbID(String id) {
		imdbID = id;
	}

	@JsonProperty
	public void setRating(int rating) {
		this.rating = rating;
	}
}
