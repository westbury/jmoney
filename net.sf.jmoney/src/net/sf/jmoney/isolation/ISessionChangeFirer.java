/*
 *
 *  JMoney - A Personal Finance Manager
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

package net.sf.jmoney.isolation;


/**
 * @author Nigel
 *
 * Many different types of events may be fired through the
 * SessionChangeListener interface.  To avoid duplicating code to handle
 * each of these different types of events, the code where an event 
 * change originates can create an
 * implementation of this interface and pass the implementation to the
 * 'firing' methods.  When the 'firing' methods have determined a
 * listener that is to receive notification, they call the fire method
 * of this implementation.
 */
public interface ISessionChangeFirer {
	void fire(SessionChangeListener listener);
}
