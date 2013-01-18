package net.sf.jmoney.search.views;

public class SearchException extends Exception {
	private static final long serialVersionUID = 1L;

	public SearchException(String string, Exception e) {
		super(string, e);
	}

}
