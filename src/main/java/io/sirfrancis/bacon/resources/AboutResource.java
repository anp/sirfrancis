package io.sirfrancis.bacon.resources;

import com.codahale.metrics.annotation.Timed;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.views.AboutView;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by Adam on 1/11/2015.
 */
@Path("/about")
@Produces(MediaType.TEXT_HTML)
public class AboutResource {
	private BaconConfiguration config;

	public AboutResource(BaconConfiguration config) {
		this.config = config;
	}

	@GET
	@Timed
	public AboutView showAbout() {
		return new AboutView(config.getStaticContentPath());
	}

}
