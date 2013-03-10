package net.sf.jmoney;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

public class Helper {

	public static <S extends ExtendableObject> void copyScalarValues(
			ExtendablePropertySet<S> propertySet,
			S source,
			S destination) {
		for (ScalarPropertyAccessor<?, ? super S> accessor : propertySet.getScalarProperties3()) {
			copyValue(accessor, source, destination);
		}
	}

	private static <S extends ExtendableObject, V> void copyValue(ScalarPropertyAccessor<V, ? super S> accessor, S source, S destination) {
		V value = accessor.getValue(source);
		accessor.setValue(destination, value);
	}
}
