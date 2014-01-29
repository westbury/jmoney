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

import net.sf.jmoney.entrytable.CellBlock;
import net.sf.jmoney.entrytable.CellFocusListener;
import net.sf.jmoney.entrytable.ICellControl2;
import net.sf.jmoney.entrytable.RowControl;
import net.sf.jmoney.entrytable.SplitEntryRowControl;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.internal.databinding.provisional.swt.ControlCreator;
import org.eclipse.jface.internal.databinding.provisional.swt.UpdatingComposite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
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
	public IPropertyControl<Entry> createCellControl(Composite parent, IObservableValue<? extends Entry> master,
			RowControl rowControl, SplitEntryRowControl coordinator) {

		// HACK: Bit of a hack this, but master never changes so we can do this.
//		Entry entry = master.getValue();
		
    	return new PropertiesCellControl(parent, rowControl, master);
	}

	private class PropertiesCellControl implements IPropertyControl<Entry> {
		private final class PropertyControlCreator extends ControlCreator {
			private final ScalarPropertyAccessor<?, ? super Entry> accessor;
			private final IObservableValue<? extends Entry> entryObservable;

			private PropertyControlCreator(UpdatingComposite parent,
					ScalarPropertyAccessor<?, ? super Entry> accessor,
					IObservableValue<? extends Entry> entryObservable) {
				super(parent);
				this.accessor = accessor;
				this.entryObservable = entryObservable;
			}

			@Override
			protected Control createControl() {
				Composite composite = new Composite(parent, SWT.NONE);
				composite.setLayout(new GridLayout(2, false));
				Label label = new Label(composite, SWT.LEFT);
				label.setText(accessor.getDisplayName() + ":"); //$NON-NLS-1$
				label.setForeground(labelColor);

				final Control propertyControl = accessor.createPropertyControl2(composite, entryObservable);
				propertyControl.setLayoutData(new GridData(accessor.getMinimumWidth(), SWT.DEFAULT));
				propertyControl.setBackground(controlColor);

				// TODO: This will not add listener to child controls - fix this.

				// TODO: This is a kludge.
				// We need this wrapper for just for two methods,
				// select and unselect.  Do we really need those?
				//
				FocusListener controlFocusListener = new CellFocusListener<RowControl>(rowControl, new ICellControl2<Entry>() {
					@Override
					public Control getControl() {
						return propertyControl;
					}

					@Override
					public void load(Entry data) {
//						propertyControl.load(data);
					}

					@Override
					public void save() {
//						propertyControl.save();
					}

					@Override
					public void setSelected() {
						propertyControl.setBackground(RowControl.selectedCellColor);
					}

					@Override
					public void setUnselected() {
						propertyControl.setBackground(null);
					}
				});

				/*
				 * The control may in fact be a composite control, in which case the
				 * composite control itself will never get the focus. Only the child
				 * controls will get the focus, so we add the listener recursively
				 * to all child controls.
				 */
				addFocusListenerRecursively(propertyControl, controlFocusListener);
				
				return composite;
			}

			@Override
			public boolean equals(Object other) {
				return accessor == ((PropertyControlCreator)other).accessor;
			}

			@Override
			public int hashCode() {
				return accessor.hashCode();
			}
		}

		private UpdatingComposite propertiesControl;
//		private Entry entry = null;

		final private Color labelColor;
		final private Color controlColor;
//		private List<IPropertyControl> properties;
		private RowControl rowControl;

		public PropertiesCellControl(Composite parent, RowControl rowControl, final IObservableValue<? extends Entry> entryObservable) {
	    	this.rowControl = rowControl;

			labelColor = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
			controlColor = Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW);

//			final Map<ScalarPropertyAccessor<?, ? super Entry>, ControlCreator> controlCreators = new HashMap<ScalarPropertyAccessor<?, ? super Entry>, ControlCreator>();
				
			propertiesControl = new UpdatingComposite(parent, SWT.NONE) {
				@Override
				protected void createControls() {
					/*
					 * This is a tracked getter, so be sure to get the account
					 * through an observable.
					 */
					Account account = EntryInfo.getAccountAccessor().observe(entryObservable.getValue()).getValue();
					
					if (account != null) {
						for (final ScalarPropertyAccessor<?, ? super Entry> accessor : EntryInfo.getPropertySet().getScalarProperties3()) {
							// Be sure not to include the three properties that have their own columns
							if (accessor != EntryInfo.getAccountAccessor()
									&& accessor != EntryInfo.getMemoAccessor()
									&& accessor != EntryInfo.getAmountAccessor()
									&& isEntryPropertyApplicable(accessor, entryObservable.getValue(), account)) {
//								ControlCreator creator = controlCreators.get(accessor);
//								if (creator == null) {
									ControlCreator creator = new PropertyControlCreator(this, accessor,
											entryObservable);
//									controlCreators.put(accessor, creator);
//								}
								creator.create();
							}
						}
					}
				}
			};
			propertiesControl.setLayout(new RowLayout(SWT.HORIZONTAL));

			// This is a bit of a hack because we don't have any layouts that track
			// changes.
			IObservableValue<Point> sizeObservable = new ComputedValue<Point>() {
				@Override
				protected Point calculate() {
					return propertiesControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				}
			};
			sizeObservable.addChangeListener(new IChangeListener() {

				@Override
				public void handleChange(ChangeEvent event) {
					Composite c = propertiesControl;
					do {
						c.layout(true);
						c = c.getParent();
					} while (c != null);
//	    	        shell.pack();
					transactionDialog.refreshScrolling();

					c = propertiesControl;
					do {
						c.layout(true);
						c = c.getParent();
					} while (c != null);
				}
			});
			
			/*
			 * Note that this dialog is modal so changes cannot be made from outside the dialog.
			 * Refreshing of the view after inserting and removing splits is handled by the commands
			 * directly, so we are only interested in property changes.
			 */
//			entry.getDataManager().addChangeListener(new SessionChangeAdapter() {
//				@Override
//				public void objectChanged(IModelObject changedObject, IScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
//					if (changedObject == entry) {
//						if (changedProperty == EntryInfo.getAccountAccessor()) {
//							createPropertyControls();
//							propertiesControl.layout(true);
//							Composite c = propertiesControl;
//							do {
//								c.layout(true);
//								c = c.getParent(comp);
//							} while (c != null);
////			    	        shell.pack();
//							transactionDialog.refreshScrolling();
//
//							c = propertiesControl;
//							do {
//								c.layout(true);
//								c = c.getParent();
//							} while (c != null);
//
//						}
//					}
//				}
//			}, propertiesControl);
			
			/*
			 * Update if the account changes.
			 * Is this entirely correct?  Do we stipulate that which properties are to be shown depends only on
			 * the account?  I suspect not.  We should show only applicable properties which depends on many things.
			 */
			IObservableValue<Account> accountObservable = EntryInfo.getAccountAccessor().observeDetail(entryObservable);

			// Don't need this anymore because all in UpdatableComposite
//			createPropertyControls(accountObservable.getValue());
//
//			accountObservable.addValueChangeListener(new IValueChangeListener<Account>() {
//				@Override
//				public void handleValueChange(ValueChangeEvent<Account> event) {
//					createPropertyControls(event.diff.getNewValue());
//					propertiesControl.layout(true);
//					Composite c = propertiesControl;
//					do {
//						c.layout(true);
//						c = c.getParent();
//					} while (c != null);
////	    	        shell.pack();
//					transactionDialog.refreshScrolling();
//
//					c = propertiesControl;
//					do {
//						c.layout(true);
//						c = c.getParent();
//					} while (c != null);
//				}
//			});
		}
		
		@Override
		public Control getControl() {
			return propertiesControl;
		}

		@Override
		public void load(final Entry entry) {
//			this.entry = entry;
//
//			createPropertyControls();
//
//			/*
//			 * Note that this dialog is modal so changes cannot be made from outside the dialog.
//			 * Refreshing of the view after inserting and removing splits is handled by the commands
//			 * directly, so we are only interested in property changes.
//			 */
//			entry.getDataManager().addChangeListener(new SessionChangeAdapter() {
//				@Override
//				public void objectChanged(IModelObject changedObject, IScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
//					if (changedObject == entry) {
//						if (changedProperty == EntryInfo.getAccountAccessor()) {
//							createPropertyControls();
//							propertiesControl.layout(true);
//							Composite c = propertiesControl;
//							do {
//								c.layout(true);
//								c = c.getParent();
//							} while (c != null);
////			    	        shell.pack();
//							transactionDialog.refreshScrolling();
//
//							c = propertiesControl;
//							do {
//								c.layout(true);
//								c = c.getParent();
//							} while (c != null);
//
//						}
//					}
//				}
//			}, propertiesControl);


		}

//		private void createPropertyControls(Account account) {
//			for (Control control : propertiesControl.getChildren()) {
//				control.dispose();
//			}
//
//			properties = new ArrayList<IPropertyControl>();
//
//			if (account != null) {
//				for (ScalarPropertyAccessor<?, ? super Entry> accessor : EntryInfo.getPropertySet().getScalarProperties3()) {
//					// Be sure not to include the three properties that have their own columns
//					if (accessor != EntryInfo.getAccountAccessor()
//							&& accessor != EntryInfo.getMemoAccessor()
//							&& accessor != EntryInfo.getAmountAccessor()
//							&& isEntryPropertyApplicable(accessor, account)) {
//						createPropertyControl(propertiesControl, accessor);
//					}
//				}
//			}
//		}

		private boolean isEntryPropertyApplicable(
				ScalarPropertyAccessor<?,? super Entry> accessor, Entry entry, Account account) {

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

		private void createPropertyControl(Composite parent, ScalarPropertyAccessor accessor) {
//			Composite composite = new Composite(parent, SWT.NONE);
//			composite.setLayout(new GridLayout(2, false));
//			Label label = new Label(composite, SWT.LEFT);
//			label.setText(accessor.getDisplayName() + ":"); //$NON-NLS-1$
//			label.setForeground(labelColor);
//
//			final IPropertyControl propertyControl = accessor.createPropertyControl(composite);
//			propertyControl.getControl().setLayoutData(new GridData(accessor.getMinimumWidth(), SWT.DEFAULT));
//			propertyControl.getControl().setBackground(controlColor);
//
//			// TODO: This will not add listener to child controls - fix this.
//
//			// TODO: This is a kludge.
//			// We need this wrapper for just for two methods,
//			// select and unselect.  Do we really need those?
//			//
//			FocusListener controlFocusListener = new CellFocusListener<RowControl>(rowControl, new ICellControl2<Entry>() {
//				@Override
//				public Control getControl() {
//					return propertyControl.getControl();
//				}
//
//				@Override
//				public void load(Entry data) {
//					propertyControl.load(data);
//				}
//
//				@Override
//				public void save() {
//					propertyControl.save();
////					fireUserChange(coordinator);
//				}
//
//				@Override
//				public void setSelected() {
//					propertyControl.getControl().setBackground(RowControl.selectedCellColor);
//				}
//
//				@Override
//				public void setUnselected() {
//					propertyControl.getControl().setBackground(null);
//				}
//			});
//
//			/*
//			 * The control may in fact be a composite control, in which case the
//			 * composite control itself will never get the focus. Only the child
//			 * controls will get the focus, so we add the listener recursively
//			 * to all child controls.
//			 */
//			addFocusListenerRecursively(propertyControl.getControl(), controlFocusListener);
//
//			propertyControl.load(entry);
//			properties.add(propertyControl);
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
//			for (IPropertyControl control : properties) {
//				control.save();
//			}
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
