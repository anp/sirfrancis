package io.sirfrancis.bacon.core;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

/**
 * Created by adam on 1/25/15.
 */
public class Rating {
	@NotNull
	private Movie movie;

	@NotNull
	private int rating;

	public Rating(Movie movie, int rating) {
		this.movie = movie;
		this.rating = rating;
	}

	@JsonProperty
	public Movie getMovie() {
		return movie;
	}

	@JsonProperty
	public void setMovie(Movie movie) {
		this.movie = movie;
	}

	@JsonProperty
	public int getRating() {
		return rating;
	}

	@JsonProperty
	public void setRating(int rating) {
		this.rating = rating;
	}
}
