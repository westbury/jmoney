/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2010 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.gnucashXML.actions;

import net.sf.jmoney.gnucashXML.GnucashXMLPlugin;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Page 1 of the GNU Cash export Wizard
 */
public class GnuCashExportWizardPage extends WizardPage  {

	private static final String GNU_CASH_EXPORT_WIZARD_PAGE = "gnuCashExportWizardPage"; // //$NON-NLS-1$

	private IWorkbenchWindow window;
	
	private Text filePathText;
	
	/**
	 * Create an instance of this class
	 */
	protected GnuCashExportWizardPage(IWorkbenchWindow window) {
		super(GNU_CASH_EXPORT_WIZARD_PAGE);
		this.window = window;
		setTitle("GNU Cash Export");
		setDescription("Choose where you want to export the GnuCash xml file.");
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

		new Label(composite, SWT.WRAP).setText("Export file:");
		
		Composite fileComposite = new Composite(composite, SWT.NONE);
		fileComposite.setLayout(new GridLayout(2, false));


		filePathText = new Text(fileComposite, SWT.BORDER);
		filePathText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button fileBrowseButton = new Button(fileComposite, SWT.NONE);
		fileBrowseButton.setText("Browse");
		fileBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				browsePressed();
			}
		});
		
		fileComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		setPageComplete(false);
		filePathText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				// TODO Validate text
				
				setPageComplete(true);
			}
		});
		
		restoreWidgetValues();
		
		// can not finish initially, but don't want to start with an error
		// message either
		setPageComplete(false);

		setControl(composite);

		filePathText.setFocus();
		
		Dialog.applyDialogFont(composite);
	}

	protected void browsePressed() {
		FileDialog fileChooser = new FileDialog(window.getShell(), SWT.SAVE);

		fileChooser.setText(
				GnucashXMLPlugin.getResourceString("MainFrame.export"));
		fileChooser.setFilterExtensions(new String[] { "*.xml;*.xac" });
		fileChooser.setFilterNames(new String[] { "XML Gnucash Files (*.xml; *.xac)" });
		// TODO: Faucheux - delete Directory 
//		fileChooser.setFilterPath("D:\\Documents and Settings\\Administrateur\\Mes documents\\Mes comptes");
		String filename = fileChooser.open();
		if (filename != null) {
			filePathText.setText(filename);
			setPageComplete(true);
		}
	}
	
	/**
	 * Hook method for restoring widget values to the values that they held last
	 * time this wizard was used to completion.
	 */
	protected void restoreWidgetValues() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			String directory = settings.get("directory");
			if (directory != null) {
				filePathText.setText(directory);
			}
		}
	}

	public String getFileName() {
		return filePathText.getText();
	}
}
