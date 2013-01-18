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

package net.sf.jmoney.reconciliation.reconcilePage;

import java.util.Calendar;
import java.util.Date;

import net.sf.jmoney.AlternativeContentLayout;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.DateControl;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Once an import file has been selected, this dialog box appears to get more
 * information from the user and to confirm the import.
 * 
 * Specifically, this dialog box asks for a range of dates to import. Either the
 * start date, the end date or both may be blank to indicate there is no limit
 * to the entries to be imported.
 * 
 * @author Nigel Westbury
 */
public class ImportStatementDialog extends Dialog {
	
	/**
	 * The start date, or null if no start date
	 */
	private Date startDate = null;

	/**
	 * The end date, or null if no start date
	 */
	private Date endDate = null;

	/**
	 * Error message label widget.
	 */
	private Text errorMessageText;
	
	private AlternativeContentLayout alternativeContentLayout;

	DateControl startDateControl;
	DateControl endDateControl;
	
	/**
	 * Creates an input dialog with OK and Cancel buttons. Note that the dialog
	 * will have no visual representation (no widgets) until it is told to open.
	 * <p>
	 * Note that the <code>open</code> method blocks for input dialogs.
	 * </p>
	 * 
	 * @param parentShell
	 *            the parent shell
	 * @param lastStatement
	 */
	public ImportStatementDialog(Shell parentShell, Date earliestDateInFile, Date latestDateInFile, Date statementDate) {
		super(parentShell);

		if (statementDate == null) {
			this.startDate = earliestDateInFile;
			this.endDate = latestDateInFile;
		} else {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(statementDate);
			calendar.add(Calendar.MONTH, -1);
			calendar.add(Calendar.DAY_OF_MONTH, +1);

			startDateControl.setDate(calendar.getTime());
			endDateControl.setDate(statementDate);
		}
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			startDate = startDateControl.getDate();
			endDate = endDateControl.getDate();
		}
		super.buttonPressed(buttonId);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
			shell.setText("New Statement");
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
			Label label = new Label(composite, SWT.WRAP);
			label.setText("You may restrict the import to a range of dates.");
			
			GridData messageData = new GridData();
			Rectangle rect = getShell().getMonitor().getClientArea();
			messageData.widthHint = rect.width/2;
			label.setLayoutData(messageData);
		
		
		Composite alternativeContentContainer = new Composite(composite, 0);
		alternativeContentLayout = new AlternativeContentLayout();
		alternativeContentContainer.setLayout(alternativeContentLayout);

		Label startDateLabel = new Label(composite, SWT.NONE);
		startDateLabel.setText("Start Date:");
		GridData numberLabelData = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_CENTER);
		numberLabelData.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		startDateLabel.setLayoutData(numberLabelData);
		startDateLabel.setFont(parent.getFont());

		startDateControl = new DateControl(composite);
		startDateControl.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));
		startDateControl.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				boolean isDateValid = (startDateControl.getDate() != null);
				setErrorMessage(isDateValid ? null : "Date must be in the format " + JMoneyPlugin.getDefault().getDateFormat());
			}
		});

		Label endDateLabel = new Label(composite, SWT.NONE);
		endDateLabel.setText("End Date:");
		GridData dateLabelData = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_CENTER);
		dateLabelData.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		endDateLabel.setLayoutData(dateLabelData);
		endDateLabel.setFont(parent.getFont());

		endDateControl = new DateControl(composite);
		endDateControl.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));
		endDateControl.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				boolean isDateValid = (endDateControl.getDate() != null);
				setErrorMessage(isDateValid ? null : "Date must be in the format " + JMoneyPlugin.getDefault().getDateFormat());
			}
		});

		errorMessageText = new Text(composite, SWT.READ_ONLY);
		errorMessageText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));
		errorMessageText.setBackground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

		startDateControl.setDate(startDate);
		endDateControl.setDate(endDate);

		applyDialogFont(composite);
		return composite;
	}

	public Date getStartDate() {
		return startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	/**
	 * Sets or clears the error message.
	 * If not <code>null</code>, the OK button is disabled.
	 * 
	 * @param errorMessage
	 *            the error message, or <code>null</code> to clear
	 * @since 3.0
	 */
	public void setErrorMessage(String errorMessage) {
		errorMessageText.setText(errorMessage == null ? "" : errorMessage); //$NON-NLS-1$
		
		// If called during createDialogArea, the okButton
		// will not have been created yet.
		Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null) {
			okButton.setEnabled(errorMessage == null);
		}
		errorMessageText.getParent().update();
	}
}
