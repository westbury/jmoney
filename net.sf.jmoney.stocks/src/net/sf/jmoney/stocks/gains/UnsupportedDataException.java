package net.sf.jmoney.stocks.gains;

import org.eclipse.core.runtime.IStatus;

public class UnsupportedDataException extends Exception {
	private static final long serialVersionUID = 1L;

	private IStatus status;

	public UnsupportedDataException(IStatus status) {
		this.status = status;
	}

	public IStatus getStatus() {
		return status;
	}
}