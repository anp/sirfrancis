package io.sirfrancis.bacon.health;

import com.codahale.metrics.health.HealthCheck;

/**
 * Created by Adam on 1/4/2015.
 */
public class TemplateHealthCheck extends HealthCheck{
	private final String template;

	public TemplateHealthCheck(String template) {
		this.template = template;
	}

	@Override
	protected Result check() throws Exception {
		final String saying = String.format(template,"TEST");
		if (!saying.contains("TEST")) {
			return Result.unhealthy("template doesn't include the name");
		}
		return Result.healthy();
	}
}
