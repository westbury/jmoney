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

package net.sf.jmoney.property.model;

import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.IncomeExpenseAccount;

public class RealProperty extends Commodity {
	
	/**
	 * The income account into which all dividends from stock in this
	 * account are entered.
	 */
	IObjectKey rentalIncomeAccountKey;

    /**
     * Constructor used by datastore plug-ins to create
     * a stock object.
     */
	public RealProperty(
			IObjectKey objectKey,
			ListKey parentKey,
			String name,
			IObjectKey rentalIncomeAccountKey,
			IValues extensionValues) {
		super(objectKey, parentKey, name, extensionValues);
		
		this.rentalIncomeAccountKey = rentalIncomeAccountKey;
	}

    /**
     * Constructor used by datastore plug-ins to create
     * a stock object.
     */
	public RealProperty(
			IObjectKey objectKey,
			ListKey parentKey) {
		super(objectKey, parentKey);
	}

	@Override
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.property.realProperty";
	}
	
	@Override
	public long parse(String amountString) {
			return Long.parseLong(amountString);
	}
	
	@Override
	public String format(long amount) {
		return Long.toString(amount);
	}
	
	/**
	 * @return The scale factor.  Always 1 for real property for the time being.
	 */
	// TODO: This property should be for currency only.
	@Override
	public short getScaleFactor() {
		return 1;
	}

	/**
	 * @return the account that contains the dividend income for
	 * 		stock in this account
	 */
	public IncomeExpenseAccount getRentalIncomeAccount() {
        return rentalIncomeAccountKey == null
        ? null
        		: (IncomeExpenseAccount)rentalIncomeAccountKey.getObject();
	}

	public void setRentalIncomeAccount(IncomeExpenseAccount rentalIncomeAccount) {
		IncomeExpenseAccount oldAccount = getRentalIncomeAccount();
		this.rentalIncomeAccountKey = rentalIncomeAccount == null ? null : rentalIncomeAccount.getObjectKey();

		// Notify the change manager.
		processPropertyChange(RealPropertyInfo.getRentalIncomeAccountAccessor(), oldAccount, rentalIncomeAccount);
	}

}

