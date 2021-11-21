/*
 * 
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2021 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.ebay;


import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Page that does not actually get any information from the user
 */
public class EbayImportWizardPage extends WizardPage  {

	private static final String EBAY_IMPORT_WIZARD_PAGE = "ebayImportWizardPage"; //$NON-NLS-1$

	/**
	 * Create an instance of this class
	 */
	protected EbayImportWizardPage(IWorkbenchWindow window) {
		super(EBAY_IMPORT_WIZARD_PAGE);
		setTitle("Ebay Orders Import from Clipboard");
		setDescription("Import data from an Ebay orders page, with text copied to the clipboard.");
	}

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		GridLayout gdContainer = new GridLayout(2, false);
		gdContainer.horizontalSpacing = 10;
		gdContainer.verticalSpacing = 25;
		gdContainer.marginWidth = 20;
		gdContainer.marginHeight = 20;
		composite.setLayout(gdContainer);

		Label label = new Label(composite, SWT.WRAP);
		label.setText(
				"This wizard will import data from Ebay orders page.  You must view the order hisntory in a web browser (show orders), select all the text, and copy to the clipboard." +
				" With the text in the clipboard, click 'Finish'..");
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		gd.widthHint = 600;
		label.setLayoutData(gd);
		
		setPageComplete(true);
		
		restoreWidgetValues();
		
		setControl(composite);

		Dialog.applyDialogFont(composite);
	}

	/**
	 * Hook method for restoring widget values to the values that they held last
	 * time this wizard was used to completion.
	 */
	protected void restoreWidgetValues() {
		// Nothing to restore
	}
}
