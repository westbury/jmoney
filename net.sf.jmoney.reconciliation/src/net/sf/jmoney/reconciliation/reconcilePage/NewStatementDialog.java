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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jmoney.AlternativeContentLayout;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.DateControl;
import net.sf.jmoney.reconciliation.BankStatement;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * An input dialog for soliciting a statement number or statement date from the user.
 * 
 * @author Nigel Westbury
 */
class NewStatementDialog extends Dialog {
	/**
	 * The last statement to exist before this request to create a new statement.  This statement is
	 * used to determine a default value for the new statement and also to determine the method by which
	 * statements in this account are identified (by number or by date).
	 */
	private BankStatement lastStatement;

	/**
	 * The input value; the empty string by default.
	 */
	private BankStatement value = null;

	/**
	 * Ok button widget.
	 */
	private Button okButton;

	/**
	 * Error message label widget.
	 */
	private Text errorMessageText;
	
	private int statementIdType;
	private static final int NOT_YET_DETERMINED = 0;
	private static final int BY_NUMBER = 1;
	private static final int BY_DATE = 2;
	private Composite byDateComposite;
	private Composite byNumberComposite;

	private AlternativeContentLayout alternativeContentLayout;

	/**
	 * Input widget for statement number.
	 */
	Text text;

	/**
	 * Input widget for statement date.
	 */
	DateControl dateControl;
	
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
	public NewStatementDialog(Shell parentShell, BankStatement lastStatement) {
		super(parentShell);
		this.lastStatement = lastStatement;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			switch (statementIdType) {
			case BY_NUMBER:
			value = new BankStatement(text.getText());
			break;
			case BY_DATE:
				Date date = dateControl.getDate();
				value = new BankStatement(date);
				break;
			default:
				throw new RuntimeException("bad case");
			}
		} else {
			value = null;
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
		okButton = createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		if (lastStatement == null) {
			statementIdType = NOT_YET_DETERMINED;
			
			Label label = new Label(composite, SWT.WRAP);
			label.setText("You are creating the first statement for this account.  "
					+ "Some banks number their statements (common in United Kingdom) while some bank do not (common in United States).  "
					+ "If the statements are not numbered then you must identify each statement by date.");
			
			GridData messageData = new GridData();
			Rectangle rect = getShell().getMonitor().getClientArea();
			messageData.widthHint = rect.width/2;
			label.setLayoutData(messageData);
			
			Button byNumberButton = new Button(composite, SWT.RADIO);
			byNumberButton.setText("Identify Bank Statements by Sequence Number");
			byNumberButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					promptForNumber();
				}
			});
			
			Button byDateButton = new Button(composite, SWT.RADIO);
			byDateButton.setText("Identify Bank Statements by Date");
			byDateButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					promptForDate();
				}
			});
		}
		
		
		Composite alternativeContentContainer = new Composite(composite, 0);
		alternativeContentLayout = new AlternativeContentLayout();
		alternativeContentContainer.setLayout(alternativeContentLayout);
		
		// create composite for getting statement by number
		byNumberComposite = new Composite(alternativeContentContainer, 0);
		byNumberComposite.setLayout(new GridLayout());
		
			Label numberLabel = new Label(byNumberComposite, SWT.WRAP);
			numberLabel.setText("Number of the new statement:");
			GridData numberLabelData = new GridData(GridData.GRAB_HORIZONTAL
					| GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_CENTER);
			numberLabelData.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
			numberLabel.setLayoutData(numberLabelData);
			numberLabel.setFont(parent.getFont());
			
			text = new Text(byNumberComposite, SWT.SINGLE | SWT.BORDER);
			text.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
					| GridData.HORIZONTAL_ALIGN_FILL));
			text.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					Pattern numberPattern = Pattern.compile("^(\\d){1,4}$");
					Matcher m = numberPattern.matcher(text.getText());
					boolean b = m.matches();
					setErrorMessage(b ? null : "Statement number must be a number in the range 1 to 9999");
				}
			});
		
		// create composite for getting statement by number
		byDateComposite = new Composite(alternativeContentContainer, 0);
		byDateComposite.setLayout(new GridLayout());
		
			Label dateLabel = new Label(byDateComposite, SWT.WRAP);
			dateLabel.setText("Date of the new statement:");
			GridData dateLabelData = new GridData(GridData.GRAB_HORIZONTAL
					| GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_CENTER);
			dateLabelData.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
			dateLabel.setLayoutData(dateLabelData);
			dateLabel.setFont(parent.getFont());
			
			dateControl = new DateControl(byDateComposite/*, SWT.SINGLE | SWT.BORDER*/);
			dateControl.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
					| GridData.HORIZONTAL_ALIGN_FILL));
			dateControl.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					boolean isDateValid = (dateControl.getDate() != null);
					setErrorMessage(isDateValid ? null : "Statement date must be in the format " + JMoneyPlugin.getDefault().getDateFormat());
				}
			});
		
			errorMessageText = new Text(composite, SWT.READ_ONLY);
			errorMessageText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
					| GridData.HORIZONTAL_ALIGN_FILL));
			errorMessageText.setBackground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			
		if (lastStatement != null) {
			if (lastStatement.isNumber()) {
				statementIdType = BY_NUMBER;
				text.setText(Integer.toString(lastStatement.getNumber() + 1));
			} else {
				statementIdType = BY_DATE;
				Date initialDate = lastStatement.getStatementDate();
				
       	        Calendar calendar = Calendar.getInstance();
    	        calendar.setTime(initialDate);
    	        calendar.add(Calendar.MONTH, 1);
    	       dateControl.setDate(calendar.getTime());
			}
		}
		
		switch (statementIdType) {
		case BY_NUMBER:
			alternativeContentLayout.show(byNumberComposite);
			text.setFocus();
			text.selectAll();
			break;
		case BY_DATE:
			alternativeContentLayout.show(byDateComposite);
			dateControl.setFocus();
			break;
		}
		
		applyDialogFont(composite);
		return composite;
	}

	/**
	 * 
	 */
	protected void promptForNumber() {
		statementIdType = BY_NUMBER;
		alternativeContentLayout.show(byNumberComposite);
	}

	/**
	 * 
	 */
	protected void promptForDate() {
		statementIdType = BY_DATE;
		alternativeContentLayout.show(byDateComposite);
	}

	/**
	 * Returns the string typed into this input dialog.
	 * 
	 * @return the input string
	 */
	public BankStatement getValue() {
		return value;
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
		if (okButton != null) {
			okButton.setEnabled(errorMessage == null);
		}
		errorMessageText.getParent().update();
	}
}
