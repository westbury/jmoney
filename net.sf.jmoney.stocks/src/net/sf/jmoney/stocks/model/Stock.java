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

import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;

public class Stock extends Security {

	// This implementation formats all quantities as numbers with four decimal places.
	// The unit for a stock is one ten-thousandth of a stock.
	public static int SCALE_FACTOR = 10000;
	public static int SCALE_DIGITS = 4;

	private static NumberFormat numberFormat = NumberFormat.getNumberInstance();
	static {
		numberFormat.setMaximumFractionDigits(4);
		numberFormat.setMinimumFractionDigits(0);
	}

	private String nominalValue;

    /**
     * Constructor used by datastore plug-ins to create
     * a stock object.
     */
	public Stock(
			IObjectKey objectKey,
			ListKey parentKey,
			String name,
			String cusip,
			String symbol,
			String nominalValue,
			IValues extensionValues) {
		super(objectKey, parentKey, name, cusip, symbol, extensionValues);

		this.nominalValue = nominalValue;
	}

    /**
     * Constructor used by datastore plug-ins to create
     * a stock object.
     */
	public Stock(
			IObjectKey objectKey,
			ListKey parentKey) {
		super(objectKey, parentKey);

		this.nominalValue = null;
	}

	@Override
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.stocks.stock";
	}

	/**
	 * @return the nominal value.  For example, "ORD 25P"
	 */
	public String getNominalValue() {
		return nominalValue;
	}

	public void setNominalValue(String nominalValue) {
		String oldNominalValue = this.nominalValue;
		this.nominalValue = nominalValue;

		// Notify the change manager.
		processPropertyChange(StockInfo.getNominalValueAccessor(), oldNominalValue, nominalValue);
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
	public int getScaleFactor() {
		return SCALE_FACTOR;
	}
}

