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

package net.sf.jmoney;

import org.eclipse.ui.IMemento;

/**
 * An implementation of this interface is returned by all implementations
 * of the createFormPage method in the IBookkeepingPageFactory interface.
 *
 * @author Nigel Westbury
 */
public interface IBookkeepingPage {
	/**
	 * Save the state of the page in a memento.  This method
	 * is called when the editor containing this page is closed.
	 * If the editor is re-created, the memento will be passed
	 * to the <code>createFormPage</code> method.
	 */
	void saveState(IMemento memento);
}
