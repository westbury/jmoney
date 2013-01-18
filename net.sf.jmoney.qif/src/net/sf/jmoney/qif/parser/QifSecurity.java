/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (C) 2001-2008 Craig Cavanaugh, Nigel Westbury, and others
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


public class QifSecurity {

	private String name;
    private String description;
	private String symbol;
	private String type;
	private String goal;

	public static QifSecurity parseSecurity(QifReader in) throws IOException, InvalidQifFileException {
		QifSecurity security = new QifSecurity();

		String line = in.readLine();
		loop: while (line != null) {
			if (line.length() == 0) {
				throw new RuntimeException("unexpected blank line");
			}

			char key = line.charAt(0);
			String value = line.substring(1);

			switch (key) {
			case 'N':
				security.name = value;
				break;
			case 'D':
				security.description = value;
				break;
			case 'S':
				security.symbol = value;
				break;
			case 'T':
				security.type = value;
				break;
			case 'G':
				security.goal = value;
				break;
			case '^':
				break loop;
			default:
				throw new InvalidQifFileException("Unknown field in 'security' type: " + line, in);
			}
			line = in.readLine();
		}
		
		return security;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getSymbol() {
		return symbol;
	}

	public String getType() {
		return type;
	}

	public String getGoal() {
		return goal;
	}
}
