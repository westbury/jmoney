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
 * the BankAccount object.
 * <P>
 * To add fields and methods to a BankAccount object, one should derive a
 * class from BankAccountExtension. This mechanism allows multiple
 * extensions to a BankAccount object to be added and maintained at runtime.
 * <P>
 * All extensions to BankAccount objects implement the same methods that are
 * in the BankAccount object. This is for convenience so the consumer can
 * get a single object that supports both the original BankAccount methods
 * and the extension methods. All BankAccount methods are passed on to the
 * BankAccount object.
 * 
 * @author Nigel Westbury
 */
public abstract class BankAccountExtension extends CurrencyAccountExtension {
    
    public BankAccountExtension(ExtendableObject/*BankAccount*/ extendedObject) {
    	super(extendedObject);
    }

	public String getBank() {
		return getBaseObject().getBank();
	}

	/**
	 * @return the account number of this account
	 */
	public String getAccountNumber() {
		return getBaseObject().getAccountNumber();
	}

	/**
	 * @param bank the name of the bank
	 */
	public void setBank(String bank) {
		getBaseObject().setBank(bank);
	}

	/**
	 * Sets the account number of this account.
	 * @param accountNumber the account number
	 */
	public void setAccountNumber(String accountNumber) {
		getBaseObject().setAccountNumber(accountNumber);
	}

	// This does some casting - perhaps this is not needed
	// if generics are used????
    @Override	
	public BankAccount getBaseObject() {
		return (BankAccount)baseObject;
	}
}
