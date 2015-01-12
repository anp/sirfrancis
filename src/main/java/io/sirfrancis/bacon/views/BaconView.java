package io.sirfrancis.bacon.views;

import io.dropwizard.views.View;

/**
 * Created by Adam on 1/12/2015.
 */
public class BaconView extends View {
	private String staticContentAddress;

	public BaconView(String templatePath, String staticContentAddress) {
		super(templatePath);
		this.staticContentAddress = staticContentAddress;
	}

	public String getStaticContentAddress() {
		return staticContentAddress;
	}
}
