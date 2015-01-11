package io.sirfrancis.bacon.client;

import com.sun.jersey.api.client.Client;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anp68 on 1/10/2015.
 */
public class TraktClient {

	private static Client jerseryClient;

	public TraktClient(Client jerseryClient) {
		this.jerseryClient = jerseryClient;
	}

	public List<TraktRating> getTraktRatings(String username, String saltedPass) {
		List<TraktRating> ratings = new ArrayList<>();



		return ratings;
	}
}
