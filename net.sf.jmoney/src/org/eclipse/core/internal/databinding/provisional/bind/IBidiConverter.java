package org.eclipse.core.internal.databinding.provisional.bind;

/**
 * Interface for bi-directional conversions.
 * <P>
 * Bi-directional conversions encapsulate both the target-to-model and the
 * model-to-target conversions in a single converter. This simplifies the API
 * for two-way binding as only one converter need be specified. It also helps to
 * ensure consistency in the conversions.
 * <P>
 * The two conversions must be consistent with each other. More specifically,
 * for any value of type T1, call modelToTarget to get a type T2, pass that
 * value to targetToModel to get a type T1, pass that value to modelToTarget to
 * get a type T2. The value returned by the second call to modelToTarget must be
 * 'equal' to the value returned by the first call to modelToTarget ('equal'
 * meaning the 'equal' method returns 'true'). Likewise, for any value of type
 * T2, call targetToModel to get a type T1, pass that value to modelToTarget to
 * get a type T2, pass that value to targetToModel to get a type T1. The value
 * returned by the second call to targetToModel must be 'equal' to the value
 * returned by the first call to targetToModel.
 * <P>
 * Note that the above rules allow the conversion to start with non-canonical
 * forms but the first conversion must always result in a canonical form
 * (meaning if the value is conceptually the same then it must be 'equal'. For
 * example, "12,345.300" may be allowed as the target, this being converted to a
 * BigDecimal with a value of 12345.30 in the model, being converted back to
 * "12345.30" in the target. This does not 'equal' the original target value but
 * converting this back again to the model data type must result in a BigDecimal
 * that 'equals' the BigDecimal obtained from the first conversion.
 * 
 * @since 1.5
 * @param <T1>
 *            the data type on the model side
 * @param <T2>
 *            the data type on the target side
 */
public interface IBidiConverter<T1, T2> {

	/**
	 * @param fromObject
	 * @return the value converted for use on the target side
	 */
	T2 modelToTarget(T1 fromObject);

	/**
	 * @param fromObject
	 * @return the value converted for use on the model side
	 */
	T1 targetToModel(T2 fromObject);

}
