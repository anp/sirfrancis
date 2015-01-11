package io.sirfrancis.bacon.resources;

import com.codahale.metrics.annotation.Timed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by Adam on 1/4/2015.
 */

@Path("/")
@Produces(MediaType.TEXT_HTML)

public class HomeResource {

	public HomeResource() {
	}

	@GET
	@Timed
	public HomeView showHome() {
		return new HomeView();
	}
}
