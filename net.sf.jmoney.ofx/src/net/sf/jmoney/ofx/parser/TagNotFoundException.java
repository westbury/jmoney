package net.sf.jmoney.ofx.parser;

public class TagNotFoundException extends Exception {
	private static final long serialVersionUID = 1037419180606514304L;

	public TagNotFoundException(SimpleElement parentElement, String tagName) {
		super(tagName + " was not found but was expected in " + parentElement.getTagName());
	}
}
