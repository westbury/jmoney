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

public class QifCategory {
    
	public enum Type {
		income,
		expense
	}
	
    private String name;
    private String description;
    private boolean taxRelated = false;
    private Type type;
    private String budgetAmount;
    private String taxSchedule;    
                        
    public QifCategory() {        
        type = Type.expense;
    }

	public static QifCategory parseCategory(QifReader in) throws IOException, InvalidQifFileException {
		QifCategory cat = new QifCategory();

		String line = in.readLine();
		loop: while (line != null) {
			char key = line.charAt(0);
			String value = line.substring(1);

			switch (key) {
			case 'N':
                    cat.name = value;
    				break;
			case 'D':
                    cat.description = value;
    				break;
			case 'T':
                    cat.taxRelated = true;
    				break;
			case 'I':
	        	cat.type = Type.income;
    				break;
			case 'E':
		        	cat.type = Type.expense;
    				break;
			case 'B':
                    cat.budgetAmount = value;
    				break;
			case 'R':
                    cat.taxSchedule = value;
    				break;
			case '^': // a complete category item
                    break loop;
			default:
				throw new InvalidQifFileException("Unknown field in 'category' type: " + line, in);
			}

			line = in.readLine();
		}

		return cat;
	}        

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public boolean getTaxRelated() {
		return taxRelated;
	}

	public Type getType() {
		return type;
	}

	public String getBudgetAmount() {
		return budgetAmount;
	}

	public String getTaxSchedule() {
		return taxSchedule;
	}
}
