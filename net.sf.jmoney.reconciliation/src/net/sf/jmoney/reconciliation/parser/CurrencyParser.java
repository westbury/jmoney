package net.sf.jmoney.reconciliation.parser;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;

public class CurrencyParser {
	static final HashMap<String, NumberFormat> hashMap = new HashMap<String, NumberFormat>();

	static {
		Locale[] availableLocales = Locale.getAvailableLocales();
		for (int i = 0; i < availableLocales.length; i++) {
			Locale locale = availableLocales[i];
			if (locale.getCountry().length() == 2) {
				String currencyCode = Currency.getInstance(locale)
						.getCurrencyCode();
				NumberFormat numberFormat = NumberFormat
						.getCurrencyInstance(locale);
				hashMap.put(currencyCode, numberFormat);
			}
		}
	}

	public static NumberFormat getNumberFormat(Currency currency) {
		return hashMap.get(currency.getCurrencyCode());
	}

	/**
	 * This is a cumbersome way of converting a given string containing some amount into a long using the correct CurrencyFormat.
	 * 
	 * Note : only tested for EUR
	 * Fails for USD because currency.getSymbol returns USD iso $
	 * 
	 * @param amount
	 * @param currency
	 * @return
	 * @throws ParseException
	 */
	public static long parseAmount(String amount, Currency currency) throws ParseException
	{
		boolean positive =  amount.startsWith("+");
		NumberFormat currencyInstance = getNumberFormat(currency);
		Number parse = currencyInstance.parse(amount.substring(positive?1:0)+" "+currency.getSymbol());
		return double2long(currency, parse);
	}

	public static long double2long(Currency currency, Number parse) {
		double d = parse.doubleValue();
		d *= Math.pow(10.,currency.getDefaultFractionDigits());
		return (long) d;
	}
}
