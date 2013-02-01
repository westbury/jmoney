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

package net.sf.jmoney.paypal;

import java.util.HashSet;
import java.util.Set;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.isolation.UncommittedObjectKey;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.AccountInfo;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.TransactionManagerForAccounts;
import net.sf.jmoney.resources.Messages;
import net.sf.jmoney.wizards.WizardPropertyPage;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

public class NewAccountWizard extends Wizard implements IWorkbenchWizard {
	
	private TransactionManagerForAccounts transactionManager;
	
	private PaypalAccount newUncommittedAccount;
	
	/**
	 * This is set when 'finish' is pressed and the new account is committed.
	 */
	private PaypalAccount newCommittedAccount;

	/**
	 * Default version used from plugin.xml.
	 */
	public NewAccountWizard() {
		this(JMoneyPlugin.getDefault().getSession(), null);
	}

	/**
	 * 
	 * @param finalPropertySet the property set object of the class
	 * 		of account to create 
	 * @param parentAccount the parent account or null if this is to be
	 * 		a top level account 
	 */
	public NewAccountWizard(Session session, CapitalAccount parentAccount) {

		this.setWindowTitle(Messages.NewAccountWizard_WindowTitleCategory);
		this.setHelpAvailable(true);
		
		transactionManager = new TransactionManagerForAccounts(session.getDataManager());
		
		CapitalAccount parentAccount2 = transactionManager.getCopyInTransaction(parentAccount);
		if (parentAccount2 == null) {
			Session session2 = transactionManager.getSession();
			newUncommittedAccount = session2.createAccount(PaypalAccountInfo.getPropertySet());
		} else {
			newUncommittedAccount = parentAccount2.createSubAccount(PaypalAccountInfo.getPropertySet());
		}
	}
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void addPages()
	{
		Set<ScalarPropertyAccessor<?,?>> excludedProperties = new HashSet<ScalarPropertyAccessor<?,?>>(); 

		excludedProperties.add(PaypalAccountInfo.getTransferBankAccountAccessor());
		excludedProperties.add(PaypalAccountInfo.getTransferCreditCardAccountAccessor());
		excludedProperties.add(PaypalAccountInfo.getPaypalFeesAccountAccessor());

		// Show the page that prompts for all the property values.
		WizardPage propertyPage = new WizardPropertyPage("propertyPage", Messages.NewAccountWizard_PropertyPageTitle, Messages.NewAccountWizard_PropertyPageMessage, newUncommittedAccount, PaypalAccountInfo.getPropertySet(), AccountInfo.getNameAccessor(), excludedProperties); //$NON-NLS-1$
		addPage(propertyPage);

		// Show the page that prompts for the paypal transfer accounts
		WizardPage transferAccountsPage = new TransferAccountsPage("transferPage", newUncommittedAccount); //$NON-NLS-1$
		addPage(transferAccountsPage);

		// Show the page that prompts for the category to use for paypal fees
		WizardPage paypalFeesPage = new PaypalFeesPage("paypalFeesPage", newUncommittedAccount); //$NON-NLS-1$
		addPage(paypalFeesPage);

		WizardPage summaryPage = new SummaryPage("summaryPage"); //$NON-NLS-1$
		addPage(summaryPage);
	}
	
	@Override
	public boolean performFinish() {
		// TODO: verify properties are valid.
		
		transactionManager.commit("Add New Account");
		
		newCommittedAccount = (PaypalAccount)((UncommittedObjectKey)newUncommittedAccount.getObjectKey()).getCommittedObjectKey().getObject();
		
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

