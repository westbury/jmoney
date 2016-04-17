package net.sf.jmoney.email;

import javax.mail.MessagingException;

public class UnexpectedContentException extends Exception {
	private static final long serialVersionUID = 1L;

	public UnexpectedContentException(String message) {
		super(message);
	}

	public UnexpectedContentException(Exception e) {
		// TODO Auto-generated constructor stub
	}

}
