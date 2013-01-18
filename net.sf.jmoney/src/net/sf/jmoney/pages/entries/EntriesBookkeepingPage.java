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

package net.sf.jmoney.pages.entries;

import net.sf.jmoney.IBookkeepingPageFactory;
import net.sf.jmoney.resources.Messages;
import net.sf.jmoney.views.AccountEditor;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;

/**
 * @author Nigel Westbury
 */
public class EntriesBookkeepingPage implements IBookkeepingPageFactory {

    /* (non-Javadoc)
     * @see net.sf.jmoney.IBookkeepingPage#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
     */
	@Override
	public void createPages(AccountEditor editor, IEditorInput input, IMemento memento) throws PartInitException {
		IEditorPart formPage = new AccountEntriesEditor();
		editor.addPage(formPage, Messages.EntriesBookkeepingPage_Label);
    }
}