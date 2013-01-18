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

package net.sf.jmoney.pages.entries;

import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.resources.Messages;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * This dialog box class prompts the user for a foreign currency amount when
 * none has been entered.
 * <P>
 * It is called only in the following situation:
 * <UL>
 * <LI>The transaction is a simple transaction</LI>
 * <LI>both the capital account and the income and expense account have
 * different currencies (or commodities)</LI>
 * <LI>the amount for the income and expense account was zero</LI>
 * </UL>
 * 
 * You may wonder why this dialog box is used only in such a narrow situation.
 * The reason is that in all other situations the user can enter the foreign
 * currency amount in the normal way in the credit or debit columns. However, in
 * a simple transaction the two entries are combined and displayed on one row.
 * This necessitates the creation on another column to hold the amount of the
 * income or expense. However, that column is so rarely used that it is not
 * displayed by default. As the column is not displayed by default, we cannot
 * require the user to enter data in it.
 * 
 * @author Nigel Westbury
 */
public class ForeignCurrencyDialog {

	private Shell shell;

	private Display display;

	@SuppressWarnings("unchecked")
	public ForeignCurrencyDialog(Shell parent, EntryData item) {

		this.display = parent.getDisplay();

		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL
				| SWT.OK | SWT.CANCEL);
		shell.setText(Messages.ForeignCurrencyDialog_Text);

		GridLayout sectionLayout = new GridLayout();
		sectionLayout.numColumns = 1;
		sectionLayout.marginHeight = 0;
		sectionLayout.marginWidth = 0;
		shell.setLayout(sectionLayout);

		// Create the transaction property area
		Composite transactionArea = new Composite(shell, 0);
		GridLayout transactionLayout = new GridLayout();
		transactionLayout.numColumns = 2;
		transactionLayout.marginHeight = 0;
		transactionLayout.marginWidth = 0;
		transactionArea.setLayout(transactionLayout);

		Label label1 = new Label(transactionArea, SWT.NONE);
		Entry entry1 = item.getEntry();
		Entry entry2 = item.getOtherEntry();
		Object[] messageArgs = new Object[] {
				entry1.getAmount() > 0 ? Messages.ForeignCurrencyDialog_Credit : Messages.ForeignCurrencyDialog_Debit,
				entry1.getCommodityInternal().format(Math.abs(entry1.getAmount())),
				entry1.getAccount().getName(), entry2.getAccount().getName() };

		label1
				.setText(NLS
						.bind(
								Messages.ForeignCurrencyDialog_Information,
								messageArgs));
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label1.setLayoutData(gd);

		Label label2 = new Label(transactionArea, SWT.NONE);
		Object[] messageArgs2 = new Object[] { entry2.getCommodityInternal().getName() };
		label2.setText(NLS.bind(Messages.ForeignCurrencyDialog_Question, messageArgs2));

		final IPropertyControl propertyControl = EntryInfo.getAmountAccessor()
				.createPropertyControl(transactionArea);
		propertyControl.load(item.getOtherEntry());

		// Create the button area
		Composite buttonArea = new Composite(shell, 0);

		RowLayout layoutOfButtons = new RowLayout();
		layoutOfButtons.fill = false;
		layoutOfButtons.justify = true;
		buttonArea.setLayout(layoutOfButtons);

		// Create the 'add entry' button.
		Button addButton = new Button(buttonArea, SWT.PUSH);
		addButton.setText(Messages.ForeignCurrencyDialog_Ok);
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				propertyControl.save();
				shell.close();
			}
		});

		// Create the 'close' button
		Button closeButton = new Button(buttonArea, SWT.PUSH);
		closeButton.setText(Messages.ForeignCurrencyDialog_Close);
		closeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				propertyControl.save();
				shell.close();
			}
		});

		shell.pack();
	}

	public void open() {
		shell.pack();
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}
}
