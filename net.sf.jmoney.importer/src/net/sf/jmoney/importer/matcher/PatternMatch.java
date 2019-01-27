package net.sf.jmoney.importer.matcher;

import net.sf.jmoney.importer.model.MemoPattern;

/**
 * This class represents the result of a successful match against a pattern.
 * 
 * @author Nigel Westbury
 *
 */
public class PatternMatch {
	public final MemoPattern pattern;
	public final Object[] args;
	
	public PatternMatch(MemoPattern pattern, Object[] args) {
		this.pattern = pattern;
		this.args = args;
	}
}
