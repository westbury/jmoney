/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2006 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.stocks.model;

import net.sf.jmoney.model2.Currency;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogMessageArea;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * An input dialog that allows the user to configure the methods for importing statement data
 * for a particular account.
 * 
 * @author Nigel Westbury
 */
class RatesDialog extends Dialog {
	
	private String title;
	
	private RatesTable rates;
	
	private Currency currencyForFormatting;
	
	private DialogMessageArea messageArea;

	private RatesEditorControl control;

	/**
	 * Ok button widget.
	 */
	private Button okButton;

	Image errorImage;

	/**
	 * Creates an input dialog with OK and Cancel buttons. Note that the dialog
	 * will have no visual representation (no widgets) until it is told to open.
	 * <p>
	 * Note that the <code>open</code> method blocks for input dialogs.
	 * </p>
	 * 
	 * @param parentShell
	 *            the parent shell
	 * @param currency 
	 */
	public RatesDialog(Shell parentShell, String title, RatesTable rates, Currency currencyForFormatting) {
		super(parentShell);
		
		this.title = title;
		this.rates = rates;
		this.currencyForFormatting = currencyForFormatting;
		
		// Load the error indicator
//		URL installURL = ReconciliationPlugin.getDefault().getBundle().getEntry("/icons/error.gif");
//		errorImage = ImageDescriptor.createFromURL(installURL).createImage();
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {

			RatesTable newRates = control.getRatesTable();
			if (newRates == null) {
				// TODO: Set the error message.
				return;
			}
			
			rates = newRates;
		}
		super.buttonPressed(buttonId);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(title);
	}
	
	@Override
	public boolean close() {
		boolean closed = super.close();
		
		// Dispose the image
		if (closed) {
//			errorImage.dispose();
		}
		
		return closed;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		okButton = createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		composite.setLayout(new GridLayout(1, false));

		// Message label
		messageArea = new DialogMessageArea();
		messageArea.createContents(composite);
		
		// What are these for?
//		messageArea.setTitleLayoutData(createMessageAreaData());
//		messageArea.setMessageLayoutData(createMessageAreaData());

		Label label = new Label(composite, SWT.WRAP);
		label.setText("Setup the rates.  This allows JMoney to calculate the various commissions and taxes for you. " +
				"A commission or tax may include a fixed amount and percentages.  The percentages may vary based on the amount of the purchase or sale.");

		GridData messageData = new GridData();
		Rectangle rect = getShell().getMonitor().getClientArea();
		messageData.widthHint = rect.width/2;
		label.setLayoutData(messageData);

		control = new RatesEditorControl(composite);
		control.setRatesTable(rates, currencyForFormatting);

		applyDialogFont(composite);
		return composite;
	}

	/**
	 * Sets or clears the error message.
	 * If not <code>null</code>, the OK button is disabled.
	 * 
	 * @param errorMessage
	 *            the error message, or <code>null</code> to clear
	 */
	public void updateErrorMessage() {
		// TODO:
	
//		if (errorMessage == null) {
			messageArea.clearErrorMessage();
//		} else {
//			messageArea.updateText(errorMessage, IMessageProvider.ERROR);
//		}
		
//		errorMessageText.setText(errorMessage == null ? "" : errorMessage); //$NON-NLS-1$

		// If called during createDialogArea, the okButton
		// will not have been created yet.
		if (okButton != null) {
			okButton.setEnabled(true);
		}
//		errorMessageText.getParent().update();
	}
	
	public RatesTable getRates() {
		return rates;
	}
}
