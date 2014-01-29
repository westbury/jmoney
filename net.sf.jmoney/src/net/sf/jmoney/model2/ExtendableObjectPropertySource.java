package net.sf.jmoney.model2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jmoney.resources.Messages;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

public class ExtendableObjectPropertySource<S extends ExtendableObject> implements IPropertySource {

	/**
	 * If we return a value that implements IPropertySource then the property
	 * sheet will allow the user to edit the properties of the value but it will
	 * not allow the user to change the property value itself to reference a
	 * different object. ExtendableObject implements IPropertySource (though an
	 * adapter), and this is not the behavior we want because the value is
	 * really a 'reference', not an 'embedded' object.
	 * 
	 * To avoid this behavior, we wrap ExtendableObject objects in a class that
	 * hides the IPropertySource interface.
	 */
	public class Wrapper {

		public S extendableObject;
		
		private String valueAsText;

		public Wrapper(S extendableObject, ScalarPropertyAccessor<?, ? super S> accessor) {
			this.extendableObject = extendableObject;
			valueAsText = accessor.formatValueForTable(ExtendableObjectPropertySource.this.extendableObject);
		}
		
		@Override
		public
		String toString() {
			return valueAsText;
		}
	}

	private S extendableObject;
	
	private List<IPropertyDescriptor> descriptors = new ArrayList<IPropertyDescriptor>();
	
	private Map<String, ScalarPropertyAccessor<?,? super S>> accessorMap = new HashMap<String, ScalarPropertyAccessor<?,? super S>>();
	
	public static <S extends ExtendableObject> ExtendableObjectPropertySource<S> construct(ExtendablePropertySet<S> propertySet,
			ExtendableObject extendableObject) {
		return new ExtendableObjectPropertySource<S>(propertySet, (S)extendableObject);
	}

    ExtendableObjectPropertySource(ExtendablePropertySet<S> propertySet, final S extendableObject) {
    	this.extendableObject = extendableObject;
    	
		for (final ScalarPropertyAccessor<?,? super S> accessor : propertySet.getScalarProperties3()) {
			accessorMap.put(accessor.getName(), accessor);
			
			addToDescriptors(extendableObject, accessor);
		}
    }

	private <S1 extends ExtendableObject, S2 extends S1> void addToDescriptors(final S2 extendableObject,
			final ScalarPropertyAccessor<?, S1> accessor) {
		descriptors.add(new IPropertyDescriptor() {

			@Override
			public CellEditor createPropertyEditor(Composite parent) {
				
				CellEditor editor = new CellEditor() {

					IObservableValue<S1> observableValue = new WritableValue<S1>();
					
					@Override
					protected Control createControl(Composite parent) {
						return accessor.createPropertyControl2(parent, observableValue);
					}

					@Override
					protected Object doGetValue() {
						return observableValue.getValue();
					}

					@Override
					protected void doSetFocus() {
						// TODO need a proper way of setting focus
//							control.getControl().setFocus();
					}

					@Override
					protected void doSetValue(Object value) {
						observableValue.setValue(extendableObject);
					}

//						private <V> void setValue(ScalarPropertyAccessor<V> accessor, Object value) {
//							extendableObject.setPropertyValue(accessor, (V)accessor.getClass().cast(value));
//						}
				};
				
				editor.create(parent);
				
				return editor;
			}

			@Override
			public String getCategory() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getDescription() {
				return Messages.ExtendableObjectPropertySource_0 + accessor.displayName;
			}

			@Override
			public String getDisplayName() {
				return accessor.getDisplayName();
			}

			@Override
			public String[] getFilterFlags() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Object getHelpContextIds() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Object getId() {
				return accessor.getName();
			}

			@Override
			public ILabelProvider getLabelProvider() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean isCompatibleWith(
					IPropertyDescriptor anotherProperty) {
				// TODO Auto-generated method stub
				return false;
			}
		});
	}
    	

	@Override
	public Object getEditableValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
	    return descriptors.toArray(new IPropertyDescriptor[0]);
	}

	@Override
	public Object getPropertyValue(Object id) {
		ScalarPropertyAccessor<?,? super S> accessor = accessorMap.get(id);
		Object value = accessor.getValue(extendableObject);
		
		/*
		 * If we return a value that implements IPropertySource then the property
		 * sheet will allow the user to edit the properties of the value but it will
		 * not allow the user to change to property value itself to a different object.
		 * ExtendableObject implements IPropertySource (though an adapter), and this is
		 * not the behavior we want because the value is really a 'reference', not an
		 * 'embedded' object.
		 * 
		 * To avoid this behavior, we wrap ExtendableObject objects in a class that hides
		 * the IPropertySource interface.
		 */
		if (value instanceof ExtendableObject) {
			return new Wrapper((S)value, accessor);
		} else {
			return value;
		}
	}

	@Override
	public boolean isPropertySet(Object id) {
		/*
		 * Assume the property is set, because JMoney does not really
		 * have the concept of a property not being set.  This method
		 * is used only when resetting to default values (I think),
		 * and it does not matter if the property already has the default
		 * value.
		 */
		return true;
	}

	@Override
	public void resetPropertyValue(Object id) {
		ScalarPropertyAccessor<?,?> accessor = accessorMap.get(id);
		resetPropertyValue(accessor);
	}
	
	private <V> void resetPropertyValue(ScalarPropertyAccessor<V,? super S> accessor) {
		accessor.setValue(extendableObject, accessor.getDefaultValue());
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
//		if (value instanceof Wrapper) {
//			value = ((Wrapper)value).extendableObject;
//		}
//		setPropertyValue2(accessorMap.get(id), value);
	}


//	private <V> void setPropertyValue2(ScalarPropertyAccessor<V> accessor, Object value) {
//		extendableObject.setPropertyValue(accessor, (V)accessor.getClass().cast(value));
//
//	}
}
