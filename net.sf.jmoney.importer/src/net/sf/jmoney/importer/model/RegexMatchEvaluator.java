package net.sf.jmoney.importer.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


class RegexMatchEvaluator implements IMatchEvaluator {
	Pattern compiledPattern;
	
	RegexMatchEvaluator(String columnPattern) throws InputPatternSyntaxException {
		try {
			this.compiledPattern = Pattern.compile(columnPattern, Pattern.CASE_INSENSITIVE);
		} catch (PatternSyntaxException e) {
			throw new InputPatternSyntaxException(e.getMessage());
		}
	}
	
	@Override
	public MatchResult matcher(String text) {
		Matcher m = compiledPattern.matcher(text);
		if (m.matches()) {
			/*
			 * Group zero is the entire string and the groupCount method
			 * does not include that group, so there is really one more group
			 * than the number given by groupCount.
			 */
			String[] args = new String[m.groupCount()+1];
			for (int i = 0; i <= m.groupCount(); i++) {
				args[i] = m.group(i);
			}
			return new MatchResult(true, args);
		} else {
			return new MatchResult(false, null);
		}
	}
		
}