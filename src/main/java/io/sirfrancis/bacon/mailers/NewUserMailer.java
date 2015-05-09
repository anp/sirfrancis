package io.sirfrancis.bacon.mailers;

import com.sendgrid.SendGrid;
import com.sendgrid.SendGridException;

public class NewUserMailer {

	private SendGrid sender;
	private String confirmURLFormat;

	public NewUserMailer(SendGrid sender, String confirmURLFormat) {
		this.sender = sender;
		this.confirmURLFormat = confirmURLFormat;
	}

	public void sendAccountCreationConfirmationEmail(String email, String confirmKey) throws SendGridException {
		SendGrid.Email message = new SendGrid.Email();

		String confirmURL = confirmURLFormat.replace("%e", email).replace("%k", confirmKey);

		message.addTo(email);

		//TODO make this a configuration value
		message.setFrom("admin@sirfrancis.io");
		message.setSubject("New SirFrancis Account Confirmation");
		message.setText(
				"Follow this link to confirm your SirFrancis account creation:\n\n" + confirmURL);

		sender.send(message);
	}
}
