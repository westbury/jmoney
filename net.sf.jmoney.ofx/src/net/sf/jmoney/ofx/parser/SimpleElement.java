package net.sf.jmoney.ofx.parser;

/*
 * @(#)SimpleElement.java
 */

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

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
	}

	public SimpleElement getDescendant(String... tagsToFind) {
		return getDescendant(0, tagsToFind);
	}

	private SimpleElement getDescendant(int tagNameIndex, String... tagsToFind) {
		for (SimpleElement child : getChildElements()) {
			if (child.getTagName().equalsIgnoreCase(tagsToFind[tagNameIndex])) {
				if (tagNameIndex == tagsToFind.length-1) {
					return child;
				} else {
					return child.getDescendant(tagNameIndex+1, tagsToFind);
				}
			}
		}
		return null;
	}

	public Date getDate(String tagName) {
		SimpleElement tmpElement = findElement(tagName);
		String data = tmpElement.getTrimmedText();

		// For some extraordinary reason, the date pattern does not match.
		/*
		 * System.out.println("data=" + childMatch.group(2) + "Y"); Matcher
		 * dateMatch = datePattern.matcher(data); System.out.println("data=" +
		 * childMatch.group(2) + "Z"); if (!dateMatch.matches()) { throw new
		 * RuntimeException("bad date"); }
		 * 
		 * int year = Integer.parseInt(childMatch.group(1)); int month =
		 * Integer.parseInt(childMatch.group(2)); int day =
		 * Integer.parseInt(childMatch.group(3));
		 */
		// So let's just extract another way
		int year = Integer.parseInt(data.substring(0, 4));
		int month = Integer.parseInt(data.substring(4, 6));
		int day = Integer.parseInt(data.substring(6, 8));

		// Date object is always based on local time, which is ok because the
		// Date as milliseconds is never persisted, and the timezone won't change
		// within a session (will it?)
		Calendar cal = Calendar.getInstance();
		cal.set(year, month-1, day, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		return cal.getTime();
	}

	public long getAmount(String tagName, long defaultValue) {
		try {
			return getAmount(tagName);
		} catch (TagNotFoundException e) {
			return defaultValue;
		}
	}

	public long getAmount(String tagName) throws TagNotFoundException {
		SimpleElement tmpElement = findElement(tagName);
		if (tmpElement == null) {
			throw new TagNotFoundException(this, tagName);
		}
		String data = tmpElement.getTrimmedText();

		// long amount = CurrencyParser.parseAmount(data, currency);
		// NOTE [roel] : in Belgium we use ',' as decimal separator
		// I have tried using DecimalFormat but I am unable to 'guess' the
		// Locale based on info available in the ofx-file. -> solution : this
		// dirty hack

		// We got rounding errors using double, so try this...
		// I am sure it can be tidied up, but I just want something that
		// works.
		String [] parts = data.replace(',', '.').split("\\.");
		long amount = Integer.parseInt(parts[0]) * 100;
		if (parts.length > 1) {
			
			/* QFX seems to put extra zeroes at the end.
			 (or at least hsabank.com does)
			 Remove them to get a length of two.
			 
			 Even worse, though, is Wells Fargo bank.  They have been known to put lots
			 of nines at the end, such as 69299.9999 when they mean 69300.00.  Therefore
			 if all the additional digits are nines then we round up.
			 */
			boolean zeroFound = false;
			boolean nineFound = false;
			while (parts[1].length() > 2) {
				if (parts[1].charAt(parts[1].length()-1) == '0' && !nineFound) {
					parts[1] = parts[1].substring(0, parts[1].length() - 1);
					zeroFound = true;
				} else if (parts[1].charAt(parts[1].length()-1) == '9' && !zeroFound) {
					parts[1] = parts[1].substring(0, parts[1].length() - 1);
					nineFound = true;
				} else {
					// TODO use a multi-status for this so the user gets all warnings at one
					// time.
					MessageDialog.openWarning(Display.getDefault().getActiveShell(), "Bad Data", "An amount of " + data +" was found.  Extra digits must be either all zeroes or all nines.  Extra digits have been truncated but you should verify the accuracy of this entry.");
					parts[1] = parts[1].substring(0, 2);
//					throw new RuntimeException("Extra digits and they are not zeroes or all nines");
				}
			}
			if (parts[1].length() == 1) {
				/*
				 * Some banks (e.g. Citibank) only put one digit after the
				 * decimal place if the second digit is zero (number of pennies
				 * is divisible by ten).
				 */
				parts[1] = parts[1] + "0";
			}
			int fractionalPart = Integer.parseInt(parts[1]);
			if (nineFound) {
				fractionalPart++;
			}
			if (data.startsWith("-")) {
				amount -= fractionalPart;
			} else {
				amount += fractionalPart;
			}
		}
//		long amount = CurrencyParser.double2long(currency, Double
//				.parseDouble(data.replace(',', '.')));
		
		return amount;
	}

	/**
	 * 
	 * @param tagName
	 * @return tag value, or null if no child exists with the given tag name
	 */
	public String getString(String tagName) {
		SimpleElement tmpElement = findElement(tagName);
		return tmpElement == null ? null : tmpElement.getTrimmedText();
	}

	public BigDecimal getBigDecimal(String tagName) {
		SimpleElement tmpElement = findElement(tagName);
		return tmpElement == null ? null : new BigDecimal(tmpElement.getTrimmedText());
	}
}