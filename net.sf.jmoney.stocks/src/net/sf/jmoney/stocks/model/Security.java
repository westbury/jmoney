/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package net.sf.jmoney.stocks.model;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.regex.Pattern;

import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.Commodity;

public abstract class Security extends Commodity {

	// This implementation formats all quantities as numbers with three decimal places.
	private int SCALE_FACTOR = 1000;

	private static NumberFormat numberFormat = NumberFormat.getNumberInstance();
	static {
		numberFormat.setMaximumFractionDigits(3);
		numberFormat.setMinimumFractionDigits(0);
	}

	private String cusip;
	private String symbol;

	/**
	 * Constructor used by datastore plug-ins to create
	 * a stock object.
	 */
	public Security(
			IObjectKey objectKey,
			ListKey parentKey,
			String name,
			String cusip,
			String symbol,
			IValues extensionValues) {
		super(objectKey, parentKey, name, extensionValues);

		this.cusip = cusip;
		this.symbol = symbol;
	}

	/**
	 * Constructor used by datastore plug-ins to create
	 * a stock object.
	 */
	public Security(
			IObjectKey objectKey,
			ListKey parentKey) {
		super(objectKey, parentKey);

		this.cusip = null;
		this.symbol = null;
	}

	@Override
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.stocks.security";
	}

	/**
	 * If 12 characters, this is assumed to be an ISIN.
	 * 
	 * @return the 9 digit CUSIP if a US security or the CINS (one letter
	 *         followed by 8 digits) if a non-US security
	 */
	public String getCusip() {
		return cusip;
	}

	public String getISIN() {
		return cusip;
	}

	/**
	 * This is a helper method.
	 * 
	 * @return
	 */
	public String getSedol() {
		if (cusip != null && cusip.length() == 12 && cusip.startsWith("GB00")) {
			return cusip.substring(4, 7);
		} else {
			return null;
		}
	}
	
	public static String convertSedolToIsin(String sedol) {
		return appendIsinCheckDigit("GB00" + sedol);
	}
	
	private static String appendIsinCheckDigit(String isin) {
		return isin + calculateIsinCheckDigit(isin);
	}
	
	public String getSymbol() {
		return symbol;
	}

	/**
	 * @param cusip
	 *            the 9 digit CUSIP if a US security or the CINS (one letter
	 *            followed by 8 digits) if a non-US security
	 */
	public void setCusip(String cusip) {
		String oldCusip = this.cusip;
		this.cusip = cusip;

		// Notify the change manager.
		processPropertyChange(SecurityInfo.getCusipAccessor(), oldCusip, cusip);
	}

	public void setSymbol(String symbol) {
		String oldSymbol = this.symbol;
		this.symbol = symbol;

		// Notify the change manager.
		processPropertyChange(SecurityInfo.getSymbolAccessor(), oldSymbol, symbol);
	}

	@Override
	public long parse(String amountString) {
		Number amount;
		try {
			amount = numberFormat.parse(amountString);
		} catch (ParseException ex) {
			// If bad user entry, return zero
			amount = new Double(0);
		}
		return Math.round(amount.doubleValue() * SCALE_FACTOR);
	}

	@Override
	public String format(long amount) {
		double a = ((double) amount) / SCALE_FACTOR;
		return numberFormat.format(a);
	}

	/**
	 * @return The scale factor.  Always 1000 for stock for the time being.
	 */
	// TODO: This property should be for currency only.
	@Override
	public short getScaleFactor() {
		return 1000;
	}


	private static final Pattern ISIN_PATTERN = Pattern.compile("[A-Z]{2}([A-Z0-9]){9}[0-9]");

	/**
	 * This is a helper method that checks that an ISIN is valid.
	 * 
	 * @param isin the ISIN to be validated
	 * @return true if a valid ISIN, false otherwise
	 */
	public static boolean checkIsinCode(final String isin) {
		if (isin == null) {
			return false;
		}

		if (!ISIN_PATTERN.matcher(isin).matches()) {
			return false;
		}

		char calculatedValue = calculateIsinCheckDigit(isin);
		return isin.charAt(11) == calculatedValue;
	}

	private static char calculateIsinCheckDigit(final String isin) {
		StringBuilder digits = new StringBuilder();
		for (int i = 0; i < 11; ++i) {
			digits.append(Character.digit(isin.charAt(i), 36));
		}
		digits.reverse();

		int sum = 0;
		for (int i = 0; i < digits.length(); ++i) {
			int digit = Character.digit(digits.charAt(i), 36);
			if (i % 2 == 0) {
				digit *= 2;
			}
			sum += digit / 10;
			sum += digit % 10;
		}
		int tensComplement = (sum % 10 == 0) ? 0 : ((sum / 10) + 1) * 10 - sum;
		return Character.forDigit(tensComplement, 36);
	}

}

