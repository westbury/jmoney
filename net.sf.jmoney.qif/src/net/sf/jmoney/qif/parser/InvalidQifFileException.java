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
 * The QIF file had invalid contents.  The message
 * should describe the error together with the line
 * number.
 */
public class InvalidQifFileException extends Exception {

	private static final long serialVersionUID = 1L;

	private QifReader in;
	
	public InvalidQifFileException(String message, QifReader in) {
		super(message);
		this.in = in;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + "(line " + in.getLineNumber() + ")"; 
	}

	public InvalidQifFileException(String message, Exception e) {
		super(message, e);
	}
	
	
}
