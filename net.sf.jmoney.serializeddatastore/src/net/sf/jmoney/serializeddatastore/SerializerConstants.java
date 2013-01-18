/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
 *  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.serializeddatastore;

/**
 *
 * @author  Nigel
 */
public interface SerializerConstants {
	/**
	 * Corresponding int for CANCEL.
	 */
	public static final int CANCEL = 0;

	/**
	 * Corresponding int for OK.
	 */
	public static final int OK = 1;
/*
	public static final ImageIcon NEW_ICON =
		new ImageIcon(SerializerConstants.class.getResource("New16.gif"));

	public static final ImageIcon OPEN_ICON =
		new ImageIcon(SerializerConstants.class.getResource("Open16.gif"));
	
	public static final ImageIcon SAVE_ICON =
		new ImageIcon(SerializerConstants.class.getResource("Save16.gif"));
	
	public static final ImageIcon SAVE_AS_ICON =
		new ImageIcon(SerializerConstants.class.getResource("SaveAs16.gif"));
*/

	/**
	 * The language resource bundle.
	 */
/*
	public static ResourceBundle LANGUAGE =
		ResourceBundle.getBundle("net.sf.jmoney.serializeddatastore.Language");
*/    
	/**
	 * Filename extension
	 */
	public static final String FILE_EXTENSION = ".jmx"; //$NON-NLS-1$

	/**
	 * File filter name
	 */
	public static final String FILE_FILTER_NAME = "JMoney Files (*.jmx)"; //$NON-NLS-1$
}
