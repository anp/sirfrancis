package io.sirfrancis.bacon.core;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

/**
 * Created by adam on 1/26/15.
 */
public class Recommendation implements Comparable<Recommendation>{
	@NotNull
	private Movie movie;

	@NotNull
	private int score;

	public Recommendation(Movie movie, int score) {
		this.movie = movie;
		this.score = score;
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
	public int getScore() {
		return score;
	}

	@JsonProperty
	public void setScore(int score) {
		this.score = score;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Recommendation) {
			return movie.equals(((Recommendation) other).getMovie());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return movie.hashCode();
	}

	@Override
	public int compareTo(Recommendation other) {
		return score - other.getScore();
	}
}
