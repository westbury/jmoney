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

import java.math.BigDecimal;

public class QifSplitTransaction {
    public String category;
    public String memo;
    public BigDecimal amount;
    public String percentage;
    
    QifSplitTransaction(String category, String memo,
			BigDecimal amount, String percentage) {
		this.category = category;
		this.memo = memo;
		this.amount = amount;
		this.percentage = percentage;
	}

    @Override
	public String toString() {
        StringBuffer buf = new StringBuffer();        
        buf.append("Memo: " + memo + "\n");
        buf.append("Category: " + category + "\n");
        if (amount != null) {
            buf.append("Amount:" + amount.toString() + "\n");
        }                       
        return buf.toString();
    }

	public String getMemo() {
		return memo;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public QifCategoryLine getCategory() {
		return new QifCategoryLine(category);
	}
}
