/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (C) 2008 Nigel Westbury
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

public enum QifDateFormat {
	/**
	 * Always assume mm-dd-yy
	 */
	UsDateOrder,

	/**
	 * Always assume dd-mm-yy
	 */
	EuDateOrder,

	/**
	 * Look to see what is in the file. If there are dates but none with a day
	 * of month of more than 12 then throw an exception.
	 */
	DetermineFromFile,

	/**
	 * Look to see what is in the file as though DetermineFromFile was set.  However
	 * if that cannot determine the date order then use the system date.
	 */
	DetermineFromFileAndSystem
}
