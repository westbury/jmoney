package net.sf.jmoney.entrytable;

import org.eclipse.swt.widgets.Control;

public class InvalidUserEntryException extends Exception {
	private static final long serialVersionUID = -8693190447361905525L;

	Control itemWithError = null;

	public InvalidUserEntryException(String message, Control itemWithError) {
		super(message);
		this.itemWithError = itemWithError;
	}

	public Control getItemWithError() {
		return itemWithError;
	}
}
