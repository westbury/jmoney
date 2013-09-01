package net.sf.jmoney.ameritrade;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BondSalePatternMatcher {
	private Pattern pattern;
	private Matcher matcher;

	BondSalePatternMatcher() {
		pattern = Pattern.compile("((BONDS - FULL CALL)|(SELL TRADE)) \\(([0-9,A-Z]*)\\) (.*) @ ([0-9,A-Z]*\\.[0-9,A-Z]*)");
	}
	
	public boolean matches(String memo) {
		matcher = pattern.matcher(memo);
		return matcher.matches();
	}

	public String getSymbol() {
		return matcher.group(4);
	}
}
