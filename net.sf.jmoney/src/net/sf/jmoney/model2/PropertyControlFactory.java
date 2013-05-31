package net.sf.jmoney.model2;

import java.util.Comparator;

import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * This class provides a default implementation of some of the methods
 * in the IPropertyControlFactory interface.  Plug-ins do not have to use
 * this class but can instead implement the interface directly.
 * <P>
 * This class, like the IPropertyControlFactory interface, is parameterized
 * with the class of values of the property.  However, this class further
 * requires that the class implements the Comparable interface.  This allows
 * this class to provide a default implementation of the getComparator method.
 * If the values of the property are of a class that does not implement Comparable
 * then this helper class cannot be used.
 *
 * @author Nigel Westbury
 *
 */
public abstract class PropertyControlFactory<S extends ExtendableObject, V extends Comparable<? super V>> implements IPropertyControlFactory<S,V> {

	   @Override
		public IPropertyControl<S> createPropertyControl(Composite parent, ScalarPropertyAccessor<V,S> propertyAccessor) {
	    	final WritableValue<S> observable = new WritableValue<S>();

	    	final Control control = createPropertyControl(parent, propertyAccessor, observable);

	        return new IPropertyControl<S>() {
				@Override
				public Control getControl() {
					return control;
				}

				@Override
				public void load(S object) {
					observable.setValue(object);
				}

				@Override
				public void save() {
					// Nothing to do because should always be saved
					// by data binding.
				}
			};
	    }

	@Override
	public String formatValueForTable(S extendableObject, ScalarPropertyAccessor<? extends V,S> propertyAccessor) {
		V value = propertyAccessor.getValue(extendableObject);
		return value.toString();
	}

	@Override
	public String formatValueForMessage(S extendableObject, ScalarPropertyAccessor<? extends V,S> propertyAccessor) {
		V value = propertyAccessor.getValue(extendableObject);
		return value.toString();
	}

	@Override
	public Comparator<V> getComparator() {
		return new Comparator<V>() {
			@Override
			public int compare(V value1, V value2) {
				return value1.compareTo(value2);
			}
		};
	}
}
