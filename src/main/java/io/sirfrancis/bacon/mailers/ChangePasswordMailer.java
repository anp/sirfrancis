package io.sirfrancis.bacon.mailers;

import com.sendgrid.SendGrid;
import com.sendgrid.SendGridException;

public class ChangePasswordMailer {

	private SendGrid sender;
	private String confirmURLFormat;

	public ChangePasswordMailer(SendGrid sender, String confirmURLFormat) {
		this.sender = sender;
		this.confirmURLFormat = confirmURLFormat;
	}

	public void sendPasswordChangeConfirmationEmail(String email, String confirmKey) throws RuntimeException {
		SendGrid.Email message = new SendGrid.Email();

		String confirmURL = confirmURLFormat.replace("%e", email).replace("%k", confirmKey);

		message.addTo(email);
		message.setFrom("admin@sirfrancis.io");
		message.setSubject("SirFrancis Password Change Confirmation");
		message.setText(
				"Follow this link to change your SirFrancis account's password:\n\n" + confirmURL);

		try {
			sender.send(message);
		} catch (SendGridException e) {
			//TODO log the exception
			throw new RuntimeException("Email failed to send: " + e.getMessage());
		}
	}
}
