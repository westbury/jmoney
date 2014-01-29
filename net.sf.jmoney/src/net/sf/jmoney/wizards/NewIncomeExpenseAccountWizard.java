/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2007 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.wizards;


import java.util.HashSet;
import java.util.Set;

import net.sf.jmoney.isolation.UncommittedObjectKey;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.AccountInfo;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.IncomeExpenseAccountInfo;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.TransactionManagerForAccounts;
import net.sf.jmoney.resources.Messages;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;

public class NewIncomeExpenseAccountWizard extends Wizard implements INewWizard {
	public static final String ID = "net.sf.jmoney.wizards.new.categoryaccount"; //$NON-NLS-1$

	private TransactionManagerForAccounts transactionManager;
	
	private IncomeExpenseAccount newUncommittedAccount;
	
	/**
	 * This is set when 'finish' is pressed and the new account is committed.
	 */
	private IncomeExpenseAccount newCommittedAccount;

	/**
	 * Default version used from plugin.xml.
	 */
	public NewIncomeExpenseAccountWizard() {
		this.setWindowTitle(Messages.NewAccountWizard_WindowTitleCategory);
		this.setHelpAvailable(true);
	}

	/**
	 * 
	 * @param session
	 * @param parentAccount the parent account or null if this is to be
	 * 		a top level account 
	 */
	public NewIncomeExpenseAccountWizard(Session session, IncomeExpenseAccount parentAccount) {
		this(); 
		createAccount(session, parentAccount);
	}

	private void createAccount(Session session,	IncomeExpenseAccount parentAccount) {
		transactionManager = new TransactionManagerForAccounts(session.getDataManager());
		
		IncomeExpenseAccount parentAccount2 = transactionManager.getCopyInTransaction(parentAccount);
		if (parentAccount2 == null) {
			Session session2 = transactionManager.getSession();
			newUncommittedAccount = session2.createAccount(IncomeExpenseAccountInfo.getPropertySet());
		} else {
			newUncommittedAccount = parentAccount2.createSubAccount();
		}
	}
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		IDataManagerForAccounts datastoreManager = (IDataManagerForAccounts)window.getActivePage().getInput();
		if (datastoreManager == null) {
			MessageDialog.openError(window.getShell(), "Unavailable", "You must open an accounting session before you can create an account.");
			return;
		}
		
		Session session = datastoreManager.getSession();

		/*
		 * We could get the parent account from the selection.  However it seems to cause less confusion
		 * if the account is created as a top level account whenever this wizard is invoked from the 'new...'
		 * drop-down.
		 */
		createAccount(session, null);
	}
	
	@Override
	public void addPages()
	{
		// Show the page that prompts for all the property values.
		Set<ScalarPropertyAccessor<?,? extends IncomeExpenseAccount>> excludedProperties = new HashSet<ScalarPropertyAccessor<?,? extends IncomeExpenseAccount>>(); 
		WizardPage propertyPage = new WizardPropertyPage<IncomeExpenseAccount>("propertyPage", Messages.NewAccountWizard_PropertyPageTitle, Messages.NewAccountWizard_PropertyPageMessage, newUncommittedAccount, IncomeExpenseAccountInfo.getPropertySet(), AccountInfo.getNameAccessor(), excludedProperties); //$NON-NLS-1$
		addPage(propertyPage);

		WizardPage summaryPage = new SummaryPage("summaryPage"); //$NON-NLS-1$
		addPage(summaryPage);
	}
	
	@Override
	public boolean performFinish() {
		// TODO: verify properties are valid.
		
		transactionManager.commit("Add New Account");
		
		newCommittedAccount = (IncomeExpenseAccount)((UncommittedObjectKey)newUncommittedAccount.getObjectKey()).getCommittedObjectKey().getObject();
		
		return true;
	}
	
	class SummaryPage extends WizardPage {
		
		SummaryPage(String pageName) {
			super(pageName);
			setTitle(Messages.NewAccountWizard_SummaryPageTitle);
			setMessage(Messages.NewAccountWizard_SummaryPageMessage);
		}
		
		@Override
		public void createControl(Composite parent) {
			Composite container = new Composite(parent, SWT.NONE);
			
			GridLayout layout = new GridLayout();
			layout.marginWidth = 10;
			layout.marginHeight =10;
			container.setLayout(layout);
			
			GridData gd1 = new GridData();
			gd1.grabExcessHorizontalSpace = true;
			gd1.horizontalAlignment = SWT.FILL;
			gd1.widthHint = 300;
			
			Label introText = new Label(container, SWT.WRAP);
			introText.setText(Messages.NewAccountWizard_SummaryPageContent);
			introText.setLayoutData(gd1);
			
			setControl(container);			
		}
		
		@Override
		public void performHelp() {
			MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.OK);
			messageBox.setMessage(Messages.NewAccountWizard_SummaryPageHelpMessage);
			messageBox.open();
		}
	}

	public Account getNewAccount() {
		return newCommittedAccount;
	}
}

