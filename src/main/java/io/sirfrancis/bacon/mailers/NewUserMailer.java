package io.sirfrancis.bacon.mailers;

import com.sendgrid.SendGrid;
import com.sendgrid.SendGridException;

public class NewUserMailer {

	private SendGrid sender;
	private String confirmURLFormat;

	public NewUserMailer(String sendGridUserName,
						 String sendGridPassword,
						 String confirmURLFormat) {

		sender = new SendGrid(sendGridUserName, sendGridPassword);
		this.confirmURLFormat = confirmURLFormat;
	}

	public void sendAccountCreationConfirmationEmail(String email, String confirmKey) throws RuntimeException {
		SendGrid.Email message = new SendGrid.Email();

		String confirmURL = confirmURLFormat.replace("%e", email).replace("%k", confirmKey);

		message.addTo(email);
		message.setFrom("admin@sirfrancis.io");
		message.setSubject("New SirFrancis Account Confirmation");
		message.setText(
				"Follow this link to confirm your SirFrancis account creation:\n\n" + confirmURL);

		try {
			sender.send(message);
		} catch (SendGridException e) {
			//TODO log the exception
			throw new RuntimeException("Email failed to send: " + e.getMessage());
		}
	}
}
