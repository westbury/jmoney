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

package net.sf.jmoney.oda.pages;

import net.sf.jmoney.IBookkeepingPageFactory;
import net.sf.jmoney.views.AccountEditor;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;

/**
 * This page displays the detailed income and expense items for a
 * particular income and expense account.  Each entry is itemized.
 * <P>
 * This page requires an income and expense account.  It is therefore
 * opened not an a page under reports, but is a page in the editor
 * for the account.  This allows multiple instances of this report
 * to be open in the editor at the same time, which may or may not
 * be a good thing.
 * 
 * @author Nigel Westbury
 */
public class IncomeExpenseDetailPage implements IBookkeepingPageFactory {

	public void createPages(AccountEditor editor, IEditorInput input,
			IMemento memento) throws PartInitException {
		IEditorPart formPage = new IncomeExpenseDetailEditor();
		editor.addPage(formPage, "Income & Expense Report");
	}
}
