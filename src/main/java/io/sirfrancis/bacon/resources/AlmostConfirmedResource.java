package io.sirfrancis.bacon.resources;

import com.codahale.metrics.annotation.Timed;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.views.AlmostConfirmedView;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by Adam on 1/12/2015.
 */
@Path("/email/almost")
@Produces(MediaType.TEXT_HTML)
public class AlmostConfirmedResource {
	private BaconConfiguration config;

	public AlmostConfirmedResource(BaconConfiguration config) {
		this.config = config;
	}

	@GET
	@Timed
	public AlmostConfirmedView showAlmost() {
		return new AlmostConfirmedView(config.getStaticContentPath());
	}
}