package net.sf.jmoney.importer.model;

/**
 * Example implementations would be regex pattern matching (used for the memo field)
 * or range matching (used for amount field)
 */
public interface IMatchEvaluator {

	class MatchResult {
		MatchResult(boolean matches, String[] args) {
			this.matches = matches;
			this.args = args;
		}
		public final boolean matches;
		public final String[] args;
	}
	
	MatchResult matcher(String text);
}
