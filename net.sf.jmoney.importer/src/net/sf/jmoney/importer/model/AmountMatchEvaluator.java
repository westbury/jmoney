package net.sf.jmoney.importer.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


class AmountMatchEvaluator implements IMatchEvaluator {
	enum LimitType { INCLUSIVE, EXCLUSIVE };

	LimitType start;
	LimitType end;
	int start_number;
	int end_number;
	
	AmountMatchEvaluator(String columnPattern) throws InputPatternSyntaxException {
		columnPattern = columnPattern.trim();
		
		if (columnPattern.startsWith("(") || columnPattern.startsWith("[")) {
			if (columnPattern.startsWith("(")) {
				start = LimitType.EXCLUSIVE;
			} else if (columnPattern.startsWith("[")) {
				start = LimitType.INCLUSIVE;
			} else {
				throw new RuntimeException("impossible");
			}

			if (columnPattern.endsWith(")")) {
				end = LimitType.EXCLUSIVE;
			} else if (columnPattern.endsWith("]")) {
				end = LimitType.INCLUSIVE;
			} else {
				throw new InputPatternSyntaxException("Opening brackets have no matching closing brackets");
			}

			String withoutBrackets = columnPattern.substring(1, columnPattern.length()-1);
			String[] parts = withoutBrackets.split(",");
			start_number = parseNumber(parts[0].trim());
			end_number = parseNumber(parts[1].trim());
		} else {
			int n = parseNumber(columnPattern);
			start_number = n;
			end_number = n;
			start = LimitType.INCLUSIVE;
			end = LimitType.INCLUSIVE;
		}
		
	}
	
	@Override
	public MatchResult matcher(String text) {
		try {
			int n = parseNumber(text);
			if (n < start_number || (n <= start_number && start == LimitType.EXCLUSIVE)) {
				return new MatchResult(false, null);
			}
			if (n > end_number || (n >= end_number && end == LimitType.EXCLUSIVE)) {
				return new MatchResult(false, null);
			}
			return new MatchResult(true, new String[0]);
		} catch (InputPatternSyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
    private int parseNumber(String text) throws InputPatternSyntaxException {
		final Pattern thisCompiledPattern = Pattern.compile("\\-?(\\d+)(\\.(\\d\\d))?");
		// This is used on the displayable value which includes commas.
		// The user might also put commas in the range specification if there is only a single value.
		String textNoCommas = text.replaceAll(",", "");
		Matcher m = thisCompiledPattern.matcher(textNoCommas);
		if (!m.matches()) {
			throw new InputPatternSyntaxException("Number does not match currency amount format: " + text);
		}
		String numberText = m.group(1) + (m.group(2) == null ? "00" : m.group(3));
		
		return Integer.parseInt(numberText);
    }
		
}