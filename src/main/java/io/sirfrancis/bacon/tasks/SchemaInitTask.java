package io.sirfrancis.bacon.tasks;

import com.google.common.collect.ImmutableMultimap;
import io.dropwizard.servlets.tasks.Task;
import io.sirfrancis.bacon.db.GraphConnection;

import java.io.PrintWriter;

/**
 * Created by adam on 3/8/15.
 */
public class SchemaInitTask extends Task {

	public SchemaInitTask() {
		super("init-schema");
	}

	public void execute(ImmutableMultimap<String, String> params, PrintWriter writer) {
		GraphConnection.initSchemaTypes();
	}
}
