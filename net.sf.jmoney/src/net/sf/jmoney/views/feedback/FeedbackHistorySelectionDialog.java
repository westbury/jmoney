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

package net.sf.jmoney.views.feedback;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import net.sf.jmoney.JMoneyPlugin;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * Dialog that shows a list of items with icon and label.
 */
public class FeedbackHistorySelectionDialog extends SelectionDialog {
	
	private static final int REMOVE_ID= IDialogConstants.CLIENT_ID+1;
	private static final int WIDTH_IN_CHARACTERS= 55;
	
	private final int historyLimit = 10;

	private List<Feedback> fInput;
	private final List<Feedback> fRemovedEntries;
	
	private TableViewer fViewer;
	private Button fRemoveButton;
	
	private boolean fIsOpenInNewView;
	
	private FeedbackView fFeedbackView;

	private static final class FeedbackLabelProvider extends LabelProvider {
		
		private ArrayList<Image> fImages = new ArrayList<Image>();
		
		@Override
		public String getText(Object element) {
			return ((Feedback)element).getLabel();
		}
		
		@Override
		public Image getImage(Object element) {

			ImageDescriptor imageDescriptor= ((Feedback)element).getImageDescriptor(); 
			if (imageDescriptor == null)
				return null;
			
			Image image= imageDescriptor.createImage();
			fImages.add(image);

			return image;
		}
		
		@Override
		public void dispose() {
			for (Image image : fImages) {
				image.dispose();
			}	
			fImages= null;
		}
	}

	public FeedbackHistorySelectionDialog(Shell parent, List<Feedback> input, FeedbackView feedbackView) {
		super(parent);
		setTitle("Feedback on Previous Actions");  
		setMessage("&Select the action for which you would like to see the feedback:"); 
		fInput= input;
		fFeedbackView= feedbackView;
		fRemovedEntries= new ArrayList<Feedback>();
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
	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings dialogSettings= JMoneyPlugin.getDefault().getDialogSettings();
		IDialogSettings section= dialogSettings.getSection("DialogBounds_FeedbackHistorySelectionDialog");
		if (section == null) {
			section= dialogSettings.addNewSection("DialogBounds_FeedbackHistorySelectionDialog");
		}
		return section;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsStrategy()
	 */
	@Override
	protected int getDialogBoundsStrategy() {
		return DIALOG_PERSISTSIZE;
	}
	
	
	/*
	 * Overrides method from Dialog
	 */
	@Override
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
	@Override
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
	@Override
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
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				okPressed();
			}
		});
		fViewer.setLabelProvider(new FeedbackLabelProvider());
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= convertHeightInCharsToPixels(15);
		gd.widthHint= convertWidthInCharsToPixels(WIDTH_IN_CHARACTERS);
		table.setLayoutData(gd);
		
		
        fRemoveButton= new Button(parent, SWT.PUSH);
        fRemoveButton.setText("&Remove"); 
        fRemoveButton.addSelectionListener(new SelectionAdapter() {
            @Override
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
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validateDialogState();
			}
		});		
		
		Label fLink= new Label(parent, SWT.NONE);
		fLink.setText(MessageFormat.format("History limited to {0} result sets not shown in views.", new Integer(historyLimit)));
		fLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		
		applyDialogFont(ancestor);

		// set input & selections last, so all the widgets are created.
		fViewer.setInput(fInput);
		fViewer.getTable().setFocus();
		return ancestor;
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
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OPEN_LABEL, true);
		createButton(parent, IDialogConstants.OPEN_ID, "Open in &New", false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == REMOVE_ID) {
			IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
			for (Object curr : selection.toList()) {
				fRemovedEntries.add((Feedback)curr);
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
			super.buttonPressed(IDialogConstants.OK_ID);
		} else {
			super.buttonPressed(buttonId);
		}
	}
		
	/*
	 * Overrides method from Dialog
	 */
	@Override
	protected void okPressed() {
		// Build a list of selected children.
		ISelection selection= fViewer.getSelection();
		if (selection instanceof IStructuredSelection)
			setResult(((IStructuredSelection) fViewer.getSelection()).toList());
		
		// remove queries
		fFeedbackView.removeFeedback(fRemovedEntries);

		super.okPressed();
	}
}


