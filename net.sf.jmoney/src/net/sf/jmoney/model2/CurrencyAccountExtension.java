/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2006 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.model2;

/**
 * This is a helper class that makes it a little easier for a plug-in to extend
 * the CurrencyAccount object.
 * <P>
 * To add fields and methods to a CurrencyAccount object, one should derive a
 * class from CurrencyAccountExtension. This mechanism allows multiple
 * extensions to a CurrencyAccount object to be added and maintained at runtime.
 * <P>
 * All extensions to CurrencyAccount objects implement the same methods that are
 * in the CurrencyAccount object. This is for convenience so the consumer can
 * get a single object that supports both the original CurrencyAccount methods
 * and the extension methods. All CurrencyAccount methods are passed on to the
 * CurrencyAccount object.
 * 
 * @author Nigel Westbury
 */
public abstract class CurrencyAccountExtension extends CapitalAccountExtension {
    
    public CurrencyAccountExtension(CurrencyAccount extendedObject) {
    	super(extendedObject);
    }

	public Currency getCurrency() {
		return getBaseObject().getCurrency();
	}

	public Commodity getCommodity(Entry entry) {
		return getBaseObject().getCommodity(entry);
	}
	
	/**
	 * @return the initial balance of this account.
	 */
	public long getStartBalance() {
		return getBaseObject().getStartBalance();
	}

	public void setCurrency(Currency currency) {
		getBaseObject().setCurrency(currency);
	}

	/**
	 * Sets the initial balance of this account.
	 * @param s the start balance
	 */
	public void setStartBalance(long startBalance) {
		getBaseObject().setStartBalance(startBalance);
	}

	// This does some casting - perhaps this is not needed
	// if generics are used????
    @Override	
	public CurrencyAccount getBaseObject() {
		return (CurrencyAccount)baseObject;
	}
}
