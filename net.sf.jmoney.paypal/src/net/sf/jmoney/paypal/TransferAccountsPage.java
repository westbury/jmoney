package net.sf.jmoney.paypal;

import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ReferencePropertyAccessor;
import net.sf.jmoney.paypal.resources.Messages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * A wizard page for the Paypal transfer accounts (bank account
 * and credit card account).
 */
public class TransferAccountsPage extends WizardPage {

	private PaypalAccount paypalAccount;
	
	public TransferAccountsPage(String pageName, PaypalAccount paypalAccount) {
		super(pageName);
		this.paypalAccount = paypalAccount;
		
		setTitle(Messages.TransferAccountsPage_PageTitle);
		setMessage(Messages.TransferAccountsPage_PageMessage);
	}
	
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		
		GridLayout layout = new GridLayout(2, false);
		layout.verticalSpacing = 15;
		container.setLayout(layout);
		
		// Create the controls to edit the properties.
		
		Label label1 = new Label(container, SWT.WRAP);
		label1.setText("When importing data from Paypal, entries may be found that indicate transfers to or from a bank account. "
				+ " You should enter here the bank account that you have set up with Paypal. "
				+ " If you have not registered a bank account with Paypal then leave this blank. "
				+ " However, if you leave this blank and bank transfers are found in the Paypal import data then the import will not be accepted.");
		GridData gd1 = new GridData(500, SWT.DEFAULT);
		gd1.horizontalSpan = 2;
		label1.setLayoutData(gd1);
		
		Label propertyLabel = new Label(container, SWT.NONE);
		ReferencePropertyAccessor<BankAccount,PaypalAccount> bankAccessor = PaypalAccountInfo.getTransferBankAccountAccessor();
		propertyLabel.setText(bankAccessor.getDisplayName() + ':');
		IPropertyControl<PaypalAccount> propertyControl = bankAccessor.createPropertyControl(container);
		propertyControl.load(paypalAccount);
		
		Label label2 = new Label(container, SWT.WRAP);
		label2.setText("When importing data from Paypal, entries may be found that indicate that funds have been credited to the account through a charge to a credit card. "
				+ " You should enter here the credit card account that you have set up with Paypal. "
				+ " If you have not registered a credit card with Paypal then leave this blank. "
				+ " However, if you leave this blank and credit card charges are found in the Paypal import data then the import will not be accepted.");
		GridData gd2 = new GridData(500, SWT.DEFAULT);
		gd2.horizontalSpan = 2;
		label2.setLayoutData(gd2);
		
		Label propertyLabel2 = new Label(container, SWT.NONE);
		ReferencePropertyAccessor<BankAccount,PaypalAccount> creditCardAccessor = PaypalAccountInfo.getTransferCreditCardAccountAccessor();
		propertyLabel2.setText(creditCardAccessor.getDisplayName() + ':');
		IPropertyControl<PaypalAccount> propertyControl2 = creditCardAccessor.createPropertyControl(container);
		propertyControl2.load(paypalAccount);
		
		setPageComplete(false);
		
		setControl(container);		
	}
	
	@Override
	public boolean canFlipToNextPage() {
		/*
		 * This method controls whether the 'Next' button is enabled.
		 */
		return true;
	}
}