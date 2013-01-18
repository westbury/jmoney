package net.sf.jmoney.qif.wizards;

import java.text.MessageFormat;

import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.qif.QIFPlugin;

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
 * Page 1 of the QIF import Wizard
 */
public class QifImportWizardPage extends WizardPage  {

	private static final String QIF_IMPORT_WIZARD_PAGE = "qifImportWizardPage"; // //$NON-NLS-1$

	private IWorkbenchWindow window;
	
	private CapitalAccount destinationAccount;
	
	private Text filePathText;
	
	/**
	 * Create an instance of this class
	 */
	protected QifImportWizardPage(IWorkbenchWindow window, CapitalAccount destinationAccount) {
		super(QIF_IMPORT_WIZARD_PAGE);
		this.window = window;
		this.destinationAccount = destinationAccount;
		setTitle("Choose File");
		setDescription("Choose the QIF file to import");
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
		if (destinationAccount == null) {
			label.setText(
					"The selected QIF file will be imported.  As you have not selected an account into which the import is to be made, " +
					"you must select a QIF file in the 'full' format.  This means the account details must be provided in the QIF file before the transactions. " +
					"Multiple accounts in a single QIF file are supported.  Accounts will be matched against existing accounts based on the name.  If an account " +
					"does not already exist then it will be created.  You can thus import QIF data from multiple files and still have transfers " +
					"and categories correctly matched (just don't rename any accounts until you have imported everything).");
		} else {
			label.setText(MessageFormat.format(
					"The selected QIF file will be imported into the {0} account.  The QIF file should be in the form that contains only entries, " +
					"i.e. no account list etc.  This is the form of the QIF files typically exported by online banking sites.",
					destinationAccount.getName()));
		}
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		gd.widthHint = 600;
		label.setLayoutData(gd);
		
		setDescription("Import data from a QIF file on your local file system.");
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
		fileChooser.setText(QIFPlugin.getResourceString("MainFrame.import"));
		fileChooser.setFilterExtensions(new String[] { "*.qif" });
		fileChooser.setFilterNames(new String[] { "Quicken Interchange Format (*.qif)" });
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
