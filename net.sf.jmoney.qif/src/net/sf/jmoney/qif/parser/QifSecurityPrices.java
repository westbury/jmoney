/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (C) 2001-2008 Nigel Westbury and others
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
import java.util.ArrayList;
import java.util.List;


public class QifSecurityPrices {

	List<String> prices = new ArrayList<String>();
	
	public static QifSecurityPrices parseSecurityPrices(QifReader in) throws IOException, InvalidQifFileException {
		QifSecurityPrices securityPrices = new QifSecurityPrices();

		String line = in.readLine();
		while (line != null && line.charAt(0) != '^') {
    		if (line.charAt(0) != '"') {
    			throw new InvalidQifFileException("bad price data", in);
    		}

			securityPrices.prices.add(line);
			line = in.readLine();
		}
		
		return securityPrices;
	}

}
