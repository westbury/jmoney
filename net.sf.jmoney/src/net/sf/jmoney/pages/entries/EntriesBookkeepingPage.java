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
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.resources.Messages;
import net.sf.jmoney.views.AccountEditor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * @author Nigel Westbury
 */
public class EntriesBookkeepingPage implements IBookkeepingPageFactory {

    /* (non-Javadoc)
     * @see net.sf.jmoney.IBookkeepingPage#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
     */
	@Override
	public void createPages(AccountEditor editor, IEditorInput input, IMemento memento) throws PartInitException {
		/*
		 * This is creating the entries page for a BankAccount or an IncomeAndExpenseAccount.
		 * In most cases we show a standard entries list.  However if the account is a child account then we let the
		 * implementation of the parent account control the details of an entry list.
		 */
		IEditorPart entriesEditor = new AccountEntriesEditor((parent, account, filter, toolkit, handlerService) -> this.createEntriesEditor(parent, account, filter, toolkit, handlerService));
		editor.addPage(entriesEditor, Messages.EntriesBookkeepingPage_Label);
    }

	public Control createEntriesEditor(Composite parent, Account account, EntriesFilter filter, FormToolkit toolkit, IHandlerService handlerService) {
		SectionPart fEntriesSection;
		if (account.getParent() != null) {
			fEntriesSection = account.getParent().createEntriesSection(parent, account, toolkit, handlerService);
			if (fEntriesSection == null) {
				fEntriesSection = new EntriesSection(parent, account, filter, toolkit, handlerService);
			}
		} else {
			fEntriesSection = new EntriesSection(parent, account, filter, toolkit, handlerService);
		}
		return fEntriesSection.getSection();
	}
}