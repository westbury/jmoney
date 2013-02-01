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

package net.sf.jmoney.property.wizards;


import java.util.HashSet;
import java.util.Set;

import net.sf.jmoney.isolation.UncommittedObjectKey;
import net.sf.jmoney.model2.CommodityInfo;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.TransactionManagerForAccounts;
import net.sf.jmoney.property.model.RealProperty;
import net.sf.jmoney.property.model.RealPropertyInfo;
import net.sf.jmoney.wizards.WizardPropertyPage;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;

public class NewPropertyWizard extends Wizard {
	
	private ExtendablePropertySet<? extends RealProperty> realAssetPropertySet;

	private TransactionManagerForAccounts transactionManager;
	
	private RealProperty newUncommittedAccount;
	
	/**
	 * This is set when 'finish' is pressed and the new stock is committed.
	 */
	private RealProperty newCommittedAsset;
	
	/**
	 * 
	 * @param finalPropertySet the property set object of the class
	 * 		of stock to create 
	 */
	public NewPropertyWizard(Session session) {
		this.realAssetPropertySet = RealPropertyInfo.getPropertySet();
		
		this.setWindowTitle("Create a New Stock");
		this.setHelpAvailable(true);
		
		transactionManager = new TransactionManagerForAccounts(session.getDataManager());
		
		Session session2 = transactionManager.getSession();
		newUncommittedAccount = session2.createCommodity(realAssetPropertySet);
	}
	
	@Override
	public void addPages()
	{
		// Show the page that prompts for all the property values.
		Set<ScalarPropertyAccessor<?,?>> excludedProperties = new HashSet<ScalarPropertyAccessor<?,?>>(); 
		WizardPage propertyPage = new WizardPropertyPage("propertyPage", "Stock Properties", "Enter values for the stock properties", newUncommittedAccount, realAssetPropertySet, CommodityInfo.getNameAccessor(), excludedProperties);
		addPage(propertyPage);

		WizardPage summaryPage = new SummaryPage("summaryPage");
		addPage(summaryPage);
	}
	
	@Override
	public boolean performFinish() {
		// TODO: verify properties are valid.
		
		transactionManager.commit("Add New Real Asset");
		
		newCommittedAsset = (RealProperty)((UncommittedObjectKey)newUncommittedAccount.getObjectKey()).getCommittedObjectKey().getObject();
		
		return true;
	}
	
	class SummaryPage extends WizardPage {
		
		SummaryPage(String pageName) {
			super(pageName);
			setTitle("Summary");
			setMessage("");
		}
		
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
			introText.setText("The new stock has been setup.");
			introText.setLayoutData(gd1);
			
			setControl(container);			
		}
		
		@Override
		public void performHelp() {
			MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.OK);
			messageBox.setMessage("No help is available for this page.");
			messageBox.open();
		}
	}

	public RealProperty getNewAsset() {
		return newCommittedAsset;
	}
}

