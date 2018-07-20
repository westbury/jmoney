package net.sf.jmoney.model2;

import java.lang.reflect.Method;
import java.util.Comparator;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.masterdetail.IObservableFactory;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.property.list.IListProperty;
import org.eclipse.core.databinding.property.map.IMapProperty;
import org.eclipse.core.databinding.property.set.ISetProperty;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.sf.jmoney.isolation.IScalarPropertyAccessor;

public class ScalarPropertyAccessor<T, S extends ExtendableObject> extends PropertyAccessor<S> implements IScalarPropertyAccessor<T,S> {

	private IValueProperty<S, T> valueProperty;

	private int weight;

	private int minimumWidth;

	private boolean sortable;

	/**
	 * never null
	 */
	private IPropertyControlFactory<S,T> propertyControlFactory;

	/**
	 * The class of the property values (if the method signatures show int,
	 * long, boolean, or char, this field will be Integer.class, Long.class,
	 * Boolean.class, or Character.class)
	 */
	private Class<T> classOfValueObject;

	/**
	 * The class of the property values (if the method signatures show int,
	 * long, boolean, or char, this field will be int.class, long.class,
	 * boolean.class, or char.class)
	 */
	private Class<?> classOfValueType;

	private boolean nullAllowed;

	/**
	 * Index into the array of scalar properties.
	 */
//	private int indexIntoScalarProperties = -1;

	private Comparator<S> parentComparator;

	private IPropertyDependency<? super S> dependency;

	public ScalarPropertyAccessor(Class<T> classOfValueObject, PropertySet<?,S> propertySet, String localName, String displayName, int weight, int minimumWidth, final IPropertyControlFactory<S,T> propertyControlFactory, IPropertyDependency<? super S> propertyDependency) {
		super(propertySet, localName, displayName);
		this.classOfValueObject = classOfValueObject;
		this.weight = weight;
		this.minimumWidth = minimumWidth;
		this.sortable = true;
		this.propertyControlFactory = propertyControlFactory;
		this.dependency = propertyDependency;

		Class implementationClass = propertySet.getImplementationClass();

		if (classOfValueObject.isPrimitive()) {
			throw new MalformedPluginException("Property '" + localName + "' in '" + implementationClass.getName() + "' has been parameterized by a primitive type (" + classOfValueObject.getName() + ").  Although primitive types may be used by the getters and setters, the equivalent object classes must be used for parameterization."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		valueProperty = propertySet.createValueProperty(this);

		Method theGetMethod;
		// Use introspection on the interface to find the getter method.
		// Following the Java beans pattern, we allow the getter for a
		// boolean property to have a prefix of either "get" or "is".
		try {
			theGetMethod = findMethod("get", localName, null); //$NON-NLS-1$
		} catch (MalformedPluginException e) {
			try {
				theGetMethod = findMethod("is", localName, null); //$NON-NLS-1$
				if (theGetMethod.getReturnType() != boolean.class) {
					throw new MalformedPluginException("Method '" + theGetMethod.getName() + "' in '" + implementationClass.getName() + "' must return boolean."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			} catch (MalformedPluginException e2) {
				String propertyNamePart =
					localName.toUpperCase().substring(0, 1)
					+ localName.substring(1, localName.length());
				throw new MalformedPluginException("The " + propertySet.getImplementationClass().getName() + " class must have a method with a signature of get" + propertyNamePart + "() or, if a boolean property, a signature of is" + propertyNamePart + "()."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		}

		if (theGetMethod.getReturnType() == void.class) {
			throw new MalformedPluginException("Method '" + theGetMethod.getName() + "' in '" + implementationClass.getName() + "' must not return void."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		classOfValueType = theGetMethod.getReturnType();

		Class<?> classOfObjectReturnedByGetter;
		if (classOfValueType == int.class) {
			classOfObjectReturnedByGetter = Integer.class;
			nullAllowed = false;
		} else if (classOfValueType == long.class) {
			classOfObjectReturnedByGetter = Long.class;
			nullAllowed = false;
		} else if (classOfValueType == boolean.class) {
			classOfObjectReturnedByGetter = Boolean.class;
			nullAllowed = false;
		} else if (classOfValueType == char.class) {
			classOfObjectReturnedByGetter = Character.class;
			nullAllowed = false;
		} else {
			classOfObjectReturnedByGetter = classOfValueType;
			nullAllowed = true;
		}


		if (classOfObjectReturnedByGetter != classOfValueObject) {
			throw new MalformedPluginException("Method '" + theGetMethod.getName() + "' in '" + implementationClass.getName() + "' returns type '" + theGetMethod.getReturnType().getName() + "' but code metadata indicates the type should be '" + classOfValueObject.getName() + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}

		this.classOfValueObject = classOfValueObject;

		// Use introspection on the implementation class to find the setter method.
		Class parameterTypes[] = {classOfValueType};
		Method theSetMethod = findMethod("set", localName, parameterTypes); //$NON-NLS-1$

		if (theSetMethod.getReturnType() != void.class) {
			throw new MalformedPluginException("Method '" + theSetMethod.getName() + "' in '" + implementationClass.getName() + "' must return void type ."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		/*
		 * Set the comparator, if any.
		 *
		 * If this object has been given a comparator to compare the values of
		 * the properties, then return a comparator that compares the parent
		 * objects by getting the property value from each parent and comparing
		 * those values.  Otherwise return null to indicate that there is no
		 * ordering.
		 */
		if (propertyControlFactory.getComparator() != null) {
			parentComparator = new Comparator<S> () {
				@Override
				public int compare(S object1, S object2) {
					T value1 = getValue(object1);
					T value2 = getValue(object2);
					if (value1 == null && value2 == null) return 0;
					if (value1 == null) return 1;
					if (value2 == null) return -1;
					return propertyControlFactory.getComparator().compare(value1, value2);
				}
			};
		} else {
			parentComparator =  null;
		}
	}

	/**
	 * The width weighting to be used when this property is displayed
	 * in a table or a grid.  If the width available for the table
	 * or grid is more than the sum of the minimum widths then the
	 * excess width is distributed across the columns.
	 * This weight indicates how much the property
	 * can benefit from being given excess width.  For example
	 * a property containing a description can benefit, whereas
	 * a property containing a date cannot.
	 */
	public int getWeight() {
		return weight;
	}

	/**
	 * The minimum width to be used when this property is displayed
	 * in a table or a grid.
	 */
	public int getMinimumWidth() {
		return minimumWidth;
	}

	/**
	 * Indicates whether users are able to sort views based on this property.
	 */
	public boolean isSortable() {
		return sortable;
	}

	/**
	 * Indicates whether the property may take null values.
	 *
	 * All properties may take null values except properties whose values are
	 * the intrinsic types (int, long, boolean, char).  Properties of type Integer,
	 * Long, Boolean, and Character may take null values.
	 */
	public boolean isNullAllowed() {
		return nullAllowed;
	}

	/**
	 * The default value for a property is suitable for uses such
	 * as:
	 *
	 * - setting the default columnn value in a database
	 * - providing values when the value is missing from an
	 * 		XML file
	 *
	 * It is expected that this value is constant (the same value
	 * is always returned for a given property).  The results will
	 * be unpredicable if this is not the case.
	 *
	 * @return the default value to use for this property, which may
	 * 		be null if the property is of a nullable type
	 */
	public T getDefaultValue() {
		return propertyControlFactory.getDefaultValue();
	}

	/**
	 * Indicates whether the property may be edited by the user.
	 */
	public boolean isEditable() {
		return (propertyControlFactory.isEditable());
	}

	/**
	 * Return a comparator to be used to compare two extendable objects based on the value
	 * of this property.  This method looks to the comparator, if any, provided by
	 * the IPropertyControlFactory implementation.  The ordering is thus defined by the
	 * plug-in that added this property.
	 *
	 * @return a comparator, or null if no comparator was provided
	 * 		for use with this property
	 */
	public Comparator<S> getComparator() {
		return parentComparator;
	}

	public Method findMethod(String prefix, String propertyName, Class [] parameters) {
		String methodName = prefix
			+ propertyName.toUpperCase().charAt(0)
			+ propertyName.substring(1, propertyName.length());

		try {
			return getDeclaredMethodRecursively(propertySet.getImplementationClass(), methodName, parameters);
		} catch (NoSuchMethodException e) {
			String parameterText = ""; //$NON-NLS-1$
			if (parameters != null) {
				for (int paramIndex = 0; paramIndex < parameters.length; paramIndex++) {
					if (paramIndex > 0) {
						parameterText = parameterText + ", "; //$NON-NLS-1$
					}
					String className = parameters[paramIndex].getName();
					if (parameters[paramIndex].isArray()) {
						// The returned class name seems to be a mess when the class is an array,
						// so we tidy it up.
						parameterText = parameterText + className.substring(2, className.length()-1) + "[]"; //$NON-NLS-1$
					} else {
						parameterText = parameterText + className;
					}
				}
			}
			throw new MalformedPluginException("The " + propertySet.getImplementationClass().getName() + " class must have a method with a signature of " + methodName + "(" + parameterText + ")."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}

	/**
	 * Gets a method from an interface.
	 * Whereas Class.getDeclaredMethod finds a method from an
	 * interface, it will not find the method if the method is
	 * defined in an interface which the given interface extends.
	 * This method will find the method if any of the interfaces
	 * extended by this interface define the method.
	 */
	@SuppressWarnings("unchecked")
	private Method getDeclaredMethodRecursively(Class implementationClass, String methodName, Class[] arguments)
		throws NoSuchMethodException {
		Class classToTry = implementationClass;
		do {
			try {
				return classToTry.getDeclaredMethod(methodName, arguments);
			} catch (NoSuchMethodException e) {
				classToTry = classToTry.getSuperclass();
			}
		} while (classToTry != null);

		throw new NoSuchMethodException();
	}

	/**
	 * Create a Control object that edits the property.
	 *
	 * @param parent
	 * @return An interface to a wrapper class.
	 */
//	public IPropertyControl<S> createPropertyControl(Composite parent) {
//		/*
//		 * When a PropertyAccessor object is created, it is provided with an
//		 * interface to a factory that constructs control objects that edit the
//		 * property. We call into that factory to create an edit control.
//		 */
//		return propertyControlFactory.createPropertyControl(parent, this);
//	}

	public Control createPropertyControl(Composite parent, S object) {
		/*
		 * When a PropertyAccessor object is created, it is provided with an
		 * interface to a factory that constructs control objects that edit the
		 * property. We call into that factory to create an edit control.
		 */
		IObservableValue<S> constantValue = new WritableValue<S>(object, null);
		return propertyControlFactory.createPropertyControl(parent, this, constantValue);
	}

	public Control createPropertyControl2(Composite parent, IObservableValue<? extends S> master) {
		/*
		 * When a PropertyAccessor object is created, it is provided with an
		 * interface to a factory that constructs control objects that edit the
		 * property. We call into that factory to create an edit control.
		 */
		return propertyControlFactory.createPropertyControl(parent, this, master);
	}

	/**
	 * Format the value of a property so it can be embedded into a
	 * message.
	 *
	 * The returned value will look sensible when embedded in a message.
	 * Therefore null values and empty values are returned as non-empty
	 * text such as "none" or "empty".  Text values are placed in
	 * quotes unless sure that only a single word will be returned that
	 * would be readable without quotes.
	 * 
	 * 
	 *
	 * @return The value of the property formatted as appropriate.
	 */
	public String formatValueForMessage(S object) {
		// When a PropertyAccessor object is created, it is
		// provided with an interface to a factory that constructs
		// control objects that edit the property.
		// This factory can also format values for us.
		// We call into that factory to obtain the property value
		// and format it.

		// If null or the empty string is returned to us,
		// change to "empty".
		String formattedValue = propertyControlFactory.formatValueForMessage(object, this);
		return (formattedValue == null || formattedValue.length() == 0)
		? "empty" : formattedValue; //$NON-NLS-1$
	}

	/**
	 * Format the value of a property as appropriate for displaying in a
	 * table.
	 *
	 * The returned value is expected to be displayed in a table or some similar
	 * view.  Null and empty values are therefore returned as empty strings.
	 * Text values are not quoted.
	 *
	 * @return The value of the property formatted as appropriate.
	 */
	public String formatValueForTable(S object) {
		// When a PropertyAccessor object is created, it is
		// provided with an interface to a factory that constructs
		// control objects that edit the property.
		// This factory can also format values for us.
		// We call into that factory to obtain the property value
		// and format it.

		// If null is returned to us, change to the empty string.
		String formattedValue = propertyControlFactory.formatValueForTable(object, this);
		return (formattedValue == null)
		? "" : formattedValue; //$NON-NLS-1$
	}

	/**
	 * Indicates if the property is a list of objects.
	 */
	@Override
	public boolean isList() {
		return false;
	}

	/**
	 * Returns the class for the values of this property. This is
	 * usually the class that is returned by the getter method, but if
	 * the getter method returns int, long, boolean, or char then this
	 * method will return Integer.class, Long.class, Boolean.class, or
	 * Character.class.
	 *
	 * @return the class of the property values (if the method signatures show int,
	 * long, boolean, or char, this field will be Integer.class, Long.class,
	 * Boolean.class, or Character.class)
	 */
	@Override
	public Class<T> getClassOfValueObject() {
		return classOfValueObject;
	}

	/**
	 * Returns the class for the values of this property. This is the
	 * class that is returned by the getter method.
	 *
	 * @return the class of the property values (if the method signatures show int,
	 * long, boolean, or char, this field will be int.class, long.class,
	 * boolean.class, or char.class)
	 */
	public Class<?> getClassOfValueType() {
		return classOfValueType;
	}

//	/**
//	 * It is often useful to have an array of property values
//	 * of an extendable object.  This array contains all scalar
//	 * properties in the extendable object, including extension
//	 * properties and properties from any base property sets.
//	 * <P>
//	 * In these arrays, the properties (including extension properties)
//	 * from the base property sets are put first in the array.
//	 * This means a given property will always be at the same index
//	 * in the array regardless of the actual derived property set.
//	 * <P>
//	 * This index is guaranteed to match the order in which
//	 * properties are returned by the PropertySet.getPropertyIterator_Scalar3().
//	 * i.e. if this method returns n then in every case where the
//	 * collection returned by getPropertyIterator_Scalar3 contains this property,
//	 * this property will be returned as the (n+1)'th element in the collection.
//	 *
//	 * @return the index of this property in the list of scalar
//	 * 			properties for the class.  This method returns zero
//	 * 			for the first scalar property returned by
//	 * 			PropertySet.getPropertyIterator3() and so on.
//	 */
//	@Override
//	public int getIndexIntoScalarProperties() {
//		return indexIntoScalarProperties;
//	}

	// TODO: This method should be accessible only from within the package.
//	public void setIndexIntoScalarProperties(int indexIntoScalarProperties) {
//		this.indexIntoScalarProperties = indexIntoScalarProperties;
//	}

	/**
	 * Indicates if this property is applicable.  An instance of an object
	 * containing this property is passed.  This method should look to the values
	 * of the other properties in the object to determine if this property is applicable.
	 *
	 * If the property is not applicable then the UI should not show a value for this
	 * property nor allow it to be updated.
	 *
	 * @return
	 */
	public boolean isPropertyApplicable(S containingObject) {
		if (dependency == null) {
			return true;
		} else {
			return dependency.isApplicable(containingObject);
		}
	}

	@Override
	public T getValue(S extendableObject) {
		return valueProperty.getValue(extendableObject);
	}

	@Override
	public void setValue(S extendableObject, T value) {
		valueProperty.setValue(extendableObject, value);
	}

	public <V2> ScalarPropertyAccessor<? super V2, S> typeIfGivenValue(
			S extendableObject, V2 value) {
		if (getValue(extendableObject) == value) {
			return (ScalarPropertyAccessor<? super V2, S>)this;
		} else {
			return null;
		}
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	@Override
	public Object getValueType() {
		return valueProperty.getValueType();
	}

	@Override
	public IObservableValue<T> observe(S source) {
		return valueProperty.observe(source);
	}

	@Override
	public IObservableValue<T> observe(Realm realm, S source) {
		return valueProperty.observe(realm, source);
	}

	@Override
	public IObservableFactory<S, IObservableValue<T>> valueFactory() {
		return valueProperty.valueFactory();
	}

	@Override
	public IObservableFactory<S, IObservableValue<T>> valueFactory(Realm realm) {
		return valueProperty.valueFactory(realm);
	}

	@Override
	public <M extends S> IObservableValue<T> observeDetail(
			IObservableValue<M> master) {
		return valueProperty.observeDetail(master);
	}

	@Override
	public <M extends S> IObservableList<T> observeDetail(IObservableList<M> master) {
		return valueProperty.observeDetail(master);
	}

	@Override
	public <M extends S> IObservableMap<M,T> observeDetail(IObservableSet<M> master) {
		return valueProperty.observeDetail(master);
	}

	@Override
	public <K, V extends S> IObservableMap<K, T> observeDetail(
			IObservableMap<K, V> masterMap) {
		return valueProperty.observeDetail(masterMap);
	}

	@Override
	public <M> IValueProperty<S, M> value(
			IValueProperty<? super T, M> detailValue) {
		return valueProperty.value(detailValue);
	}

	@Override
	public <M> IListProperty<S, M> list(IListProperty<? super T, M> detailList) {
		return valueProperty.list(detailList);
	}

	@Override
	public <E> ISetProperty<S, E> set(ISetProperty<? super T, E> detailSet) {
		return valueProperty.set(detailSet);
	}

	@Override
	public <K, V> IMapProperty<S, K, V> map(
			IMapProperty<? super T, K, V> detailMap) {
		return valueProperty.map(detailMap);
	}
}
