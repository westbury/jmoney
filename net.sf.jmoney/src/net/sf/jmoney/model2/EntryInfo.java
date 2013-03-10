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

import java.util.Date;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;
import net.sf.jmoney.fields.AccountControlFactory;
import net.sf.jmoney.fields.AmountControlFactory;
import net.sf.jmoney.fields.AmountEditor;
import net.sf.jmoney.fields.CurrencyControlFactory;
import net.sf.jmoney.fields.DateControlFactory;
import net.sf.jmoney.fields.TextControlFactory;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.isolation.SessionChangeAdapter;
import net.sf.jmoney.resources.Messages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

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
public class EntryInfo implements IPropertySetInfo {

	/**
	 * Date format used for the creation timestamp.
	 */
	private VerySimpleDateFormat dateFormat = new VerySimpleDateFormat(
            JMoneyPlugin.getDefault().getDateFormat());

    // Listen to date format changes so we keep up to date
    // Johann, 2005-07-02: Shouldn't this be done in the control?
    /*
    static {
        JMoneyPlugin.getDefault().getPreferenceStore()
                .addPropertyChangeListener(new IPropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent event) {
                        if (event.getProperty().equals("dateFormat")) {
                            fDateFormat = new VerySimpleDateFormat(JMoneyPlugin
                                    .getDefault().getDateFormat());
                        }
                    }
                });
    }
    */

	
	private static ExtendablePropertySet<Entry> propertySet = PropertySet.addBaseFinalPropertySet(Entry.class, Messages.EntryInfo_Description, new IExtendableObjectConstructors<Entry>() {

		@Override
		public Entry construct(IObjectKey objectKey, ListKey parentKey) {
			return new Entry(objectKey, parentKey);
		}

		@Override
		public Entry construct(IObjectKey objectKey,
				ListKey<? super Entry,?> parentKey, IValues<Entry> values) {
			return new Entry(
					objectKey, 
					parentKey, 
					values.getScalarValue(EntryInfo.getCheckAccessor()),
					values.getReferencedObjectKey(EntryInfo.getAccountAccessor()),
					values.getScalarValue(EntryInfo.getValutaAccessor()),
					values.getScalarValue(EntryInfo.getMemoAccessor()),
					values.getScalarValue(EntryInfo.getAmountAccessor()),
					values.getReferencedObjectKey(EntryInfo.getCommodityAccessor()),
					values.getScalarValue(EntryInfo.getCreationAccessor()),
					values.getReferencedObjectKey(EntryInfo.getIncomeExpenseCurrencyAccessor()),
					values 
			);
		}
	});

	
	private static ScalarPropertyAccessor<String,Entry> checkAccessor = null;
	private static ReferencePropertyAccessor<Account,Entry> accountAccessor = null;
	private static ScalarPropertyAccessor<Date,Entry> valutaAccessor = null;
	private static ScalarPropertyAccessor<String,Entry> memoAccessor = null;
	private static ScalarPropertyAccessor<Long,Entry> amountAccessor = null;
	private static ReferencePropertyAccessor<Commodity,Entry> commodityAccessor = null;
	private static ScalarPropertyAccessor<Long,Entry> creationAccessor = null;
	private static ReferencePropertyAccessor<Currency,Entry> incomeExpenseCurrencyAccessor = null;

	@Override
	public PropertySet registerProperties() {
		IPropertyControlFactory<Entry,String> textControlFactory = new TextControlFactory<Entry>();
        IPropertyControlFactory<Entry,Date> dateControlFactory = new DateControlFactory<Entry>();
        IReferenceControlFactory<Entry,Entry,Account> accountControlFactory = new AccountControlFactory<Entry,Entry,Account>() {
			@Override
			public IObjectKey getObjectKey(Entry parentObject) {
				return parentObject.accountKey;
			}
		};

        IPropertyControlFactory<Entry,Long> amountControlFactory = new AmountControlFactory<Entry>() {
		    @Override	
			protected Commodity getCommodity(Entry object) {
				// If not enough information has yet been set to determine
				// the currency of the amount in this entry, return
				// the default currency.
	    	    Commodity commodity = object.getCommodityInternal();
	    	    if (commodity == null) {
	    	    	commodity = object.getSession().getDefaultCurrency();
	    	    }
	    	    return commodity;
			}

			@Override
			public IPropertyControl<Entry> createPropertyControl(Composite parent, ScalarPropertyAccessor<Long,Entry> propertyAccessor) {
		    	final AmountEditor<Entry> editor = new AmountEditor<Entry>(parent, propertyAccessor, this);
		        
		    	// The format of the amount will change if either
		    	// the account property of the entry changes or if
		    	// the commodity property of the account changes.
		        editor.setListener(new SessionChangeAdapter() {
		        		@Override	
		        		public void objectChanged(IModelObject changedObject, IScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
		        			Entry entry = (Entry)editor.getObject();
		        			if (entry == null) {
		        			    return;
		        			}
		        			// Has the account property changed?
		        			if (changedObject.equals(entry) && changedProperty == EntryInfo.getAccountAccessor()) {
		        				editor.updateCommodity(entry.getCommodityInternal());	
		        			}
		        			// Has the currency property of the account changed?
		        			if (changedObject ==  entry.getAccount() && changedProperty == CurrencyAccountInfo.getCurrencyAccessor()) {
		        				editor.updateCommodity(entry.getCommodityInternal());	
		        			}
		        			// If any property in the commodity object changed then
		        			// the format of the amount might also change.
		        			if (changedObject ==  entry.getCommodityInternal()) {
		        				editor.updateCommodity(entry.getCommodityInternal());	
		        			}
		        			
		        			// TODO: All the above tests are still not complete.
		        			// If the account for the entry can contain multiple
		        			// commodities then the commodity may depend on properties
		        			// in the entry object.  We really need a special listener
		        			// that listens for any changes that would affect the
		        			// Entry.getCommodity() value.
		        		}
		        	});   	
		        
		        return editor;
			}};

		IPropertyControlFactory<Entry,Long> creationControlFactory = new PropertyControlFactory<Entry,Long>() {

			@Override
			public String formatValueForTable(Entry extendableObject, ScalarPropertyAccessor<? extends Long,Entry> propertyAccessor) {
				Long value = propertyAccessor.getValue(extendableObject);
				Date date = new Date(value);
				date.setTime(value);
		        return dateFormat.format(date);
			}

			@Override
			public String formatValueForMessage(Entry extendableObject, ScalarPropertyAccessor<? extends Long,Entry> propertyAccessor) {
				Long value = propertyAccessor.getValue(extendableObject);
				Date date = new Date(value);
				date.setTime(value);
		        return dateFormat.format(date);
			}

			@Override
			public IPropertyControl<Entry> createPropertyControl(Composite parent, final ScalarPropertyAccessor<Long,Entry> propertyAccessor) {
				// This property is not editable
				final Label control = new Label(parent, SWT.NONE);
				
		    	return new IPropertyControl<Entry>() {
					@Override
					public Control getControl() {
						return control;
					}
					@Override
					public void load(Entry object) {
						control.setText(formatValueForTable(object, propertyAccessor));
					}
					@Override
					public void save() {
						// Not editable so nothing to do
					}
				};
			}

			@Override
			public Long getDefaultValue() {
				return 0L;
			}

			@Override
			public boolean isEditable() {
				return false;
			}
		};

		IReferenceControlFactory<Entry,Entry,Commodity> commodityControlFactory = new CommodityControlFactory<Entry,Entry>() {
			@Override
			public IObjectKey getObjectKey(Entry parentObject) {
				return parentObject.commodityKey;
			}
		};
		
		IReferenceControlFactory<Entry,Entry,Currency> currencyControlFactory = new CurrencyControlFactory<Entry,Entry>() {
			@Override
			public IObjectKey getObjectKey(Entry parentObject) {
				return parentObject.incomeExpenseCurrencyKey;
			}
		};
		
		IPropertyDependency<Entry> onlyIfIncomeExpenseAccount = new IPropertyDependency<Entry>() {
			@Override
			public boolean isApplicable(Entry entry) {
				return entry.getAccount() instanceof IncomeExpenseAccount;
			}
		};
		
		IPropertyDependency<Entry> onlyIfCurrencyAccount = new IPropertyDependency<Entry>() {
			@Override
			public boolean isApplicable(Entry entry) {
				return entry.getAccount() instanceof CurrencyAccount;
			}
		};
		
		IPropertyDependency<Entry> onlyIfBankAccount = new IPropertyDependency<Entry>() {
			@Override
			public boolean isApplicable(Entry entry) {
				return entry.getAccount() instanceof BankAccount;
			}
		};
		
		checkAccessor       = propertySet.addProperty("check",Messages.EntryInfo_Check,String.class, 2, 50,  textControlFactory, onlyIfBankAccount); //$NON-NLS-1$
		accountAccessor     = propertySet.addProperty("account",Messages.EntryInfo_Category,Account.class, 2, 70,  accountControlFactory, null); //$NON-NLS-1$
		valutaAccessor      = propertySet.addProperty("valuta",Messages.EntryInfo_Valuta,Date.class, 0, 74,  dateControlFactory, onlyIfCurrencyAccount); //$NON-NLS-1$
		memoAccessor        = propertySet.addProperty("memo",Messages.EntryInfo_Memo,String.class, 5, 100, textControlFactory, null); //$NON-NLS-1$
		amountAccessor      = propertySet.addProperty("amount",Messages.EntryInfo_Amount,Long.class, 2, 70,  amountControlFactory, null); //$NON-NLS-1$
		commodityAccessor   = propertySet.addProperty("commodity","Commodity",Commodity.class, 2, 70, commodityControlFactory, null); //$NON-NLS-1$
		creationAccessor    = propertySet.addProperty("creation",Messages.EntryInfo_Creation,Long.class, 0, 70,  creationControlFactory, null); //$NON-NLS-1$
		incomeExpenseCurrencyAccessor = propertySet.addProperty("incomeExpenseCurrency",Messages.EntryInfo_Currency,Currency.class, 2, 70, currencyControlFactory, onlyIfIncomeExpenseAccount); //$NON-NLS-1$
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<Entry> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,Entry> getCheckAccessor() {
		return checkAccessor;
	}	

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<Account,Entry> getAccountAccessor() {
		return accountAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Date,Entry> getValutaAccessor() {
		return valutaAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,Entry> getMemoAccessor() {
		return memoAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Long,Entry> getAmountAccessor() {
		return amountAccessor;
	}	

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<Commodity,Entry> getCommodityAccessor() {
		return commodityAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Long,Entry> getCreationAccessor() {
		return creationAccessor;
	}	

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<Currency,Entry> getIncomeExpenseCurrencyAccessor() {
		return incomeExpenseCurrencyAccessor;
	}	
}
