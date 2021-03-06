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

package net.sf.jmoney.entrytable;

import java.util.Comparator;

import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Transaction;

import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Represents a property that can be displayed in the entries table,
 * edited by the user, or used in the filter.
 * <P>
 * Only properties where a single value exists in the cell are supported
 * by this class.
 * <P>
 * The credit, debit, and balance columns are hard coded at the end
 * of the table and are not represented by objects of this class.
 *
 * @author Nigel Westbury
 */
abstract public class PropertyBlock<R, S extends ExtendableObject> extends IndividualBlock<R> {
	private ScalarPropertyAccessor<?,? super S> accessor;
	private String id;

	public PropertyBlock(ScalarPropertyAccessor<?,? super S> accessor, String source) {
		super(
				accessor.getDisplayName(),
				accessor.getMinimumWidth(),
				accessor.getWeight()
		);

		this.accessor = accessor;
		this.id = source + '.' + accessor.getName();
	}

	public PropertyBlock(ScalarPropertyAccessor<?,S> accessor, String source, String displayName) {
		super(
				displayName,
				accessor.getMinimumWidth(),
				accessor.getWeight()
		);

		this.accessor = accessor;
		this.id = source + '.' + accessor.getName();
	}

	public String getId() {
		return id;
	}

	/**
	 * Given the input data, returns the ExtendableObject that contains
	 * the property whose value is being shown in this column.
	 *
	 * @param data
	 * @return the object containing the property being show, or null if
	 * 		no input is set or if this column is not applicable to the given input
	 */
	public abstract S getObjectContainingProperty(R data);

	/**
	 * This method is called whenever the user makes a change to this value.
	 *
	 * This method is used by the RowControl objects to update other contents
	 * that may be affected by this control. The normal session change listener
	 * is not used because the RowControl wants to be notified only when the
	 * user makes changes from within this row control, not when a change has
	 * been made elsewhere.
	 *
	 * As most controls do not affect other controls, an empty default implementation
	 * is provided.
	 */
	public void fireUserChange(R rowControl) {
		// Default implementation does nothing.
	}

    @Override
	public Control createCellControl(Composite parent, R blockInput, RowControl rowControl) {
    	// method to name type of accessor
    	return myCreateCellControl(parent, blockInput, rowControl, accessor);
    }

//	private static final IBeanValueProperty<Control, Color> backgroundProperty = BeanProperties.value(Control.class, "background", Color.class);

    
	public <S2 extends ExtendableObject> Control myCreateCellControl(Composite parent, final R blockInput, final RowControl rowControl, ScalarPropertyAccessor<?, S2> accessor2) {

		IObservableValue<S> objectContainingProperty = new ComputedValue<S>() {
			@Override
			protected S calculate() {
				return blockInput == null ? null : getObjectContainingProperty(blockInput);
			}
		};
    	
		final Control control = accessor.createPropertyControl2(parent, objectContainingProperty);

		ICellControl2<R> cellControl = new ICellControl2<R>() {

		    	@Override
			public Control getControl() {
				return control;
			}

			@Override
			public void load(R data) {
//				S entryContainingProperty = getObjectContainingProperty(data);
//				control.load(entryContainingProperty);
				throw new UnsupportedOperationException();
			}

			@Override
			public void save() {
//				control.save();
//				fireUserChange(coordinator);
				
				// This is still called...
//				throw new UnsupportedOperationException();
			}

			@Override
			public void setSelected() {
				control.setBackground(RowControl.selectedCellColor);
//				backgroundProperty.setValue(control, RowControl.selectedCellColor);
			}

			@Override
			public void setUnselected() {
				control.setBackground(null);
//				backgroundProperty.setValue(control, null);
			}
		};

		FocusListener controlFocusListener = new CellFocusListener<RowControl>(rowControl, cellControl);

		// This is a little bit of a kludge.  Might be a little safer to implement a method
		// in IPropertyControl to add the focus listener?
		addFocusListenerRecursively(control, controlFocusListener);

//			textControl.addKeyListener(keyListener);
//			textControl.addTraverseListener(traverseListener);

		return control;
	}

	/**
	 * Add listeners to each control.
	 *
	 * @param control The control to listen to.
	 */
	private void addFocusListenerRecursively(Control control, FocusListener listener) {
		control.addFocusListener(listener);

		if (control instanceof Composite) {
			Composite composite = (Composite) control;
			for (Control childControl : composite.getChildren()) {
				addFocusListenerRecursively(childControl, listener);
			}
		}
	}

    @Override
	public Comparator<R> getComparator() {
		final Comparator<?> subComparator = accessor.getComparator();
		if (subComparator == null) {
			return null;
		} else {
			return new Comparator<R>() {
				@Override
				public int compare(R entryData1, R entryData2) {
					S extendableObject1 = getObjectContainingProperty(entryData1);
					S extendableObject2 = getObjectContainingProperty(entryData2);
					if (extendableObject1 == null && extendableObject2 == null) return 0;
					if (extendableObject1 == null) return 1;
					if (extendableObject2 == null) return -1;
					return accessor.getComparator().compare(extendableObject1, extendableObject2);
				}
			};
		}
	}

	public static PropertyBlock<IObservableValue<? extends EntryFacade>,Transaction> createTransactionColumn(
			final ScalarPropertyAccessor<?,Transaction> propertyAccessor) {
		return new PropertyBlock<IObservableValue<? extends EntryFacade>, Transaction>(propertyAccessor, "transaction") { //$NON-NLS-1$
			@Override
			public Transaction getObjectContainingProperty(IObservableValue<? extends EntryFacade> data) {
				// Note that input may not be set in which case null is returned
				return data.getValue() == null ? null : data.getValue().getMainEntry().getTransaction();
			}
		};
	}

	public static PropertyBlock<IObservableValue<? extends EntryFacade>, Entry> createEntryColumn(
			final ScalarPropertyAccessor<?,Entry> propertyAccessor) {
		return new PropertyBlock<IObservableValue<? extends EntryFacade>, Entry>(propertyAccessor, "entry") { //$NON-NLS-1$
			@Override
			public Entry getObjectContainingProperty(IObservableValue<? extends EntryFacade> data) {
				// Note that input may not be set in which case null is returned
				return data.getValue() == null ? null : data.getValue().getMainEntry();
			}
		};
	}

	/**
	 * This version allows the caller to override the text used in the header.
	 * @param propertyAccessor
	 * @param displayName the text to use in the header
	 * @return
	 */
	public static PropertyBlock<IObservableValue<? extends EntryFacade>, Entry> createEntryColumn(final ScalarPropertyAccessor<?,Entry> propertyAccessor, String displayName) {
		return new PropertyBlock<IObservableValue<? extends EntryFacade>, Entry>(propertyAccessor, "entry", displayName) { //$NON-NLS-1$
			@Override
			public Entry getObjectContainingProperty(IObservableValue<? extends EntryFacade> data) {
				// Note that input may not be set in which case null is returned
				return data.getValue() == null ? null : data.getValue().getMainEntry();
			}
		};
	}
}