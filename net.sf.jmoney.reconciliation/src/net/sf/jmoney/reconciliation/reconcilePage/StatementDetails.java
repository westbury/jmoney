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

package net.sf.jmoney.reconciliation.reconcilePage;

import net.sf.jmoney.reconciliation.BankStatement;

/**
 * Class containing a statement together with the opening
 * and closing balances on that statement.  This object is
 * useful for encapsulating balances with the BankStatement
 * objects and is used within this package.
 * <P>
 * This class implements Comparable.  This enables objects of
 * this class to be maintained in a sorted map.
 * 
 * @author Nigel Westbury
 */
class StatementDetails implements Comparable<StatementDetails> {
	BankStatement statement;
	long openingBalance;
	long totalEntriesOnStatement;
	
	StatementDetails(BankStatement statement, long openingBalance, long totalEntriesOnStatement) {
		this.statement = statement;
		this.openingBalance = openingBalance;
		this.totalEntriesOnStatement = totalEntriesOnStatement;
	}
	
	long getClosingBalance() {
		return openingBalance + totalEntriesOnStatement;
	}
	
	public void adjustOpeningBalance(long amount) {
		openingBalance += amount;
	}
	
	public void adjustEntriesTotal(long amount) {
		totalEntriesOnStatement += amount;
	}
	
	public int compareTo(StatementDetails other) {
		return statement.compareTo(other.statement);
	}
}

