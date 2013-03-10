package net.sf.jmoney.paypal;

import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.ReferencePropertyAccessor;
import net.sf.jmoney.paypal.resources.Messages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * A wizard page that allows to user to decide how to account for the Paypal fees.
 */
public class PaypalFeesPage extends WizardPage {

	private PaypalAccount paypalAccount;
	
	public PaypalFeesPage(String pageName, PaypalAccount paypalAccount) {
		super(pageName);
		this.paypalAccount = paypalAccount;
		
		setTitle(Messages.PaypalFeesPage_PageTitle);
		setMessage(Messages.PaypalFeesPage_PageMessage);
	}
	
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		
		GridLayout layout = new GridLayout(2, false);
		layout.verticalSpacing = 15;
		container.setLayout(layout);
		
		// Create the controls to edit the properties.
		
		Label label1 = new Label(container, SWT.WRAP);
		label1.setText("Paypal may have charged fees."
				+ " There are various ways these fees can be accounted for when importing data.");
		GridData gd1 = new GridData(500, SWT.DEFAULT);
		gd1.horizontalSpan = 2;
		label1.setLayoutData(gd1);
		
		Label propertyLabel = new Label(container, SWT.NONE);
		ReferencePropertyAccessor<IncomeExpenseAccount,PaypalAccount> feesAccessor = PaypalAccountInfo.getPaypalFeesAccountAccessor();
		propertyLabel.setText(feesAccessor.getDisplayName() + ':');
		IPropertyControl<PaypalAccount> propertyControl = feesAccessor.createPropertyControl(container);
		propertyControl.load(paypalAccount);
		
		setPageComplete(false);
		
		setControl(container);		
	}
	
	@Override
	public boolean canFlipToNextPage() {
		/*
		 * This method controls whether the 'Next' button is enabled.
		 */
		return paypalAccount.getPaypalFeesAccount() != null;
	}
}