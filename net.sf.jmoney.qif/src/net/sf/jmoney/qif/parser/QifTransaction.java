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
 *  Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package net.sf.jmoney.qif.parser;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class QifTransaction {

	private String date;
    private BigDecimal amount;
    private Character status = ' ';
    private String number;
    private String payee = null;
    private String memo = "";
    private String category = null;
    private String U;
    private List<QifSplitTransaction> splits = new ArrayList<QifSplitTransaction>();
    private List<String> addressLines = new ArrayList<String>();

    private QifFile qifFile;
    
	public QifTransaction(QifFile qifFile) {
		this.qifFile = qifFile;
	}

	public static QifTransaction parseTransaction(QifReader in, QifFile qifFile) throws IOException, InvalidQifFileException {
        QifTransaction tran = new QifTransaction(qifFile);

	    String splitCategory = null;
	    String splitMemo = null;
	    BigDecimal splitAmount = null;
	    String splitPercentage = null;

	    String line = in.readLine();
	    loop: while (line != null) {
	    	if (line.length() == 0) {
	    		throw new InvalidQifFileException("unexpected blank line", in);
	    	}

	    	char key = line.charAt(0);
	    	String value = line.substring(1);

	    	switch (key) {
	    	case 'D':
	    		tran.date = value;
	    		qifFile.processDate(value, in);
	    		break;
	    	case 'U':
	    		tran.U = value;
	    		break;
	    	case 'T':
	    		tran.amount = QifFile.parseMoney(value);
	    		break;
	    	case 'C':
	    		if (value.length() != 1) {
	    			throw new InvalidQifFileException("Reconcile status must be one character", in);
	    		}
	    		tran.status = value.charAt(0);
	    		break;
	    	case 'P':
	    		tran.payee = value;
	    		break;
	    	case 'L':
	    		tran.category = value;
	    		break;
	    	case 'N': // trans type for inv accounts
	    		tran.number = value;
	    		break;
	    	case 'M':
	    		tran.memo = value;
	    		break;
	    	case 'A':
	    		tran.addressLines.add(value);
	    		break;
	    	case '^':
	    		break loop;
	    	case 'S':
	    		if (splitCategory != null) {
	    			tran.splits.add(new QifSplitTransaction(splitCategory, splitMemo, splitAmount, splitPercentage));
	    			splitCategory = null;
	    			splitMemo = null;
	    			splitAmount = null;
	    			splitPercentage = null;
	    		}
	    		splitCategory = value;
	    		break;
	    	case 'E':
	    		if (splitMemo != null) {
	    			tran.splits.add(new QifSplitTransaction(splitCategory, splitMemo, splitAmount, splitPercentage));
	    			splitCategory = null;
	    			splitMemo = null;
	    			splitAmount = null;
	    			splitPercentage = null;
	    		}
	    		splitMemo = value;
	    		break;
	    	case '$':
	    		if (splitAmount != null) {
	    			tran.splits.add(new QifSplitTransaction(splitCategory, splitMemo, splitAmount, splitPercentage));
	    			splitCategory = null;
	    			splitMemo = null;
	    			splitAmount = null;
	    			splitPercentage = null;
	    		}
	    		splitAmount = QifFile.parseMoney(value);
	    		break;
	    	case '%':
	    		if (splitPercentage != null) {
	    			tran.splits.add(new QifSplitTransaction(splitCategory, splitMemo, splitAmount, splitPercentage));
	    			splitCategory = null;
	    			splitMemo = null;
	    			splitAmount = null;
	    			splitPercentage = null;
	    		}
	    		splitPercentage = value;
	    		break;
	    	default:
				throw new InvalidQifFileException("Unknown field in 'transaction' type: " + line, in);
	    	}
	    	line = in.readLine();
	    }

	    // TODO: Do a few more checks on splits.
	    // Perhaps the splits transaction constructor
	    // should do the checks?
	    if (splitCategory != null) {
	    	tran.splits.add(new QifSplitTransaction(splitCategory, splitMemo, splitAmount, splitPercentage));
	    }
        	
		return tran;
	}
	
	public QifDate getDate() {
		return qifFile.parseDate(date);
	}
	
	public BigDecimal getAmount() {
		return amount;
	}
	
	public char getStatus() {
		if (status == 'x' || status == 'X') {
			return 'X';
		} else if (status == '*') {
			return '*';
		} else {
			return ' ';
		}
	}

	public String getCheckNumber() {
		return number;
	}

	public String getPayee() { 
		return payee;
	}

	public String getMemo() {
		return memo;
	}

	public QifCategoryLine getCategory() {
		return (category == null) ? null : new QifCategoryLine(category);
	}

	public String getU() {
		return U;
	}

	public List<QifSplitTransaction> getSplits() {
		return Collections.unmodifiableList(splits);
	}

	public List<String> getAddressLines() {
		return Collections.unmodifiableList(addressLines);
	}
}
