package io.sirfrancis.bacon.resources;

import com.codahale.metrics.annotation.Timed;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.views.SubConfirmedView;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by Adam on 1/12/2015.
 */
@Path("/email/confirmed")
@Produces(MediaType.TEXT_HTML)
public class SubConfirmedResource {
	private BaconConfiguration config;

	public SubConfirmedResource(BaconConfiguration config){
		this.config = config;
	}

	@GET
	@Timed
	public SubConfirmedView showConfirm() {
		return new SubConfirmedView(config.getStaticContentPath());
	}
}
