/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2009 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.importer.wizards;

import net.sf.jmoney.importer.resources.Messages;

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
 * Wizard page that asks for the location of the file of comma-separated values.
 */
public class CsvImportWizardPage extends WizardPage  {

	private static final String NAME = "csvImportWizardPage"; // //$NON-NLS-1$

	private IWorkbenchWindow window;
	
	private Text filePathText;
	
	/**
	 * Create an instance of this class
	 */
	protected CsvImportWizardPage(IWorkbenchWindow window) {
		super(NAME);
		this.window = window;
		setTitle("Choose File");
		setDescription("Select the CSV file to import");
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

		filePathText = new Text(composite,SWT.BORDER | SWT.READ_ONLY);
		filePathText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button fileBrowseButton = new Button(composite,SWT.NONE);
		fileBrowseButton.setText("Browse");
		fileBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				browsePressed();
			}
		});
		
		Label label = new Label(composite, SWT.WRAP);
		label.setText(
				"The selected CSV file will be imported.  As you have not selected an account into which the import is to be made, " +
				"an investment account called 'Ameritrade' must exist and the data will be imported into that account. " +
				"The file must have been downloaded from Ameritrade for this import to work.  To download from Ameritrade, go to Statements, History. " +
				"If entries have already been imported, this import will not create duplicates.");
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		gd.widthHint = 600;
		label.setLayoutData(gd);
		
		setDescription("Import data from a CSV file that has been downloaded from Ameritrade.");
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
		FileDialog fileChooser = new FileDialog(window.getShell(), SWT.OPEN);
		fileChooser.setText(Messages.ImportDialog_Title);
		fileChooser.setFilterExtensions(new String[] { "*.csv" });
		fileChooser.setFilterNames(new String[] { "Comma Separated Values (*.csv)" });
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
