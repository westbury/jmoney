/**
 * 
 */
package net.sf.jmoney.gnucashXML.wizards;

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
 * Page 1 of the Gnucash import Wizard
 */
public class GnucashImportWizardPage extends WizardPage
{
	private static final String GNUCASH_IMPORT_WIZARD_PAGE = "gnucashImportWizardPage"; // //$NON-NLS-1$

	private IWorkbenchWindow window;

	private Text filePathText;

	/**
	 * Create an instance of this class
	 */
	public GnucashImportWizardPage(IWorkbenchWindow window)
	{
		super(GNUCASH_IMPORT_WIZARD_PAGE);
		this.window = window;
		setTitle("Choose File");
		setDescription("Choose the Gnucash file to import");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent)
	{
		initializeDialogUnits(parent);
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		GridLayout gdContainer = new GridLayout(2, false);
		gdContainer.horizontalSpacing = 10;
		gdContainer.verticalSpacing = 25;
		gdContainer.marginWidth = 20;
		gdContainer.marginHeight = 20;
		composite.setLayout(gdContainer);

		filePathText = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
		filePathText
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Button fileBrowseButton = new Button(composite, SWT.NONE);
		fileBrowseButton.setText("Browse");
		fileBrowseButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent evt)
			{
				browsePressed();
			}
		});

		Label label = new Label(composite, SWT.WRAP);
		label
				.setText("The selected Gnucash file will be imported.");
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		gd.widthHint = 600;
		label.setLayoutData(gd);

		setDescription("Import data from a Gnucash file on your local file system.");
		setPageComplete(false);
		filePathText.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
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

	protected void browsePressed()
	{
		FileDialog fileChooser = new FileDialog(window.getShell(), SWT.OPEN);
		fileChooser.setText(GnucashXMLPlugin
				.getResourceString("MainFrame.import"));
		fileChooser.setFilterExtensions(new String[] { "*.*" });
		fileChooser
				.setFilterNames(new String[] { "All Files (*.*)" });
		String filename = fileChooser.open();
		if(filename != null)
		{
			filePathText.setText(filename);
			setPageComplete(true);
		}
	}

	/**
	 * Hook method for restoring widget values to the values that they held last
	 * time this wizard was used to completion.
	 */
	protected void restoreWidgetValues()
	{
		IDialogSettings settings = getDialogSettings();
		if(settings != null)
		{
			String directory = settings.get("directory");
			if(directory != null)
			{
				filePathText.setText(directory);
			}
		}
	}

	public String getFileName()
	{
		return filePathText.getText();
	}
}
