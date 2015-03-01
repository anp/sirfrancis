package io.sirfrancis.bacon.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * Created by adam on 1/23/15.
 */
public class Movie {
	private long omdbID;
	private String imdbID;
	private String title;
	private String indexTitle;
	private String runtime;
	private String released;
	private String language;
	private String genres;
	private String country;
	private int year;
	private String awards;
	private String mpaaRating;
	private int metascore;
	private double imdbRating;
	private int imdbVotes;
	private String posterURL;
	private double rtRating;
	private int tomatoMeter;
	private int rtNumReviews;
	private int rtNumFreshReviews;
	private int rtNumRottenReviews;
	private String rtConsensus;
	private Set<String> actors;
	private Set<String> directors;
	private Set<String> writers;
	private String updated;
	public Movie(String imdbID, long omdbID, String title) {
		this.imdbID = imdbID;
		this.omdbID = omdbID;
		this.title = title;
	}

	@JsonIgnore
	public String getIndexTitle() {
		return indexTitle;
	}

	@JsonIgnore
	public void setIndexTitle(String indexTitle) {
		this.indexTitle = indexTitle;
	}

	@JsonIgnore
	public String getUpdated() {
		return updated;
	}

	@JsonIgnore
	public void setUpdated(String updated) {
		this.updated = updated;
	}

	@JsonProperty
	public Set<String> getActors() {
		return actors;
	}

	@JsonProperty
	public void setActors(Set<String> actors) {
		this.actors = actors;
	}

	@JsonProperty
	public Set<String> getDirectors() {
		return directors;
	}

	@JsonProperty
	public void setDirectors(Set<String> directors) {
		this.directors = directors;
	}

	@JsonProperty
	public Set<String> getWriters() {
		return writers;
	}

	@JsonProperty
	public void setWriters(Set<String> writers) {
		this.writers = writers;
	}

	@JsonProperty
	public String getGenres() {
		return genres;
	}

	@JsonProperty
	public void setGenres(String genres) {
		this.genres = genres;
	}

	@JsonProperty
	public String getRtConsensus() {
		return rtConsensus;
	}

	@JsonProperty
	public void setRtConsensus(String rtConsensus) {
		this.rtConsensus = rtConsensus;
	}

	@JsonProperty
	public double getRtRating() {
		return rtRating;
	}

	@JsonProperty
	public void setRtRating(double rtRating) {
		this.rtRating = rtRating;
	}

	@JsonProperty
	public int getTomatoMeter() {
		return tomatoMeter;
	}

	@JsonProperty
	public void setTomatoMeter(int tomatoMeter) {
		this.tomatoMeter = tomatoMeter;
	}

	@JsonProperty
	public int getRtNumReviews() {
		return rtNumReviews;
	}

	@JsonProperty
	public void setRtNumReviews(int rtNumReviews) {
		this.rtNumReviews = rtNumReviews;
	}

	@JsonProperty
	public int getRtNumFreshReviews() {
		return rtNumFreshReviews;
	}

	@JsonProperty
	public void setRtNumFreshReviews(int rtNumFreshReviews) {
		this.rtNumFreshReviews = rtNumFreshReviews;
	}

	@JsonProperty
	public int getRtNumRottenReviews() {
		return rtNumRottenReviews;
	}

	@JsonProperty
	public void setRtNumRottenReviews(int rtNumRottenReviews) {
		this.rtNumRottenReviews = rtNumRottenReviews;
	}

	@JsonIgnore
	public long getOmdbID() {
		return omdbID;
	}

	@JsonProperty
	public String getImdbID() {
		return imdbID;
	}

	@JsonProperty
	public void setImdbID(String imdbID) {
		this.imdbID = imdbID;
	}

	@JsonProperty
	public String getTitle() {
		return title;
	}

	@JsonProperty
	public String getRuntime() {
		return runtime;
	}

	@JsonProperty
	public void setRuntime(String runtime) {
		this.runtime = runtime;
	}

	@JsonProperty
	public String getReleased() {
		return released;
	}

	@JsonProperty
	public void setReleased(String released) {
		this.released = released;
	}

	@JsonProperty
	public String getLanguage() {
		return language;
	}

	@JsonProperty
	public void setLanguage(String language) {
		this.language = language;
	}

	@JsonProperty
	public String getCountry() {
		return country;
	}

	@JsonProperty
	public void setCountry(String country) {
		this.country = country;
	}

	@JsonProperty
	public int getYear() {
		return year;
	}

	@JsonProperty
	public void setYear(int year) {
		this.year = year;
	}

	@JsonProperty
	public String getAwards() {
		return awards;
	}

	@JsonProperty
	public void setAwards(String awards) {
		this.awards = awards;
	}

	@JsonProperty
	public String getMpaaRating() {
		return mpaaRating;
	}

	@JsonProperty
	public void setMpaaRating(String mpaaRating) {
		this.mpaaRating = mpaaRating;
	}

	@JsonProperty
	public int getMetascore() {
		return metascore;
	}

	@JsonProperty
	public void setMetascore(int metascore) {
		this.metascore = metascore;
	}

	@JsonProperty
	public double getImdbRating() {
		return imdbRating;
	}

	@JsonProperty
	public void setImdbRating(double imdbRating) {
		this.imdbRating = imdbRating;
	}

	@JsonProperty
	public int getImdbVotes() {
		return imdbVotes;
	}

	@JsonProperty
	public void setImdbVotes(int imdbVotes) {
		this.imdbVotes = imdbVotes;
	}

	@JsonProperty
	public String getPosterURL() {
		return posterURL;
	}

	@JsonProperty
	public void setPosterURL(String posterURL) {
		this.posterURL = posterURL;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Movie) {
			return ((Movie) other).getImdbID().equals(this.getImdbID());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return imdbID.hashCode();
	}
}
