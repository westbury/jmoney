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

package net.sf.jmoney.stocks.model;

import java.util.Comparator;

import net.sf.jmoney.fields.AccountControlFactory;
import net.sf.jmoney.fields.CurrencyControlFactory;
import net.sf.jmoney.fields.TextControlFactory;
import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.isolation.IValues;
import net.sf.jmoney.isolation.ListKey;
import net.sf.jmoney.model2.AccountInfo;
import net.sf.jmoney.model2.CapitalAccountInfo;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IExtendableObjectConstructors;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IReferenceControlFactory;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ReferencePropertyAccessor;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.stocks.resources.Messages;

import org.eclipse.swt.widgets.Composite;

/**
 * This class implements an extension to the net.sf.jmoney.fields
 * extension point.  It registers the StockAccount model class.
 *
 * @author Nigel Westbury
 */
public class StockAccountInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<StockAccount> propertySet = PropertySet.addDerivedFinalPropertySet(StockAccount.class, "Stock Account", CapitalAccountInfo.getPropertySet(), new IExtendableObjectConstructors<StockAccount>() {

		@Override
		public StockAccount construct(IObjectKey objectKey, ListKey parentKey) {
			return new StockAccount(
					objectKey,
					parentKey
					);
		}

		@Override
		public StockAccount construct(IObjectKey objectKey,
				ListKey<? super StockAccount,?> parentKey, IValues<StockAccount> values) {
			return new StockAccount(
					objectKey,
					parentKey,
					values.getScalarValue(AccountInfo.getNameAccessor()),
					values.getListManager(objectKey, CapitalAccountInfo.getSubAccountAccessor()),
					values.getScalarValue(CapitalAccountInfo.getAbbreviationAccessor()),
					values.getScalarValue(CapitalAccountInfo.getCommentAccessor()),
					values.getReferencedObjectKey(StockAccountInfo.getCurrencyAccessor()),
					values.getScalarValue(StockAccountInfo.getBrokerageFirmAccessor()),
					values.getScalarValue(StockAccountInfo.getAccountNumberAccessor()),
					values.getReferencedObjectKey(StockAccountInfo.getDividendAccountAccessor()),
					values.getReferencedObjectKey(StockAccountInfo.getReturnOfCapitalAccountAccessor()),
					values.getReferencedObjectKey(StockAccountInfo.getWithholdingTaxAccountAccessor()),
					values.getScalarValue(StockAccountInfo.getTax1NameAccessor()),
					values.getScalarValue(StockAccountInfo.getTax2NameAccessor()),
					values.getReferencedObjectKey(StockAccountInfo.getCommissionAccountAccessor()),
					values.getReferencedObjectKey(StockAccountInfo.getTax1AccountAccessor()),
					values.getReferencedObjectKey(StockAccountInfo.getTax2AccountAccessor()),
					values.getScalarValue(StockAccountInfo.getBuyCommissionRatesAccessor()),
					values.getScalarValue(StockAccountInfo.getSellCommissionRatesAccessor()),
					values.getScalarValue(StockAccountInfo.getTax1RatesAccessor()),
					values.getScalarValue(StockAccountInfo.getTax2RatesAccessor()),
					values
					);
		}
	});

	private static ReferencePropertyAccessor<Currency,StockAccount> currencyAccessor = null;
	private static ScalarPropertyAccessor<String,StockAccount> brokerageFirmAccessor = null;
	private static ScalarPropertyAccessor<String,StockAccount> accountNumberAccessor = null;
	private static ScalarPropertyAccessor<String,StockAccount> tax1NameAccessor = null;
	private static ScalarPropertyAccessor<String,StockAccount> tax2NameAccessor = null;
	private static ReferencePropertyAccessor<IncomeExpenseAccount,StockAccount> dividendAccountAccessor = null;
	private static ReferencePropertyAccessor<IncomeExpenseAccount,StockAccount> returnOfCapitalAccountAccessor = null;
	private static ReferencePropertyAccessor<IncomeExpenseAccount,StockAccount> withholdingTaxAccountAccessor = null;
	private static ReferencePropertyAccessor<IncomeExpenseAccount,StockAccount> commissionAccountAccessor = null;
	private static ReferencePropertyAccessor<IncomeExpenseAccount,StockAccount> tax1AccountAccessor = null;
	private static ReferencePropertyAccessor<IncomeExpenseAccount,StockAccount> tax2AccountAccessor = null;
	private static ScalarPropertyAccessor<RatesTable,StockAccount> buyCommissionRatesAccessor = null;
	private static ScalarPropertyAccessor<RatesTable,StockAccount> sellCommissionRatesAccessor = null;
	private static ScalarPropertyAccessor<RatesTable,StockAccount> tax1RatesAccessor = null;
	private static ScalarPropertyAccessor<RatesTable,StockAccount> tax2RatesAccessor = null;

	@Override
	public PropertySet registerProperties() {

		IPropertyControlFactory<StockAccount,String> textControlFactory = new TextControlFactory<StockAccount>();

		IReferenceControlFactory<StockAccount,StockAccount,Currency> currencyControlFactory = new CurrencyControlFactory<StockAccount,StockAccount>() {
			@Override
			public IObjectKey getObjectKey(StockAccount parentObject) {
				return parentObject.currencyKey;
			}
		};

		IPropertyControlFactory<StockAccount,RatesTable> ratesControlFactory =
				new IPropertyControlFactory<StockAccount,RatesTable>() {
			@Override
			public IPropertyControl<StockAccount> createPropertyControl(Composite parent, ScalarPropertyAccessor<RatesTable,StockAccount> propertyAccessor) {
				return new RatesEditor(parent, propertyAccessor);
			}

			@Override
			public String formatValueForMessage(StockAccount extendableObject, ScalarPropertyAccessor<? extends RatesTable,StockAccount> propertyAccessor) {
				RatesTable ratesTable = propertyAccessor.getValue(extendableObject);
				if (ratesTable == null) {
					return "none";
				} else {
					return "rates table";
				}
			}

			@Override
			public String formatValueForTable(StockAccount extendableObject, ScalarPropertyAccessor<? extends RatesTable,StockAccount> propertyAccessor) {
				RatesTable ratesTable = propertyAccessor.getValue(extendableObject);
				if (ratesTable == null) {
					return "";
				} else {
					return "rates table";
				}
			}

			@Override
			public boolean isEditable() {
				// TODO Should this be set to allow control editing?
				return false;
			}

			@Override
			public Comparator<RatesTable> getComparator() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public RatesTable getDefaultValue() {
				return null;
			}
		};

		IReferenceControlFactory<StockAccount,StockAccount,IncomeExpenseAccount> dividendAccountControlFactory = new AccountControlFactory<StockAccount,StockAccount,IncomeExpenseAccount>() {
			@Override
			public IObjectKey getObjectKey(StockAccount parentObject) {
				return parentObject.dividendAccountKey;
			}
		};

		IReferenceControlFactory<StockAccount,StockAccount,IncomeExpenseAccount> returnOfCapitalAccountControlFactory = new AccountControlFactory<StockAccount,StockAccount,IncomeExpenseAccount>() {
			@Override
			public IObjectKey getObjectKey(StockAccount parentObject) {
				return parentObject.returnOfCapitalAccountKey;
			}
		};

		IReferenceControlFactory<StockAccount,StockAccount,IncomeExpenseAccount> withholdingTaxAccountControlFactory = new AccountControlFactory<StockAccount,StockAccount,IncomeExpenseAccount>() {
			@Override
			public IObjectKey getObjectKey(StockAccount parentObject) {
				return parentObject.withholdingTaxAccountKey;
			}
		};

		IReferenceControlFactory<StockAccount,StockAccount,IncomeExpenseAccount> commissionAccountControlFactory = new AccountControlFactory<StockAccount,StockAccount,IncomeExpenseAccount>() {
			@Override
			public IObjectKey getObjectKey(StockAccount parentObject) {
				return parentObject.commissionAccountKey;
			}
		};

		IReferenceControlFactory<StockAccount,StockAccount,IncomeExpenseAccount> tax1AccountControlFactory = new AccountControlFactory<StockAccount,StockAccount,IncomeExpenseAccount>() {
			@Override
			public IObjectKey getObjectKey(StockAccount parentObject) {
				return parentObject.tax1AccountKey;
			}
		};

		IReferenceControlFactory<StockAccount,StockAccount,IncomeExpenseAccount> tax2AccountControlFactory = new AccountControlFactory<StockAccount,StockAccount,IncomeExpenseAccount>() {
			@Override
			public IObjectKey getObjectKey(StockAccount parentObject) {
				return parentObject.tax2AccountKey;
			}
		};

		currencyAccessor             = propertySet.addProperty("currency",             Messages.StockAccountInfo_currency,             Currency.class,             2, 20, currencyControlFactory,             null);
		brokerageFirmAccessor        = propertySet.addProperty("brokerageFirm",        Messages.StockAccountInfo_brokerageFirm,        String.class,               5, 30, textControlFactory,                 null);
		accountNumberAccessor        = propertySet.addProperty("accountNumber",        Messages.StockAccountInfo_accountNumber,        String.class,               2, 30, textControlFactory,                 null);
		dividendAccountAccessor      = propertySet.addProperty("dividendAccount",      Messages.StockAccountInfo_dividendAccount,      IncomeExpenseAccount.class, 2, 80, dividendAccountControlFactory,      null);
		returnOfCapitalAccountAccessor=propertySet.addProperty("returnOfCapitalAccount",Messages.StockAccountInfo_returnOfCapitalAccount,IncomeExpenseAccount.class, 2, 80,returnOfCapitalAccountControlFactory, null);
		withholdingTaxAccountAccessor= propertySet.addProperty("withholdingTaxAccount",Messages.StockAccountInfo_withholdingTaxAccount,IncomeExpenseAccount.class, 2, 80, withholdingTaxAccountControlFactory,null);
		commissionAccountAccessor    = propertySet.addProperty("commissionAccount",    Messages.StockAccountInfo_commissionAccount,    IncomeExpenseAccount.class, 2, 80, commissionAccountControlFactory,    null);
		buyCommissionRatesAccessor   = propertySet.addProperty("buyCommissionRates",   Messages.StockAccountInfo_buyCommissionRates,   RatesTable.class,           1, 100,ratesControlFactory,                null);
		sellCommissionRatesAccessor  = propertySet.addProperty("sellCommissionRates",  Messages.StockAccountInfo_sellCommissionRates,  RatesTable.class,           1, 100,ratesControlFactory,                null);
		tax1NameAccessor             = propertySet.addProperty("tax1Name",             Messages.StockAccountInfo_tax1Name,             String.class,               2, 50, textControlFactory,                 null);
		tax1AccountAccessor          = propertySet.addProperty("tax1Account",          Messages.StockAccountInfo_tax1Account,          IncomeExpenseAccount.class, 2, 80, tax1AccountControlFactory,          null);
		tax1RatesAccessor            = propertySet.addProperty("tax1Rates",            Messages.StockAccountInfo_tax1Rates,            RatesTable.class,           1, 100,ratesControlFactory,                null);
		tax2NameAccessor             = propertySet.addProperty("tax2Name",             Messages.StockAccountInfo_tax2Name,             String.class,               2, 50, textControlFactory,                 null);
		tax2AccountAccessor          = propertySet.addProperty("tax2Account",          Messages.StockAccountInfo_tax2Account,          IncomeExpenseAccount.class, 2, 80, tax2AccountControlFactory,          null);
		tax2RatesAccessor            = propertySet.addProperty("tax2Rates",            Messages.StockAccountInfo_tax2Rates,            RatesTable.class,           1, 100,ratesControlFactory,                null);

		return propertySet;
	}

	public static ExtendablePropertySet<StockAccount> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<Currency,StockAccount> getCurrencyAccessor() {
		return currencyAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,StockAccount> getBrokerageFirmAccessor() {
		return brokerageFirmAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,StockAccount> getAccountNumberAccessor() {
		return accountNumberAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,StockAccount> getTax1NameAccessor() {
		return tax1NameAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,StockAccount> getTax2NameAccessor() {
		return tax2NameAccessor;
	}

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<IncomeExpenseAccount,StockAccount> getDividendAccountAccessor() {
		return dividendAccountAccessor;
	}

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<IncomeExpenseAccount,StockAccount> getReturnOfCapitalAccountAccessor() {
		return returnOfCapitalAccountAccessor;
	}

	public static ReferencePropertyAccessor<IncomeExpenseAccount,StockAccount> getWithholdingTaxAccountAccessor() {
		return withholdingTaxAccountAccessor;
	}

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<IncomeExpenseAccount,StockAccount> getCommissionAccountAccessor() {
		return commissionAccountAccessor;
	}

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<IncomeExpenseAccount,StockAccount> getTax1AccountAccessor() {
		return tax1AccountAccessor;
	}

	/**
	 * @return
	 */
	public static ReferencePropertyAccessor<IncomeExpenseAccount,StockAccount> getTax2AccountAccessor() {
		return tax2AccountAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<RatesTable,StockAccount> getBuyCommissionRatesAccessor() {
		return buyCommissionRatesAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<RatesTable,StockAccount> getSellCommissionRatesAccessor() {
		return sellCommissionRatesAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<RatesTable,StockAccount> getTax1RatesAccessor() {
		return tax1RatesAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<RatesTable,StockAccount> getTax2RatesAccessor() {
		return tax2RatesAccessor;
	}
}
