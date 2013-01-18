/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2007 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.oda.driver;

import java.util.Collection;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IncomeExpenseAccount;

import org.eclipse.datatools.connectivity.oda.OdaException;

class Parameter_Object extends Parameter {
	private String name;
	private ColumnType type;
	private ExtendablePropertySet<?> propertySet;
	private Collection<? extends ExtendableObject> value;

	public Parameter_Object(ExtendablePropertySet<?> propertySet, ColumnType type) {
		super("param1");  // TODO: where do the names come from?
		this.propertySet = propertySet;
		this.type = type;
	}

	public ExtendablePropertySet<?> getPropertySet() {
		return propertySet;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ColumnType getColumnType() {
		return type;
	}

	@Override
	public boolean isNullable() {
		// For time being, do not allow null parameters
		return false;
	}
	
	@Override
	public void setString(String value) throws OdaException {
		if (Currency.class.isAssignableFrom(propertySet.getImplementationClass())) {
			Vector<Currency> matchingCurrencies = new Vector<Currency>();
			matchingCurrencies.add(JMoneyPlugin.getDefault().getSession().getCurrencyForCode(value));
			this.value = matchingCurrencies;
			
		} else if (Account.class.isAssignableFrom(propertySet.getImplementationClass())) {
			String pattern = value.toUpperCase();
			
			/*
			 * We return all accounts, at all levels, where the name contains the given string.
			 */
			Vector<Account> matchingAccounts = new Vector<Account>();
			for (Account account: JMoneyPlugin.getDefault().getSession().getAccountCollection()) {
				if (account instanceof IncomeExpenseAccount) {
					addSubAccounts((IncomeExpenseAccount)account, matchingAccounts, pattern);
				}
				if (account instanceof CapitalAccount) {
					addSubAccounts((CapitalAccount)account, matchingAccounts, pattern);
				}
			}
			
			this.value = matchingAccounts;
		} else {
			throw new OdaException("Not yet implemented");
		}
	}

	private void addSubAccounts(CapitalAccount account, Vector<Account> matchingAccounts, String pattern) {
		if (propertySet.getImplementationClass().isAssignableFrom(account.getClass())
				&& account.getName().toUpperCase().indexOf(pattern) >= 0) {
			matchingAccounts.add(account);	
		}
		
		// TODO: Sort out Pyrenees Court (rental) BankAccount problem:
//		for (CapitalAccount subAccount: account.getSubAccountCollection()) {
//			addSubAccounts(subAccount, matchingAccounts, pattern);
//		}
		for (Account subAccount: account.getSubAccountCollection()) {
			if (subAccount instanceof CapitalAccount) {
				addSubAccounts((CapitalAccount)subAccount, matchingAccounts, pattern);
			}
		}
	}

	private void addSubAccounts(IncomeExpenseAccount account, Vector<Account> matchingAccounts, String pattern) {
		if (propertySet.getImplementationClass().isAssignableFrom(account.getClass())
				&& account.getName().toUpperCase().indexOf(pattern) >= 0) {
			matchingAccounts.add(account);	
		}
		
		// TODO: sort out Garcia BankAccount in the F1040_Mortgage_Income income/expense problem
//		for (IncomeExpenseAccount subAccount: account.getSubAccountCollection()) {
//			addSubAccounts(subAccount, matchingAccounts, pattern);
//		}
		for (Account subAccount: account.getSubAccountCollection()) {
			if (subAccount instanceof IncomeExpenseAccount) {
				addSubAccounts((IncomeExpenseAccount)subAccount, matchingAccounts, pattern);
			}
		}
	}

	public Collection<? extends ExtendableObject> getValue() {
		return value;
	}
}