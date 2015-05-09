package io.sirfrancis.bacon.core;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Set;

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
	private double rottenTomatoRating;
	private int rottenTomatoMeter;
	private int rottenTomatoesNumReviews;
	private int rottenTomatoesNumFreshReviews;
	private int rottenTomatoesNumRottenReviews;
	private String rottenTomatoesConsensus;
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


	public Set<String> getActors() {
		return actors;
	}


	public void setActors(Set<String> actors) {
		this.actors = actors;
	}


	public Set<String> getDirectors() {
		return directors;
	}


	public void setDirectors(Set<String> directors) {
		this.directors = directors;
	}


	public Set<String> getWriters() {
		return writers;
	}


	public void setWriters(Set<String> writers) {
		this.writers = writers;
	}


	public String getGenres() {
		return genres;
	}


	public void setGenres(String genres) {
		this.genres = genres;
	}


	public String getRottenTomatoesConsensus() {
		return rottenTomatoesConsensus;
	}


	public void setRottenTomatoesConsensus(String rottenTomatoesConsensus) {
		this.rottenTomatoesConsensus = rottenTomatoesConsensus;
	}


	public double getRottenTomatoRating() {
		return rottenTomatoRating;
	}


	public void setRottenTomatoRating(double rottenTomatoRating) {
		this.rottenTomatoRating = rottenTomatoRating;
	}


	public int getRottenTomatoMeter() {
		return rottenTomatoMeter;
	}


	public void setRottenTomatoMeter(int rottenTomatoMeter) {
		this.rottenTomatoMeter = rottenTomatoMeter;
	}


	public int getRottenTomatoesNumReviews() {
		return rottenTomatoesNumReviews;
	}


	public void setRottenTomatoesNumReviews(int rottenTomatoesNumReviews) {
		this.rottenTomatoesNumReviews = rottenTomatoesNumReviews;
	}


	public int getRottenTomatoesNumFreshReviews() {
		return rottenTomatoesNumFreshReviews;
	}


	public void setRottenTomatoesNumFreshReviews(int rottenTomatoesNumFreshReviews) {
		this.rottenTomatoesNumFreshReviews = rottenTomatoesNumFreshReviews;
	}


	public int getRottenTomatoesNumRottenReviews() {
		return rottenTomatoesNumRottenReviews;
	}


	public void setRottenTomatoesNumRottenReviews(int rottenTomatoesNumRottenReviews) {
		this.rottenTomatoesNumRottenReviews = rottenTomatoesNumRottenReviews;
	}

	@JsonIgnore
	public long getOmdbID() {
		return omdbID;
	}


	public String getImdbID() {
		return imdbID;
	}


	public void setImdbID(String imdbID) {
		this.imdbID = imdbID;
	}


	public String getTitle() {
		return title;
	}


	public String getRuntime() {
		return runtime;
	}


	public void setRuntime(String runtime) {
		this.runtime = runtime;
	}


	public String getReleased() {
		return released;
	}


	public void setReleased(String released) {
		this.released = released;
	}


	public String getLanguage() {
		return language;
	}


	public void setLanguage(String language) {
		this.language = language;
	}


	public String getCountry() {
		return country;
	}


	public void setCountry(String country) {
		this.country = country;
	}


	public int getYear() {
		return year;
	}


	public void setYear(int year) {
		this.year = year;
	}


	public String getAwards() {
		return awards;
	}


	public void setAwards(String awards) {
		this.awards = awards;
	}


	public String getMpaaRating() {
		return mpaaRating;
	}


	public void setMpaaRating(String mpaaRating) {
		this.mpaaRating = mpaaRating;
	}


	public int getMetascore() {
		return metascore;
	}


	public void setMetascore(int metascore) {
		this.metascore = metascore;
	}


	public double getImdbRating() {
		return imdbRating;
	}


	public void setImdbRating(double imdbRating) {
		this.imdbRating = imdbRating;
	}


	public int getImdbVotes() {
		return imdbVotes;
	}


	public void setImdbVotes(int imdbVotes) {
		this.imdbVotes = imdbVotes;
	}


	public String getPosterURL() {
		return posterURL;
	}


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
