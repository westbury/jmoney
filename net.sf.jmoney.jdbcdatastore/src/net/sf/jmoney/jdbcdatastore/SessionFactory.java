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

package net.sf.jmoney.jdbcdatastore;

import net.sf.jmoney.JMoneyPlugin;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * This factory restores an open session by opening a
 * JDBC database.
 * 
 * Although an IMemento is passed, the session is always
 * based on the JDBC options set in the preference page
 * (the user cannot select another database without changing
 * the preferences), so the memento is not used.
 */
public class SessionFactory implements IElementFactory {
//	private IWorkbenchWindow window;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IElementFactory#createElement(org.eclipse.ui.IMemento)
	 */
	public IAdaptable createElement(IMemento memento) {
		// TODO This should probably be a silent method.
		IWorkbenchWindow window = JMoneyPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
		return JDBCDatastorePlugin.getDefault().readSession(window);
	}
}