/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.reconciliation;

import net.sf.jmoney.model2.CurrencyAccount;

/**
 * Interface containing methods that execute queries against the datastore.
 * <P>
 * Plug-ins that implement a datastore may optionally implement this interface.
 * Consumers obtain this interface through the getAdapter method in the session
 * object.
 * <P>
 * See document on 'datastore utilities' for a description
 * of the overall design.
 * <P>
 * @author Nigel Westbury
 */
public interface IReconciliationQueries {

	/**
	 * Gets the set of all bank statements in this account.
	 * The datastore model does not maintain a list of bank statements,
	 * so the datastore implementation must scan the entries to
	 * find all statements on which an entry occurs.  This method
	 * also returns the final balances on each statement.
	 * <P>
	 * The statements are sorted into order.
	 * 
	 * @param account
	 * @return
	 */
	BankStatementAndBalance[] getStatements(CurrencyAccount account);

	/**
	 * A class containing a statement and balance.  An array
	 * of this class is returned by the getStatements method.
	 */
	public class BankStatementAndBalance {
		public BankStatement bankStatement;
		public long balance;
		
		public BankStatementAndBalance(BankStatement bankStatement, long balance) {
			this.bankStatement = bankStatement;
			this.balance = balance;
		}
	}
}
