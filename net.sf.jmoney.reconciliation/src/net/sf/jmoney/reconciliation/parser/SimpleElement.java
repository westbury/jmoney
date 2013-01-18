package net.sf.jmoney.reconciliation.parser;

/*
 * @(#)SimpleElement.java
 */

import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <code>SimpleElement</code> is the only node type for simplified DOM model.
 */
public class SimpleElement {
	private String tagName;
	private String text;

	private HashMap<String, String> attributes;
	private LinkedList<SimpleElement> childElements;
	private SimpleElement nextSibling;

	public SimpleElement(String tagName) {
		this.tagName = tagName;
		attributes = new HashMap<String, String>();
		childElements = new LinkedList<SimpleElement>();
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public String getText() {
		return text;
	}

	public String getTrimmedText() {
		if (isEmptyText())
			return "";
		else {
			return text.trim();
//			Matcher m = onlyWhiteSpacePattern.matcher(text);
//			if (m.matches())
//				return m.group(2);
//			else
//				return text;
		}
	}

	public void setText(String text) {
		this.text = text;
	}

	public boolean isEmptyText() {
		if (text == null || text.length() == 0)
			return true;
		// if string only contains newlines and whitespace -> consider it as
		// empty string
		Matcher m = onlyWhiteSpacePattern.matcher(text);
		return !m.matches();
	}

	public String getAttribute(String name) {
		return attributes.get(name);
	}

	public void setAttribute(String name, String value) {
		attributes.put(name, value);
	}
	
	public SimpleElement getNextSibling() {
		return nextSibling;
	}

	public void setNextSibling(SimpleElement nextSibling) {
		this.nextSibling = nextSibling;
	}
	
	public void addChildElement(SimpleElement element) {
		if (!childElements.isEmpty())
			childElements.getLast().setNextSibling(element);
		childElements.add(element);
	}

	public LinkedList<SimpleElement> getChildElements() {
		return childElements;
	}

	@Override
	public String toString() {
		return "<" + tagName + ">" + getTrimmedText() + "</" + tagName + ">";
	}

	public static String newline = System.getProperty("line.separator");

	private static final Pattern onlyWhiteSpacePattern = Pattern.compile(
			"(\\s*)([^\\s]+)(.*)", Pattern.DOTALL);

	public String toXMLString(int spaces) {
		String trimString = getTrimmedText();
		StringBuffer sb = new StringBuffer(trimString.length()
				+ tagName.length() + 2 + 3 * spaces);
		appendWhiteSpace(spaces, sb);
		sb.append("<" + tagName + ">" + newline);
		int myspace = spaces + 2;
		if (!isEmptyText()) {
			appendWhiteSpace(myspace, sb);
			sb.append(trimString);
			appendWhiteSpace(myspace, sb);
			sb.append(newline);
		}
		for (SimpleElement element : getChildElements()) {
			sb.append(element.toXMLString(myspace));
		}
		myspace -= 2;
		appendWhiteSpace(myspace, sb);
		sb.append("</" + tagName + ">" + newline);
		return sb.toString();
	}

	private void appendWhiteSpace(int spaces, StringBuffer sb) {
		for (int i = 0; i < spaces; i++) {
			sb.append(' ');
		}
	}

	/**
	 * Returns the child-element of the current element with the given tagName
	 *  
	 * @param tagToFind
	 * @return a child SimpleElement with tagName = tagToFind
	 */
	public SimpleElement findElement(String tagToFind) {
		SimpleElement found = null;
		if (getTagName().equalsIgnoreCase(tagToFind))
			return this;
		for (SimpleElement element : getChildElements()) {
			if (found != null)
				break;
			found = element.findElement(tagToFind);
		}
		return found;
	}}