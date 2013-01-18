/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2010 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.importer;

import net.sf.jmoney.importer.model.ImportAccount;
import net.sf.jmoney.importer.model.ImportAccountInfo;
import net.sf.jmoney.importer.resources.Messages;
import net.sf.jmoney.importer.wizards.IAccountImportWizard;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.views.AccountEditor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class CsvImportHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IEditorPart editor = HandlerUtil.getActiveEditorChecked(event);

		AccountEditor accountEditor = (AccountEditor)editor;
		Account account = accountEditor.getAccount();

		ImportAccount accountImporterExtension = account.getExtension(ImportAccountInfo.getPropertySet(), true);
		
		/*
		 * This handler should not have been called if no table structure is set, but check
		 * anyway.
		 */
		if (accountImporterExtension.getImportDataExtensionId() == null) {
			MessageDialog.openError(shell, "Import not Available", Messages.bind(Messages.Error_AccountNotConfigured, account.getName()));
			return null;
		}
		
		IAccountImportWizard wizard = accountImporterExtension.getImportWizard();
		if (wizard == null) {
			MessageDialog.openError(shell, "Import not Available", Messages.bind("The plug-in that imports ''{0}'' is not installed", accountImporterExtension.getImportDataExtensionId()));
			return null;
		}

		/*
		 * We start the given wizard, first setting the account into it.
		 */
		wizard.init(window, account);
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.setPageSize(600, 300);
		dialog.open();

		return null;
	}
}