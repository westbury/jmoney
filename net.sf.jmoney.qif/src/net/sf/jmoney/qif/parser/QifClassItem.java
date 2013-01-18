/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (C) 2001-2008 Craig Cavanaugh and others
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

class QifClassItem {

    String name;

    String description;

	public static QifClassItem parseClass(QifReader in) throws IOException, InvalidQifFileException {
	    QifClassItem clas = new QifClassItem();

		String line = in.readLine();
		loop: while (line != null) {
			char key = line.charAt(0);
			String value = line.substring(1);

			switch (key) {
			case 'N':
				clas.name = value;
				break;
			case 'D':
				clas.description = value;
				break;
			case '^':
				break loop;
			default:
				throw new InvalidQifFileException("Unknown field in 'class' type: " + line, in);
			}
			line = in.readLine();
		}
		
		return clas;
	}
}
