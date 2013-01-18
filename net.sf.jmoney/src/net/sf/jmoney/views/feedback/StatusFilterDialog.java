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

import net.sf.jmoney.resources.Messages;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;



public class StatusFilterDialog extends Dialog {

	private SeverityGroup severityGroup;

	private class SeverityGroup {
		private Button enablementButton;
		private Button errorButton;
		private Button warningButton;
		private Button infoButton;
		public SeverityGroup(Composite parent) {
			SelectionListener listener = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateEnablement();
				}
			};
			
			enablementButton = new Button(parent, SWT.CHECK);
			GridData data = new GridData(GridData.FILL_HORIZONTAL);
			data.horizontalSpan = 2;
			enablementButton.setLayoutData(data);
			enablementButton.setText(Messages.DialogProblemFilter_severityLabel);
			enablementButton.addSelectionListener(listener);
			
			errorButton = new Button(parent, SWT.CHECK);
			errorButton.setText(Messages.DialogProblemFilter_severityError);
			errorButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			errorButton.addSelectionListener(selectionListener);
			
			warningButton = new Button(parent, SWT.CHECK);
			warningButton.setText(Messages.DialogProblemFilter_severityWarning);
			warningButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			warningButton.addSelectionListener(selectionListener);
			
			infoButton = new Button(parent, SWT.CHECK);
			infoButton.setText(Messages.DialogProblemFilter_sererityInfo);
			infoButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			infoButton.addSelectionListener(selectionListener);
		}
		
		public boolean isEnabled() {
			return enablementButton.getSelection();
		}
		public void setEnabled(boolean enabled) {
			enablementButton.setSelection(enabled);
		}
		private boolean isErrorSelected() {
			return errorButton.getSelection();
		}
		private void setErrorSelected(boolean selected) {
			errorButton.setSelection(selected);
		}
		private boolean isWarningSelected() {
			return warningButton.getSelection();
		}
		private void setWarningSelected(boolean selected) {
			warningButton.setSelection(selected);
		}
		private boolean isInfoSelected() {
			return infoButton.getSelection();
		}
		private void setInfoSelected(boolean selected) {
			infoButton.setSelection(selected);
		}
		
		private void updateEnablement() {
			enablementButton.setEnabled(isFilterEnabled());
			errorButton.setEnabled(enablementButton.isEnabled() && enablementButton.getSelection());
			warningButton.setEnabled(enablementButton.isEnabled() && enablementButton.getSelection());
			infoButton.setEnabled(enablementButton.isEnabled() && enablementButton.getSelection());
		}
	}
	
	private StatusFilter filter;
	
	private Button filterEnabledButton;
	
	protected SelectionListener selectionListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			updateEnabledState();
		}
	};

	/**
	 * @param parentShell
	 * @param filter
	 */
	public StatusFilterDialog(Shell parentShell, StatusFilter filter) {
		super(parentShell);
		this.filter = filter;
	}
	
	/**
	 * Method declared on Dialog.
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		createOnOffArea(composite);
		createCategoriesArea(composite);
		createAttributesArea(composite); 
		createSeparatorLine(composite);	
	
		updateUIFromFilter();
		
		return composite;
	}

	/**
	 * Creates a separator line above the OK/Cancel buttons bar
	 * 
	 * @param parent the parent composite
	 */
	protected void createSeparatorLine(Composite parent) {
		// Build the separator line
		Label separator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		separator.setLayoutData(gd);
	}

	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == Dialog.OK) {
			updateFilterFromUI();
		}
		super.buttonPressed(buttonId);
	}

	/**
	 * Method declared on Window.
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.DialogProblemFilter_title);
	}

	/**
	 * Creates the filter enablement area.
	 * 
	 * @param parent the parent composite
	 */
	protected void createOnOffArea(Composite parent) {
		Font font = parent.getFont();
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		composite.setFont(font);
		composite.setLayout(new GridLayout());
		filterEnabledButton = createCheckbox(composite,
			Messages.DialogProblemFilter_onOff,
			false);
		filterEnabledButton.setFont(composite.getFont());
		filterEnabledButton.setLayoutData(new GridData());
		filterEnabledButton.addSelectionListener(selectionListener);
	}

	/**
	 * Creates the area showing which categories of problems should be included.
	 *
	 * @param parent the parent composite
	 */
	protected void createCategoriesArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
	
	}

	/**
	 * Creates a check box button with the given parent and text.
	 *
	 * @param parent the parent composite
	 * @param text the text for the check box
	 * @param grabRow <code>true</code>to grab the remaining horizontal space, <code>false</code> otherwise
	 * @return the check box button
	 */
	protected Button createCheckbox(Composite parent, String text, boolean grabRow) {
		Button button = new Button(parent, SWT.CHECK);
		if (grabRow) {
			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			button.setLayoutData(gridData);
		}
		button.setText(text);
		button.addSelectionListener(selectionListener);
		button.setFont(parent.getFont());
		return button;
	}

	/**
	 * Creates a combo box with the given parent, items, and selection
	 *
	 * @param parent the parent composite
	 * @param items the items for the combo box
	 * @param selectionIndex the index of the item to select
	 * @return the combo box
	 */
	protected Combo createCombo(Composite parent, String[] items, int selectionIndex) {
		Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		combo.setFont(parent.getFont());
		combo.setItems(items);
		combo.select(selectionIndex);
		combo.addSelectionListener(selectionListener);
		return combo;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.markerview.FiltersDialog#createAttributesArea(org.eclipse.swt.widgets.Composite)
	 */
	protected void createAttributesArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout layout = new GridLayout(5, false);
		layout.verticalSpacing = 7;
		composite.setLayout(layout);
		
		severityGroup = new SeverityGroup(composite);
	}

	/**
	 * Update the filter with data taken from the UI controls.
	 */
	protected void updateFilterFromUI() {
		filter.setSelectBySeverity(severityGroup.isEnabled());
		int severityMask = 0;
		if (severityGroup.isErrorSelected()) {
			severityMask = severityMask | IStatus.ERROR;
		}
		if (severityGroup.isWarningSelected()) {
			severityMask = severityMask | IStatus.WARNING;
		}
		if (severityGroup.isInfoSelected()) {
			severityMask = severityMask | IStatus.INFO;
		}
		filter.setSeverity(severityMask);
		
		filter.setEnabled(filterEnabledButton.getSelection());
	}

	/**
	 * Update the UI controls with data taken from the filter.
	 */
	protected void updateUIFromFilter() {
		filterEnabledButton.setSelection(filter.isEnabled());
		
		severityGroup.setEnabled(filter.getSelectBySeverity());
		int severity = filter.getSeverity();
		severityGroup.setErrorSelected((severity & IStatus.ERROR) > 0);
		severityGroup.setWarningSelected((severity & IStatus.WARNING) > 0);
		severityGroup.setInfoSelected((severity & IStatus.INFO) > 0);
		
		severityGroup.updateEnablement();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.markerview.FiltersDialog#updateEnabledState()
	 */
	protected void updateEnabledState() {
		severityGroup.updateEnablement();
	}

	/**
	 * @return <code>true</code> if the filter's enablement button is checked 
	 * otherwise <code>false</code>.
	 */
	protected boolean isFilterEnabled() {
		return (filterEnabledButton == null) || filterEnabledButton.getSelection();
	}
}
