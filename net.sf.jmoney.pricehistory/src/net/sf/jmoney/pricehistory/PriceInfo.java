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

package net.sf.jmoney.pricehistory;

import java.util.Date;

import net.sf.jmoney.fields.AmountControlFactory;
import net.sf.jmoney.fields.DateControlFactory;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IExtendableObjectConstructors;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiConverter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * This class is a listener class to the net.sf.jmoney.fields
 * extension point.  It implements an extension.
 * <P>
 * This extension registers the Price properties.  By registering
 * the properties, every one can know how to display, edit, and store
 * the properties.
 *
 * @author Nigel Westbury
 */
public class PriceInfo implements IPropertySetInfo {


	private static ExtendablePropertySet<Price> propertySet = PropertySet.addBaseFinalPropertySet(Price.class, "Commodity Historical Price", new IExtendableObjectConstructors<Price>() {

		@Override
		public Price construct(IObjectKey objectKey, ListKey<? super Price,?> parentKey) {
			return new Price(objectKey, parentKey);
		}

		@Override
		public Price construct(IObjectKey objectKey,
				ListKey<? super Price,?> parentKey, IValues<Price> values) {
			return new Price(
					objectKey,
					parentKey,
					values.getScalarValue(PriceInfo.getDateAccessor()),
					values.getScalarValue(PriceInfo.getPriceAccessor()),
					values
			);
		}
	});

	private static ScalarPropertyAccessor<Date,Price> dateAccessor = null;
	private static ScalarPropertyAccessor<Long,Price> priceAccessor = null;

	@Override
	public ExtendablePropertySet<Price> registerProperties() {
        IPropertyControlFactory<Price,Date> dateControlFactory = new DateControlFactory<Price>();

        IPropertyControlFactory<Price,Long> amountControlFactory = new AmountControlFactory<Price>() {
		    @Override
			protected Commodity getCommodity(Price object) {
				// If not enough information has yet been set to determine
				// the currency of the amount in this entry, return
				// the default currency.
	    	    Commodity commodity = (Commodity)object.getParentKey().getObject();
	    	    Currency currency = CommodityPricingInfo.getCurrencyAccessor().getValue(commodity);
	    	    if (currency == null) {
	    	    	currency = object.getSession().getDefaultCurrency();
	    	    }
	    	    return currency;
			}

			@Override
			public Control createPropertyControl(Composite parent,
					ScalarPropertyAccessor<Long,Price> propertyAccessor, final IObservableValue<? extends Price> modelObservable) {
		    	Text propertyControl = new Text(parent, SWT.TRAIL);

		    	IBidiConverter<Long,String> amountToText = new IBidiConverter<Long,String>() {
					@Override
					public String modelToTarget(Long fromObject) {
						Commodity commodity = getCommodity(modelObservable.getValue());
						return commodity.format(fromObject.longValue());
					}

					@Override
					public Long targetToModel(String amountString) {
						Commodity commodity = getCommodity(modelObservable.getValue());
				        if (amountString.trim().length() == 0) {
							/*
							 * The text box is empty so this maps to null. The property
							 * may be 'long' in the model and so not accept nulls.
							 * However that is the responsibility of the binding chain
							 * outside of this class to detect that and behave
							 * accordingly.
							 */
				        	return null;
				        } else {
				        	try {
				        		return commodity.parse(amountString);
				        	} catch (CoreException e) {
				        		StatusManager.getManager().handle(e.getStatus());
				        		return null;
				        	}
				        }
					}
				};

				Bind.twoWay(propertyAccessor.observeDetail(modelObservable))
				.convertWithTracking(amountToText)
				.to(SWTObservables.observeText(propertyControl, SWT.Modify));

				Bind.bounceBack(amountToText)
				.to(SWTObservables.observeText(propertyControl, SWT.FocusOut));

				return propertyControl;
			}

			};


		dateAccessor  = propertySet.addProperty("date",  "Date",  Date.class,1, 20,  dateControlFactory,   null);
		priceAccessor = propertySet.addProperty("price", "Price", Long.class, 2, 50, amountControlFactory, null);

		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<Price> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Date,Price> getDateAccessor() {
		return dateAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Long,Price> getPriceAccessor() {
		return priceAccessor;
	}
}
