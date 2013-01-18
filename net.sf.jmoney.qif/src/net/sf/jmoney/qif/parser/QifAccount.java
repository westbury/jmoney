/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (C) 2001-2008 Craig Cavanaugh, Johann Gyger, and others
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

package net.sf.jmoney.qif.parser;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class QifAccount {       
	private String name;
	private String type;
	private String description;
	private String notes;
	private BigDecimal creditLimit;
	private String statementBalanceDate;
	private BigDecimal statementBalance;

	private List<QifTransaction> transactions = new ArrayList<QifTransaction>();
	private List<QifInvstTransaction> invstTransactions = new ArrayList<QifInvstTransaction>();

	private String transactionType;
	
	// Is there a code, or just set from first transaction?
	public long startBalance = 0;

    private QifFile qifFile;
    
	public QifAccount(QifFile qifFile) {
		this.qifFile = qifFile;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Name: " + name + "\n");
		buf.append("Type: " + type + "\n");
		buf.append("Descrition: " + description + "\n");                     
		return buf.toString();        
	}

	public static QifAccount parseAccount(QifReader in, QifFile qifFile) throws IOException, InvalidQifFileException {
		QifAccount acc = new QifAccount(qifFile);

		String line = in.readLine();
		loop: while (line != null) {
			char key = line.charAt(0);
			String value = line.substring(1);

			switch (key) {
			case 'N':
				acc.name = value;
				break;
			case 'T':
				acc.type = value;
				break;
			case 'D':
				acc.description = value;
				break;
			case 'A':
				acc.notes = value;
				break;
			case 'L':
				acc.creditLimit = QifFile.parseMoney(value);
				break;
			case '/':
				acc.statementBalanceDate = value;
				break;
			case '$':
				acc.statementBalance = QifFile.parseMoney(value);
				break;
			case 'X':
				// must be GnuCashToQIF... not sure what it is??? ignore it.
				break;
			case 'B': 
				// This is the 'balance' in some files.  It is undocumented so ignore.
				break;
			case '^':
				break loop;
			default:
				throw new InvalidQifFileException("Unknown field in 'account' type: " + line, in);
			}
			line = in.readLine();
		}

		line = in.peekLine();
		if (line != null
				&& QifFile.startsWith(line, "!Type:")) {
			String typeString = QifFile.getTypeString(line, in);

			if (typeString.equals("Cash")
					|| typeString.equals("Bank")
					|| typeString.equals("CCard")
					|| typeString.startsWith("Oth")
					|| typeString.equals("Invst")) {

				acc.transactionType = typeString;

				// must be transactions that follow

				line = in.readLine();    // Move onto line following !Type
				do {
					if (typeString.equals("Invst")) {
						QifInvstTransaction transaction = QifInvstTransaction.parseTransaction(in, qifFile);
						acc.invstTransactions.add(transaction);
					} else {
						QifTransaction transaction = QifTransaction.parseTransaction(in, qifFile);
						acc.transactions.add(transaction);
					}
				} while (in.peekLine() != null && !in.peekLine().startsWith("!"));
			}
		}
		return acc;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getDescription() {
		return description;
	}

	public String getNotes() {
		return notes;
	}

	public BigDecimal getCreditLimit() {
		return creditLimit;
	}

	public QifDate getStatementBalanceDate() {
		return qifFile.parseDate(statementBalanceDate);
	}

	public BigDecimal getStatementBalance() {
		return statementBalance;
	}

	/**
	 * Returns the type of transactions in this account.
	 * 
	 * An account may optionally have transactions following it. If this account
	 * was exported as a list of accounts only (no transactions) then there will
	 * not be a transaction type and the returned value will be null. In such a
	 * situation there is no way of knowing the type of the account (bank,
	 * credit card, investment, etc). If there are following transactions then
	 * the type will be returned.
	 * 
	 * Note that it is possible that there will be a !type: following an account
	 * but then no transactions follow. In such a case the type will be returned
	 * by this method but the transaction list will be empty.
	 * 
	 * @return the text following !Type: in the line that follows this account,
	 *         or null if there is no such line following this account or if the
	 *         type is not a transaction type and thus not connected to this
	 *         account
	 */
	public String getTransactionType() {
		return transactionType;
	}

	public List<QifTransaction> getTransactions() {
		return Collections.unmodifiableList(transactions);
	}
	
	public List<QifInvstTransaction> getInvstTransactions() {
		return Collections.unmodifiableList(invstTransactions);
	}
}