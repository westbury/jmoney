/*
 * 
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004,2009 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.importer.wizards;

import net.sf.jmoney.importer.Activator;
import net.sf.jmoney.importer.model.AccountAssociation;
import net.sf.jmoney.importer.model.ImportAccount;
import net.sf.jmoney.importer.model.ImportAccountInfo;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * A wizard to import data from a comma-separated file that has been down-loaded
 * into a file on the local machine.
 * <P>
 * This wizard is a single page wizard that asks only for the file.
 */
public abstract class CsvImportToAccountWizard extends CsvImportWizard implements IAccountImportWizard {

	/**
	 * Set when <code>importFile</code> is called.
	 */
	private Account accountInsideTransaction;

	private Account accountOutsideTransaction;

	public CsvImportToAccountWizard() {
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("CsvImportToAccountWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("CsvImportToAccountWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	/**
	 * This form of this method is calledthe constructor is called when started 
	 * 
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 */
	@Override
	public void init(IWorkbenchWindow window, Account account) {
		this.window = window;
		this.accountOutsideTransaction = account;

		mainPage = new CsvImportWizardPage(window);
		addPage(mainPage);
	}
	
	/**
	 * Given an id for an account association, returns the account that is associated with the
	 * account into which we are importing.
	 * <P>
	 * This account is inside the transaction.
	 * 
	 * @param id
	 * @return
	 */
	protected Account getAssociatedAccount(String id) {
		ImportAccount a = accountInsideTransaction.getExtension(ImportAccountInfo.getPropertySet(), false);
		if (a != null) {
			for (AccountAssociation aa : a.getAssociationCollection()) {
				if (aa.getId().equals(id)) {
					return aa.getAccount();
				}
			}
		}
		return null;
	}

	@Override
	protected void startImport(TransactionManager transactionManager) throws ImportException {
		accountInsideTransaction = transactionManager.getCopyInTransaction(accountOutsideTransaction);
		setAccount(accountInsideTransaction);
	}

	protected abstract void setAccount(Account accountInsideTransaction) throws ImportException;

	/**
	 * This method returns a label that describes the source and is suitable for use
	 * in labels and messages shown to the user.  This will typically be the name of the
	 * bank or brokerage firm.
	 */
	protected abstract String getSourceLabel();
}