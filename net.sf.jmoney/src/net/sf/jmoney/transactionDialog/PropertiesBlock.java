/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2007 Nigel Westbury <westbury@users.sf.net>
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

package net.sf.jmoney.transactionDialog;

import java.util.ArrayList;
import java.util.List;

import net.sf.jmoney.entrytable.CellBlock;
import net.sf.jmoney.entrytable.CellFocusListener;
import net.sf.jmoney.entrytable.ICellControl2;
import net.sf.jmoney.entrytable.RowControl;
import net.sf.jmoney.entrytable.SplitEntryRowControl;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.SessionChangeAdapter;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.resources.Messages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/**
 * This class represents a block that contains all properties (except a few standard ones that
 * have their own column).  All the properties appear in this cell, with labels and wrapped in
 * a horizontal row layout.
 * <P>
 * Use this when you want to show all properties to the user, with the set of properties adjusting
 * according to what is applicable to each entry.  This is not the most user friendly of formats,
 * but it does give the user the most control.
 */
class PropertiesBlock extends CellBlock<Entry, SplitEntryRowControl> {
	
	// TODO We should not really have this field.  We should use listeners or something, not sure what.
	TransactionDialog transactionDialog;
	
	public PropertiesBlock(TransactionDialog transactionDialog) {
		super(400, 20);
		this.transactionDialog = transactionDialog;
	}
	
	// TODO: remove entry parameter from this method.
	@Override
	public void createHeaderControls(Composite parent, Entry entry) {
		Label label = new Label(parent, SWT.NULL);
		label.setText(Messages.PropertiesBlock_Properties);
		label.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
	}

	@Override
	public IPropertyControl<Entry> createCellControl(Composite parent,
			RowControl rowControl, SplitEntryRowControl coordinator) {
    	
    	return new PropertiesCellControl(parent, rowControl);
	}

	private class PropertiesCellControl implements IPropertyControl<Entry> {
		private Composite propertiesControl;
		private Entry entry = null;
		
		final private Color labelColor;
		final private Color controlColor;
		private List<IPropertyControl> properties;
		private RowControl rowControl;

		public PropertiesCellControl(Composite parent, RowControl rowControl) {
	    	this.rowControl = rowControl;
	    	
			labelColor = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
			controlColor = Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW);
			
			propertiesControl = new Composite(parent, SWT.NONE);
			propertiesControl.setLayout(new RowLayout(SWT.HORIZONTAL));
		}

		@Override
		public Control getControl() {
			return propertiesControl;
		}

		@Override
		public void load(final Entry entry) {
			this.entry = entry;

			createPropertyControls();

			/*
			 * Note that this dialog is modal so changes cannot be made from outside the dialog.
			 * Refreshing of the view after inserting and removing splits is handled by the commands
			 * directly, so we are only interested in property changes. 
			 */
			entry.getDataManager().addChangeListener(new SessionChangeAdapter() {
				@Override
				public void objectChanged(IModelObject changedObject, IScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
					if (changedObject == entry) {
						if (changedProperty == EntryInfo.getAccountAccessor()) {
							createPropertyControls();
							propertiesControl.layout(true);
							Composite c = propertiesControl;
							do {
								c.layout(true);
								c = c.getParent();
							} while (c != null);
//			    	        shell.pack();
							transactionDialog.refreshScrolling();
							
							c = propertiesControl;
							do {
								c.layout(true);
								c = c.getParent();
							} while (c != null);
							
						}
					}
				}
			}, propertiesControl);

			
		}

		private void createPropertyControls() {
			for (Control control : propertiesControl.getChildren()) {
				control.dispose();
			}

			properties = new ArrayList<IPropertyControl>();

			if (entry.getAccount() != null) {
				for (ScalarPropertyAccessor accessor : EntryInfo.getPropertySet().getScalarProperties3()) {
					// Be sure not to include the three properties that have their own columns
					if (accessor != EntryInfo.getAccountAccessor()
							&& accessor != EntryInfo.getMemoAccessor()
							&& accessor != EntryInfo.getAmountAccessor()
							&& isEntryPropertyApplicable(accessor, entry.getAccount())) {
						createPropertyControl(propertiesControl, accessor);
					}
				}
			}
		}

		private boolean isEntryPropertyApplicable(
				ScalarPropertyAccessor<?,Entry> accessor, Account account) {

			boolean x = true;
			if (account instanceof CapitalAccount) {
				 x = !accessor.getName().equals("net.sf.jmoney.paypal.entryProperties.itemUrl");
			} else {
				 x = accessor.getName().equals("net.sf.jmoney.paypal.entryProperties.itemUrl")
				|| accessor.getName().equals("net.sf.jmoney.paypal.entryProperties.itemUrl")
				|| accessor.getName().equals("net.sf.jmoney.stocks.entryProperties.security");
			}

			return accessor.isPropertyApplicable(entry);
			
			// TODO Push this into the metadata.
//			if (account instanceof CapitalAccount) {
//				return !accessor.getName().equals("net.sf.jmoney.paypal.entryProperties.itemUrl");
//			} else {
//				return accessor.getName().equals("net.sf.jmoney.paypal.entryProperties.itemUrl")
//				|| accessor.getName().equals("net.sf.jmoney.paypal.entryProperties.itemUrl")
//				|| accessor.getName().equals("net.sf.jmoney.stocks.entryProperties.security");
//			}
		}

		@SuppressWarnings("unchecked")
		private void createPropertyControl(Composite parent, ScalarPropertyAccessor accessor) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(2, false));
			Label label = new Label(composite, SWT.LEFT);
			label.setText(accessor.getDisplayName() + ":"); //$NON-NLS-1$
			label.setForeground(labelColor);

			final IPropertyControl propertyControl = accessor.createPropertyControl(composite);
			propertyControl.getControl().setLayoutData(new GridData(accessor.getMinimumWidth(), SWT.DEFAULT));
			propertyControl.getControl().setBackground(controlColor);

			// TODO: This will not add listener to child controls - fix this.

			// TODO: This is a kludge.
			// We need this wrapper for just for two methods,
			// select and unselect.  Do we really need those?
			//
			FocusListener controlFocusListener = new CellFocusListener<RowControl>(rowControl, new ICellControl2<Entry>() {
				@Override
				public Control getControl() {
					return propertyControl.getControl();
				}

				@Override
				public void load(Entry data) {
					propertyControl.load(data);
				}

				@Override
				public void save() {
					propertyControl.save();
//					fireUserChange(coordinator);
				}

				@Override
				public void setSelected() {
					propertyControl.getControl().setBackground(RowControl.selectedCellColor);
				}

				@Override
				public void setUnselected() {
					propertyControl.getControl().setBackground(null);
				}
			});

			/*
			 * The control may in fact be a composite control, in which case the
			 * composite control itself will never get the focus. Only the child
			 * controls will get the focus, so we add the listener recursively
			 * to all child controls.
			 */
			addFocusListenerRecursively(propertyControl.getControl(), controlFocusListener);

			propertyControl.load(entry);
			properties.add(propertyControl);
		}

		private void addFocusListenerRecursively(Control control, FocusListener listener) {
			control.addFocusListener(listener);
			if (control instanceof Composite) {
				for (Control child: ((Composite)control).getChildren()) {
					addFocusListenerRecursively(child, listener);
				}
			}
		}

		@Override
		public void save() {
			for (IPropertyControl control : properties) {
				control.save();
			}
		}

		public void setFocusListener(FocusListener controlFocusListener) {
			// TODO: Should this method be removed???
		}

		public void setSelected() {
			propertiesControl.setBackground(RowControl.selectedCellColor);
		}

		public void setUnselected() {
			propertiesControl.setBackground(null);
		}
	}

}
