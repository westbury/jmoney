/**
 * 
 */
package net.sf.jmoney.serializeddatastore.handlers;

public class OpenSessionException extends Exception {
	private static final long serialVersionUID = 1L;

	public OpenSessionException(String message) {
		super(message); 
	}

	public OpenSessionException(Exception e) {
		super(e.getLocalizedMessage(), e);
	}
}