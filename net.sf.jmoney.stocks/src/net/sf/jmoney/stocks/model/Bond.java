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
import java.util.Date;

import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.Currency;

public class Bond extends Security {
	
	private static final int MAX_DECIMALS = 4;
	private static final short[] SCALE_FACTOR = { 1, 10, 100, 1000, 10000 };
	private static NumberFormat[] numberFormat = null;
	
	/**
	 * Guaranteed non-null because the session default currency is
	 * set by default.
	 */
	protected IObjectKey currencyKey;

	private int interestRate;
	private Date maturityDate;
	private long redemptionValue;
	
	private static void initNumberFormat() {
		numberFormat = new NumberFormat[MAX_DECIMALS + 1];
		for (int i = 0; i < numberFormat.length; i++) {
			numberFormat[i] = NumberFormat.getNumberInstance();
			numberFormat[i].setMaximumFractionDigits(i);
			numberFormat[i].setMinimumFractionDigits(i);
		}
	}
	
    /**
     * Constructor used by datastore plug-ins to create
     * a bond object.
     */
	public Bond(
			IObjectKey objectKey,
			ListKey parentKey,
			String name,
			String cusip,
			String symbol,
			IObjectKey currencyKey,
			int interestRate,
			Date maturityDate,
			long redemptionValue,
			IValues extensionValues) {
		super(objectKey, parentKey, name, cusip, symbol, extensionValues);

		/*
		 * The currency for this account is not allowed to be null, because
		 * users of this class may assume it to be non-null and would not know
		 * how to handle this bond if it were null.
		 * 
		 * If null is passed, set to the default currency for the session.
		 * This is guaranteed to be never null.
		 */
		if (currencyKey != null) {
			this.currencyKey = currencyKey;
		} else {
			this.currencyKey = getDataManager().getSession().getDefaultCurrency().getObjectKey();
		}
		
		this.interestRate = interestRate;
		this.maturityDate = maturityDate;
		this.redemptionValue = redemptionValue;
	}

    /**
     * Constructor used by datastore plug-ins to create
     * a bond object.
     */
	public Bond(
			IObjectKey objectKey,
			ListKey parentKey) {
		super(objectKey, parentKey);
		
		// Set the currency to the session default currency.
		this.currencyKey = getDataManager().getSession().getDefaultCurrency().getObjectKey();
		
		this.interestRate = 0;
		this.maturityDate = null;
		this.redemptionValue = 0;
	}

	@Override
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.stocks.bond";
	}
	
	public Currency getCurrency() {
        return (Currency)currencyKey.getObject();
	}

	public void setCurrency(Currency aCurrency) {
	    if (aCurrency == null) throw new IllegalArgumentException();
        Currency oldCurrency = getCurrency();
		currencyKey = aCurrency.getObjectKey();

		// Notify the change manager.
		processPropertyChange(BondInfo.getCurrencyAccessor(), oldCurrency, aCurrency);
	}

	/**
	 * @return the coupon interest rate.
	 */
	public int getInterestRate() {
		return interestRate;
	}
	
	public void setInterestRate(int interestRate) {
		int oldInterestRate = this.interestRate;
		this.interestRate = interestRate;

		// Notify the change manager.
		processPropertyChange(BondInfo.getInterestRateAccessor(), new Integer(oldInterestRate), new Integer(interestRate));
	}
	
	/**
	 * @return the coupon interest rate.
	 */
	public Date getMaturityDate() {
		return maturityDate;
	}
	
	public void setMaturityDate(Date maturityDate) {
		Date oldMaturityDate = this.maturityDate;
		this.maturityDate = maturityDate;

		// Notify the change manager.
		processPropertyChange(BondInfo.getMaturityDateAccessor(), oldMaturityDate, maturityDate);
	}
	
	/**
	 * @return the redemption value.
	 */
	public long getRedemptionValue() {
		return redemptionValue;
	}
	
	public void setRedemptionValue(long redemptionValue) {
		long oldRedemptionValue = this.redemptionValue;
		this.redemptionValue = redemptionValue;

		// Notify the change manager.
		processPropertyChange(BondInfo.getRedemptionValueAccessor(), new Long(oldRedemptionValue), new Long(redemptionValue));
	}
	
	/**
	 * @return a number format instance for this currency.
	 */
	
	private NumberFormat getNumberFormat() {
		if (numberFormat == null)
			initNumberFormat();
		return numberFormat[2];
	}

	@Override
	public long parse(String amountString) {
		Number amount;
		try {
			amount = getNumberFormat().parse(amountString);
		} catch (ParseException pex) {
			amount = new Double(0);
		}
		return Math.round(
				amount.doubleValue() * getScaleFactor());
	}
	
	@Override
	public String format(long amount) {
		double a = ((double) amount) / getScaleFactor();
		return getNumberFormat().format(a);
	}
	
	/**
	 * @return the scale factor for this currency (10 to the number of decimals)
	 */
	@Override
	public short getScaleFactor() {
		return SCALE_FACTOR[2];
	}
}
