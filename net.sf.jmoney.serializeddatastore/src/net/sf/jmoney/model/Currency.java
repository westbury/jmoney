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

package net.sf.jmoney.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.ResourceBundle;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.serializeddatastore.Messages;

/**
 * This class was created because the currency support wich comes with the Java
 * SDK is to complicated. Therefore we provide a simpler model which is
 * not based upon locales but upon the ISO 4217 currencies.
 */
public class Currency implements Comparable {

	public static final ResourceBundle NAME =
		ResourceBundle.getBundle("net.sf.jmoney.resources.Currency"); //$NON-NLS-1$
	public static final int MAX_DECIMALS = 4;
	private static final short[] SCALE_FACTOR = { 1, 10, 100, 1000, 10000 };
	private static Hashtable currencies = null;
	private static Object[] sortedCurrencies = null;
	private static NumberFormat[] numberFormat = null;

	private String code; // ISO 4217 Code
	private byte decimals;

	/**
	 * @return the available currencies.
	 */
	public static Object[] getAvailableCurrencies() {
		if (currencies == null)
			initSystemCurrencies();
		if (sortedCurrencies == null) {
			sortedCurrencies = currencies.values().toArray();
			Arrays.sort(sortedCurrencies);
		}
		return sortedCurrencies;
	}

	/**
	 * @param code the currency code.
	 * @return the corresponding currency.
	 */
	public static Currency getCurrencyForCode(String code) {
		if (currencies == null)
			initSystemCurrencies();
		return (Currency) currencies.get(code);
	}

	@SuppressWarnings("unchecked")
	private static void initSystemCurrencies() {
		// TODO: How does this work?  Currencies.txt is not in resources folder?!
		InputStream in = JMoneyPlugin.class.getResourceAsStream("resources/Currencies.txt"); //$NON-NLS-1$
		BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
		currencies = new Hashtable();
		try {
			String line = buffer.readLine();
			String c;
			byte d;
			while (line != null) {
				c = line.substring(0, 3);
				d = 2;
				try {
					d = Byte.parseByte(line.substring(4, 5));
				} catch (Exception ex) {
				}
				currencies.put(c, new Currency(c, d));
				line = buffer.readLine();
			}
		} catch (IOException ioex) {
		}
	}

	private static void initNumberFormat() {
		numberFormat = new NumberFormat[MAX_DECIMALS + 1];
		for (int i = 0; i < numberFormat.length; i++) {
			numberFormat[i] = NumberFormat.getNumberInstance();
			numberFormat[i].setMaximumFractionDigits(i);
			numberFormat[i].setMinimumFractionDigits(i);
		}
	}

	protected Currency(String c, byte d) {
		if (d > MAX_DECIMALS)
			throw new IllegalArgumentException(Messages.Currency_DecimalProblem);
		code = c;
		decimals = d;
	}

	/**
	 * @return the currency code.
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @return the name of the currency.
	 */
	public String getCurrencyName() {
		return NAME.getString(getCode());
	}

	@Override
	public String toString() {
		return getCurrencyName() + " (" + getCode() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @return a number format instance for this currency.
	 */
	public NumberFormat getNumberFormat() {
		if (numberFormat == null)
			initNumberFormat();
		return numberFormat[getDecimals()];
	}

	public long parse(String amountString) {
		Number amount = new Double(0);
		try {
			amount = getNumberFormat().parse(amountString);
		} catch (ParseException pex) {
		}
		return Math.round(
			amount.doubleValue() * getScaleFactor());
	}
        
	public String format(long amount) {
		double a = ((double) amount) / getScaleFactor();
		return getNumberFormat().format(a);
	}

	public String format(Long amount) {
		return amount == null ? "" : format(amount.longValue()); //$NON-NLS-1$
	}

	/**
	 * @return the number of decimals that this currency has.
	 */
	public byte getDecimals() {
		return decimals;
	}

	/**
	 * @return the scale factor for this currency (10 to the number of decimals)
	 */
	public short getScaleFactor() {
		return SCALE_FACTOR[decimals];
	}

	public int compareTo(Object obj) {
		return getCurrencyName().compareTo(((Currency) obj).getCurrencyName());
	}
}
