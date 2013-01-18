package net.sf.jmoney.qif.wizards;

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
 * Page 1 of the QIF export Wizard
 */
public class QifExportWizardPage extends WizardPage  {

	private static final String QIF_EXPORT_WIZARD_PAGE = "qifExportWizardPage"; // //$NON-NLS-1$

	private IWorkbenchWindow window;
	
	private Text filePathText;
	
	/**
	 * Create an instance of this class
	 */
	protected QifExportWizardPage(IWorkbenchWindow window) {
		super(QIF_EXPORT_WIZARD_PAGE);
		this.window = window;
		setTitle("Choose File");
		setDescription("Choose the QIF file to export to");
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
				"Data will be exported to the selected file.");
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		gd.widthHint = 600;
		label.setLayoutData(gd);
		
		setDescription("Export data to a QIF file.");
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
		fileChooser.setText(QIFPlugin.getResourceString("MainFrame.export"));
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
