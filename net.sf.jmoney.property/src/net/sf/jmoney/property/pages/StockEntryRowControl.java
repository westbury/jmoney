package net.sf.jmoney.property.pages;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import net.sf.jmoney.entrytable.BaseEntryRowControl;
import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.FocusCellTracker;
import net.sf.jmoney.entrytable.InvalidUserEntryException;
import net.sf.jmoney.entrytable.RowSelectionTracker;
import net.sf.jmoney.entrytable.VirtualRowTable;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.property.model.RealPropertyAccount;

import org.eclipse.swt.widgets.Composite;

public class StockEntryRowControl extends BaseEntryRowControl<StockEntryData, StockEntryRowControl> {

	public enum TransactionType {
		Buy,
		Sell,
		Other
	}

	private ArrayList<ITransactionTypeChangeListener> transactionTypeChangeListeners = new ArrayList<ITransactionTypeChangeListener>();

	/**
	 * The share price currently shown to the user, or null if
	 * this is a new entry and no share price has been entered.
	 * <P>
	 * This is saved here (not fetched each time) because we often
	 * update other fields when fields are updated in order to keep
	 * the transaction balanced.  We need to know the previous price,
	 * not the price that would have balanced the transaction, when
	 * we do these calculations.
	 */
	private BigDecimal agreedPrice;

	private List<IPropertyChangeListener<BigDecimal>> agreedPriceListeners = new ArrayList<IPropertyChangeListener<BigDecimal>>();

	public StockEntryRowControl(final Composite parent, int style, VirtualRowTable rowTable, Block<StockEntryData, ? super StockEntryRowControl> rootBlock, final RowSelectionTracker selectionTracker, final FocusCellTracker focusCellTracker) {
		super(parent, style, rowTable, rootBlock, selectionTracker, focusCellTracker);
		init(this, this, rootBlock);
	}

	@Override
	public void setInput(StockEntryData inputEntryData) {
		if (inputEntryData.getTransactionType() == null) {
			/*
			 * This is a new transaction so start with everything fluid.
			 */
		} else {
			switch (inputEntryData.getTransactionType()) {
			case Buy:
			case Sell:
				break;
			case Other:
				break;
			}
		}

		if (uncommittedEntryData.isPurchaseOrSale()) {
			agreedPrice = uncommittedEntryData.calculatePrice();
		} else {
			agreedPrice = null;
		}

		this.addTransactionTypeChangeListener(new ITransactionTypeChangeListener() {
			public void transactionTypeChanged() {
				if (uncommittedEntryData.isPurchaseOrSale()) {
					agreedPrice = uncommittedEntryData.calculatePrice();
				} else {
					agreedPrice = null;
				}
			}
		});

		/*
		 * This must be called after we have set our own stuff up.  The reason
		 * being that this call loads controls (such as the stock price control).
		 * These controls will not load correctly if this object is not set up.
		 */
		super.setInput(inputEntryData);
	}

	/*
	 * Notify listeners when the transaction type (purchase, sale, dividend etc.)
	 * changes.
	 */
	public void fireTransactionTypeChange() {
		for (ITransactionTypeChangeListener listener: transactionTypeChangeListeners) {
			listener.transactionTypeChanged();
		}
	}

	public void addTransactionTypeChangeListener(ITransactionTypeChangeListener listener) {
		transactionTypeChangeListeners.add(listener);
	}

	@Override
	protected StockEntryData createUncommittedEntryData(
			Entry entryInTransaction, TransactionManager transactionManager) {
		StockEntryData entryData = new StockEntryData(entryInTransaction, transactionManager);
		return entryData;
	}

	@Override
	protected StockEntryRowControl getThis() {
		return this;
	}

	@Override
	public void amountChanged() {
		RealPropertyAccount account = (RealPropertyAccount)getUncommittedEntryData().getEntry().getAccount();

		Entry entry = uncommittedEntryData.getEntry();

		TransactionType transactionType = uncommittedEntryData.getTransactionType();
		if (transactionType == null) {
			/*
			 * The user has not yet entered enough information into the transaction
			 * to guess the transaction type.  In particular, the user has not selected
			 * the transaction type.
			 * 
			 * We have already set the net amount to be no longer fluid.  There is nothing to do in this case.
			 */
		} else {
			switch (transactionType) {
			case Buy:
			case Sell:
				break;
			case Other:
				// If there are two entries in the transaction and
				// if both entries have accounts in the same currency or
				// one or other account is not known or one or other account
				// is a multi-currency account then we set the amount in
				// the other entry to be the same but opposite signed amount.
				if (entry.getTransaction().hasTwoEntries()) {
					Entry otherEntry = entry.getTransaction().getOther(entry);
					Commodity commodity1 = entry.getCommodityInternal();
					Commodity commodity2 = otherEntry.getCommodityInternal();
					if (commodity1 == null || commodity2 == null || commodity1.equals(commodity2)) {
						otherEntry.setAmount(-entry.getAmount());
					}
				}
				break;
			}
		}
	}

	@Override
	protected void specificValidation() throws InvalidUserEntryException {
		// TODO: We should remove this method and call the EntryData method directly.
		uncommittedEntryData.specificValidation();
	}

	public BigDecimal getAgreedPrice() {
		return agreedPrice;
	}

	public void setAgreedPrice(BigDecimal agreedPrice) {
		if (!agreedPrice.equals(this.agreedPrice)) {
			this.agreedPrice = agreedPrice;
			for (IPropertyChangeListener<BigDecimal> listener : agreedPriceListeners) {
				listener.propertyChanged(agreedPrice);
			}
		}
	}

	public void addStockPriceChangeListener(
			IPropertyChangeListener<BigDecimal> listener) {
		agreedPriceListeners.add(listener);

	}
}