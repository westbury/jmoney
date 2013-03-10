/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.model2;

import net.sf.jmoney.fields.TextControlFactory;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.resources.Messages;

import org.eclipse.swt.widgets.Composite;

/**
 * This class is a listener class to the net.sf.jmoney.fields
 * extension point.  It implements an extension.
 * <P>
 * This extension registers the Entry properties.  By registering
 * the properties, every one can know how to display, edit, and store
 * the properties.
 * <P>
 * These properties are supported in the JMoney base code, so everyone
 * including plug-ins will know about these properties.  However, to
 * follow the Eclipse paradigm (every one should be treated equal,
 * including oneself), these are registered through the same extension
 * point that plug-ins must also use to register their properties.
 * 
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class CurrencyInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<Currency> propertySet = PropertySet.addDerivedFinalPropertySet(Currency.class, Messages.CurrencyInfo_Description, CommodityInfo.getPropertySet(), new IExtendableObjectConstructors<Currency>() {

		@Override
		public Currency construct(IObjectKey objectKey, ListKey parentKey) {
			return new Currency(
					objectKey, 
					parentKey
			);
		}

		@Override
		public Currency construct(IObjectKey objectKey,
				ListKey<? super Currency,?> parentKey, IValues<Currency> values) {
			return new Currency(
					objectKey, 
					parentKey, 
					values.getScalarValue(CommodityInfo.getNameAccessor()),
					values.getScalarValue(CurrencyInfo.getCodeAccessor()),
					values.getScalarValue(CurrencyInfo.getDecimalsAccessor()),
					values
			);
		}
	});

	private static ScalarPropertyAccessor<String,Currency> codeAccessor = null;
	private static ScalarPropertyAccessor<Integer,Currency> decimalsAccessor = null;

	@Override
	public PropertySet registerProperties() {
		IPropertyControlFactory<Currency,String> textControlFactory = new TextControlFactory<Currency>();
		
		IPropertyControlFactory<Currency,Integer> numberControlFactory = new PropertyControlFactory<Currency,Integer>() {
			@Override
			public IPropertyControl<Currency> createPropertyControl(Composite parent, ScalarPropertyAccessor<Integer,Currency> propertyAccessor) {
				// Property is not editable
				return null;
			}

			@Override
			public Integer getDefaultValue() {
				return 0;
			}

			@Override
			public boolean isEditable() {
				return false;
			}
		};

		codeAccessor = propertySet.addProperty("code", Messages.CurrencyInfo_Code, String.class, 0, 8, textControlFactory, null); //$NON-NLS-1$
		decimalsAccessor = propertySet.addProperty("decimals", Messages.CurrencyInfo_DecimalPlace, Integer.class, 0, 8, numberControlFactory, null); //$NON-NLS-1$
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<Currency> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,Currency> getCodeAccessor() {
		return codeAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Integer,Currency> getDecimalsAccessor() {
		return decimalsAccessor;
	}	
}
