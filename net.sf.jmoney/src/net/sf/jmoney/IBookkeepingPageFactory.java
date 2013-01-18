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

import net.sf.jmoney.views.AccountEditor;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;

/**
 * Interface that must be implemented by all classes that
 * implement an extension to the net.sf.jmoney.pages extension
 * point.
 *
 * @author Nigel Westbury
 */
public interface IBookkeepingPageFactory {
	/**
	 * Create the form page.
	 * 
	 * @param editor The editor to which the page is added.
	 * @param memento A memento containing the state of the page
	 * 			when the page was last closed.  If no prior state
	 * 			is available then <code>memento</code> will be null.
	 */
	void createPages(AccountEditor editor, IEditorInput input, IMemento memento) throws PartInitException;
}
