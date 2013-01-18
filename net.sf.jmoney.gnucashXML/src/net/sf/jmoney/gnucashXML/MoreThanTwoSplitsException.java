/*
 * Created on 11 juil. 2004
 */
package net.sf.jmoney.gnucashXML;

/**
 * @author Faucheux
 */
public class MoreThanTwoSplitsException extends Exception {
	private static final long serialVersionUID = 383550925610636327L;
	public MoreThanTwoSplitsException() {
		super();
	}
	public MoreThanTwoSplitsException(String s) {
		super(s);
	}
}
