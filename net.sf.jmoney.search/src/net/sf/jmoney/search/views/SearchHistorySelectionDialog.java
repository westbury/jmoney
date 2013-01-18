package net.sf.jmoney.search.views;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import net.sf.jmoney.search.Activator;
import net.sf.jmoney.search.IEntrySearch;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * Dialog that shows a list of items with icon and label.
 */
public class SearchHistorySelectionDialog extends SelectionDialog {
	
	private static final int REMOVE_ID= IDialogConstants.CLIENT_ID+1;
	private static final int WIDTH_IN_CHARACTERS= 55;
	
	private List<IEntrySearch> fInput;
	private final List<IEntrySearch> fRemovedEntries;
	
	private TableViewer fViewer;
	private Button fRemoveButton;
	
	private boolean fIsOpenInNewView;
	private Link fLink;
	
	private SearchView fSearchView;

	private static class HistoryConfigurationDialog extends StatusDialog {
		
		private static final int DEFAULT_ID= 100;
		
		private int fHistorySize;
		private Text fHistorySizeTextField;
		private final List<IEntrySearch> fCurrentList;
		private final List<IEntrySearch> fCurrentRemoves;
		
		public HistoryConfigurationDialog(Shell parent, List<IEntrySearch> currentList, List<IEntrySearch> removedEntries) {
			super(parent);
			fCurrentList = currentList;
			fCurrentRemoves = removedEntries;
			setTitle("History Size");  
			fHistorySize= SearchPreferencePage.getHistoryLimit();
			setHelpAvailable(false);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
		 * @since 3.4
		 */
		protected boolean isResizable() {
			return true;
		}

		/*
		 * Overrides method from Dialog
		 */
		protected Control createDialogArea(Composite container) {
			Composite ancestor= (Composite) super.createDialogArea(container);
			GridLayout layout= (GridLayout) ancestor.getLayout();
			layout.numColumns= 2;
			ancestor.setLayout(layout);
						
			Label limitText= new Label(ancestor, SWT.NONE);
			limitText.setText("Configure the search &history size:");
			limitText.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
			
			fHistorySizeTextField= new Text(ancestor, SWT.BORDER | SWT.RIGHT);
			fHistorySizeTextField.setTextLimit(2);
			fHistorySizeTextField.setText(String.valueOf(fHistorySize));
			fHistorySizeTextField.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validateDialogState();
				}
			});
					
			GridData gridData= new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
			gridData.widthHint= convertWidthInCharsToPixels(6);
			fHistorySizeTextField.setLayoutData(gridData);
			fHistorySizeTextField.setSelection(0, fHistorySizeTextField.getText().length());
			applyDialogFont(ancestor);

			return ancestor;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.StatusDialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
		 */
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, DEFAULT_ID, "Restore &Default", false);
			super.createButtonsForButtonBar(parent);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
		 */
		protected void buttonPressed(int buttonId) {
			if (buttonId == DEFAULT_ID) {
				IPreferenceStore store = Activator.getDefault().getPreferenceStore();
				fHistorySizeTextField.setText(store.getDefaultString(SearchPreferencePage.LIMIT_HISTORY));
				validateDialogState();
			}
			super.buttonPressed(buttonId);
		}
		

		protected final boolean validateDialogState() {
			IStatus status= null;
			try {
				String historySize= fHistorySizeTextField.getText();
				int size= Integer.parseInt(historySize);
				if (size < 1 || size >= 100) {
					status= new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, "Internal error: history size must be in range 1 to 100", null);
				} else {
					fHistorySize= size;
				}
			} catch (NumberFormatException e) {
				status= new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, "Internal error: history size must be a numeric value", null);
			}
			if (status == null) {
				status= new Status(IStatus.OK, Activator.PLUGIN_ID, IStatus.OK, new String(), null);
			}
			updateStatus(status);
			return !status.matches(IStatus.ERROR);
		}
		
		@Override
		protected void okPressed() {
			IPreferenceStore store = Activator.getDefault().getPreferenceStore();
			store.setValue(SearchPreferencePage.LIMIT_HISTORY, fHistorySize);
			
			// establish history size
			for (int i= fCurrentList.size() - 1; i >= fHistorySize; i--) {
				fCurrentRemoves.add(fCurrentList.get(i));
				fCurrentList.remove(i);
			}
			super.okPressed();
		}
		
	}
	
	private static final class SearchesLabelProvider extends LabelProvider {
		
		private ArrayList<Image> fImages= new ArrayList<Image>();
		
		public String getText(Object element) {
			return ((IEntrySearch)element).getLabel();
		}
		
		public Image getImage(Object element) {

			ImageDescriptor imageDescriptor= ((IEntrySearch)element).getImageDescriptor(); 
			if (imageDescriptor == null)
				return null;
			
			Image image= imageDescriptor.createImage();
			fImages.add(image);

			return image;
		}
		
		public void dispose() {
			for (Image image : fImages) {
				image.dispose();
			}	
			fImages= null;
		}
	}

	public SearchHistorySelectionDialog(Shell parent, List<IEntrySearch> input, SearchView searchView) {
		super(parent);
		setTitle("Previous Searches");  
		setMessage("&Select the search to show in the search result view:"); 
		fInput= input;
		fSearchView= searchView;
		fRemovedEntries= new ArrayList<IEntrySearch>();
		setHelpAvailable(false);
	}

	/**
	 * @return the isOpenInNewView
	 */
	public boolean isOpenInNewView() {
		return fIsOpenInNewView;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsSettings()
	 */
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings dialogSettings= Activator.getDefault().getDialogSettings();
		IDialogSettings section= dialogSettings.getSection("DialogBounds_SearchHistorySelectionDialog");
		if (section == null) {
			section= dialogSettings.addNewSection("DialogBounds_SearchHistorySelectionDialog");
		}
		return section;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsStrategy()
	 */
	protected int getDialogBoundsStrategy() {
		return DIALOG_PERSISTSIZE;
	}
	
	
	/*
	 * Overrides method from Dialog
	 */
	protected Label createMessageArea(Composite composite) {
		Composite parent= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		parent.setLayout(layout);
		parent.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label label = new Label(parent,SWT.WRAP);
		label.setText(getMessage()); 
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		//gd.widthHint= convertWidthInCharsToPixels(WIDTH_IN_CHARACTERS);
		label.setLayoutData(gd);
		

		applyDialogFont(label);
		return label;
	}
	
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#create()
	 */
	public void create() {
		super.create();
		
		List<?> initialSelection = getInitialElementSelections();
		if (initialSelection != null)
			fViewer.setSelection(new StructuredSelection(initialSelection));

		validateDialogState();
	}
	
	/*
	 * Overrides method from Dialog
	 */
	protected Control createDialogArea(Composite container) {
		Composite ancestor= (Composite) super.createDialogArea(container);
		
		createMessageArea(ancestor);
		
		Composite parent= new Composite(ancestor, SWT.NONE);
		
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		parent.setLayout(layout);
		parent.setLayoutData(new GridData(GridData.FILL_BOTH));

		fViewer= new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		fViewer.setContentProvider(new ArrayContentProvider());
		
		final Table table= fViewer.getTable();
		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				okPressed();
			}
		});
		fViewer.setLabelProvider(new SearchesLabelProvider());
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= convertHeightInCharsToPixels(15);
		gd.widthHint= convertWidthInCharsToPixels(WIDTH_IN_CHARACTERS);
		table.setLayoutData(gd);
		
		
        fRemoveButton= new Button(parent, SWT.PUSH);
        fRemoveButton.setText("&Remove"); 
        fRemoveButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                buttonPressed(REMOVE_ID);
            }
        });
		GridData removeButtonLayoutData = new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false);
		fRemoveButton.setFont(JFaceResources.getDialogFont());
		GC gc = new GC(fRemoveButton);
		gc.setFont(fRemoveButton.getFont());
		FontMetrics fFontMetrics= gc.getFontMetrics();
		gc.dispose();
		int widthHint= Dialog.convertHorizontalDLUsToPixels(fFontMetrics, IDialogConstants.BUTTON_WIDTH);
		removeButtonLayoutData.widthHint = Math.max(widthHint, fRemoveButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		removeButtonLayoutData.horizontalAlignment = GridData.FILL;	 
		fRemoveButton.setLayoutData(removeButtonLayoutData);
		
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				validateDialogState();
			}
		});		
		
		fLink= new Link(parent, SWT.NONE);
		configureHistoryLink();
		fLink.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				HistoryConfigurationDialog dialog= new HistoryConfigurationDialog(getShell(), fInput, fRemovedEntries);
				if (dialog.open() == Window.OK) {
					fViewer.refresh();
					configureHistoryLink();
				}
			}
		});
		fLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		
		
		applyDialogFont(ancestor);

		// set input & selections last, so all the widgets are created.
		fViewer.setInput(fInput);
		fViewer.getTable().setFocus();
		return ancestor;
	}

	private void configureHistoryLink() {
		int historyLimit= SearchPreferencePage.getHistoryLimit();
		fLink.setText(MessageFormat.format("History limited to {0} result sets not shown in views.", new Integer(historyLimit)));
	}
	
	protected final void validateDialogState() {
		IStructuredSelection sel= (IStructuredSelection) fViewer.getSelection();
		int elementsSelected= sel.toList().size();
		
		fRemoveButton.setEnabled(elementsSelected > 0);
		Button okButton= getOkButton();
		if (okButton != null) {
			okButton.setEnabled(elementsSelected == 1);
		}
		Button openInNewButton= getButton(IDialogConstants.OPEN_ID);
		if (openInNewButton != null) {
			openInNewButton.setEnabled(elementsSelected == 1);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.SelectionDialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OPEN_LABEL, true);
		createButton(parent, IDialogConstants.OPEN_ID, "Open in &New", false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	
	protected void buttonPressed(int buttonId) {
		if (buttonId == REMOVE_ID) {
			IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
			for (Object curr : selection.toList()) {
				fRemovedEntries.add((IEntrySearch)curr);
				fInput.remove(curr);
				fViewer.remove(curr);
			}
			if (fViewer.getSelection().isEmpty() && !fInput.isEmpty()) {
				fViewer.setSelection(new StructuredSelection(fInput.get(0)));
			}
			return;
		}
		if (buttonId == IDialogConstants.OPEN_ID) {
			fIsOpenInNewView= true;
			buttonId= IDialogConstants.OK_ID;
		}
		super.buttonPressed(buttonId);
	}
		
	/*
	 * Overrides method from Dialog
	 */
	protected void okPressed() {
		// Build a list of selected children.
		ISelection selection= fViewer.getSelection();
		if (selection instanceof IStructuredSelection)
			setResult(((IStructuredSelection) fViewer.getSelection()).toList());
		
		// remove queries
		fSearchView.removeSearches(fRemovedEntries);

		super.okPressed();
	}
}


