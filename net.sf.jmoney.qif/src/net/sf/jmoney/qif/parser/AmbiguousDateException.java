/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2008 Nigel Westbury <westbury@users.sourceforge.net>
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


/**
 * This exception will be thrown only if
 * 
 * <UL>
 * <LI>The date format passed to the QifFile constructor
 * was DetermineFromFile</LI>
 * <LI>The file contained at least one date but no
 * dates where the day of the month was more than 12</LI>
 * </UL>
 * 
 * If this exception is throw then the caller should make some
 * attempt at the date format, perhaps by looking at the machine's
 * locale or perhaps by asking the user, then calling the constructor
 * again with a specific date format.
 */
public class AmbiguousDateException extends Exception {

	private static final long serialVersionUID = 1L;

	public AmbiguousDateException() {
		super();
	}
}
