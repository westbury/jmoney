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

import java.util.EventListener;

/**
 * Listener interface for changes to a session.
 * <P>
 * An event is fired for every object added, every object
 * deleted, and every change made to the scalar properties
 * of an object.
 * <P>
 * For each change, only one of the methods below will be
 * called.  The <code>sessionChanged</code> method is for use
 * in all situations where none of the other three methods
 * apply.  There are currently no known situations where none
 * of the other three methods apply so the <code>sessionChanged</code>
 * method may need to be removed.
 * 
 * @author  Nigel Westbury
 */
public interface SessionChangeFirerListener extends EventListener {
	/**
	 * A change or some sort or another has occurred in the session.
	 */
    void sessionChanged(ISessionChangeFirer firer);
}
