package io.sirfrancis.bacon.tasks;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMultimap;
import com.sendgrid.SendGrid;
import io.dropwizard.servlets.tasks.Task;
import io.sirfrancis.bacon.BaconConfiguration;
import io.sirfrancis.bacon.db.QuizDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;

public class CreateQuizPathTask extends Task {
	private static final Logger LOGGER = LoggerFactory.getLogger(CreateQuizPathTask.class);
	SendGrid mailer;
	QuizDAO quizDAO;

	public CreateQuizPathTask() {
		super("create-quiz-path");
		quizDAO = new QuizDAO();
		mailer = new SendGrid(BaconConfiguration.getSendgridUsername(), BaconConfiguration.getSendgridPassword());
	}

	@Override
	@Timed
	public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
		LOGGER.info("Beginning calculations for quiz creation.");

		quizDAO.writeQuizPath();

		SendGrid.Email completionEmail = new SendGrid.Email();
		completionEmail.setFrom("admin@sirfrancis.io");
		completionEmail.addTo("adam.n.perry@gmail.com");
		completionEmail.setSubject("Quiz Path Creation Complete");
		completionEmail.setText("Quiz Path Created Successfully. Try it out.");

		LOGGER.info("Finished calculating and storing quiz path.");
	}


}
