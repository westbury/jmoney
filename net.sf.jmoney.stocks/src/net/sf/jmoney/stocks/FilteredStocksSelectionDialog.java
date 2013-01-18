/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2009 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.stocks;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.stocks.model.Stock;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.model.StockEntry;
import net.sf.jmoney.stocks.model.StockEntryInfo;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Shows a list of stock to the user with a text entry field for a string
 * pattern used to filter the list of stocks.
 */
public class FilteredStocksSelectionDialog extends FilteredItemsSelectionDialog {

	private static final String DIALOG_SETTINGS = "org.eclipse.ui.dialogs.FilteredStocksSelectionDialog"; //$NON-NLS-1$

	private static final String HIDE_STOCK_NO_LONGER_OWNED = "HideSoldStock"; //$NON-NLS-1$

	private StockAccount account;
	
	private HideSoldStockAction hideSoldStockAction;

	private StockInListLabelProvider stockInListLabelProvider;

	private StockInStatusLineLabelProvider stockInStatusLineLabelProvider;

	private boolean isHidingSoldStock;

	private class StockSummary {
		public long total = 0;
	}
	
	private Map<Stock, StockSummary> totals = new HashMap<Stock, StockSummary>();

	/**
	 * Creates a new instance of the class
	 * 
	 * @param shell
	 *            the parent shell
	 * @param stockAccount
	 *            the account in which we look to see the stock to be listed
	 */
	public FilteredStocksSelectionDialog(Shell shell, StockAccount stockAccount) {
		super(shell, true);

		this.account = stockAccount;
		
		setSelectionHistory(new StockSelectionHistory());

		setTitle("Show Stock Details");
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell,
				IStockHelpContextIds.SHOW_STOCK_DETAILS_DIALOG);

		stockInListLabelProvider = new StockInListLabelProvider();

		stockInStatusLineLabelProvider = new StockInStatusLineLabelProvider();

		setListLabelProvider(stockInListLabelProvider);
		setDetailsLabelProvider(stockInStatusLineLabelProvider);

		/*
		 * Obtain the set of stock that has ever been held in this account.
		 * Also determine the current balance for use in the filter and in
		 * the status line.
		 */
		for (Entry entry : account.getEntries()) {
			StockEntry entry2 = entry.getExtension(StockEntryInfo.getPropertySet(), false);
			if (entry2 != null) {
				if (entry2.getCommodity() instanceof Stock) {
					Stock stock = (Stock)entry2.getCommodity();
					StockSummary stockWrapper = totals.get(stock);
					if (stockWrapper == null) {
						stockWrapper = new StockSummary();
						totals.put(stock, stockWrapper);
					}
					
					stockWrapper.total += entry.getAmount();
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#getDialogSettings()
	 */
	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = StocksPlugin.getDefault()
				.getDialogSettings().getSection(DIALOG_SETTINGS);

		if (settings == null) {
			settings = StocksPlugin.getDefault().getDialogSettings()
					.addNewSection(DIALOG_SETTINGS);
		}

		return settings;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#storeDialog(org.eclipse.jface.dialogs.IDialogSettings)
	 */
	@Override
	protected void storeDialog(IDialogSettings settings) {
		super.storeDialog(settings);

		settings.put(HIDE_STOCK_NO_LONGER_OWNED, hideSoldStockAction.isChecked());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#restoreDialog(org.eclipse.jface.dialogs.IDialogSettings)
	 */
	@Override
	protected void restoreDialog(IDialogSettings settings) {
		super.restoreDialog(settings);

		boolean showDerived = settings.getBoolean(HIDE_STOCK_NO_LONGER_OWNED);
		hideSoldStockAction.setChecked(showDerived);
		this.isHidingSoldStock = showDerived;

		applyFilter();   // needed?????
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#fillViewMenu(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	protected void fillViewMenu(IMenuManager menuManager) {
		super.fillViewMenu(menuManager);

		hideSoldStockAction = new HideSoldStockAction();
		menuManager.add(hideSoldStockAction);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#createExtendedContentArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	/**
	 * This method is called to get the text on which the pattern matching is done.
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#getElementName(java.lang.Object)
	 */
	@Override
	public String getElementName(Object element) {
		Stock stock = (Stock) element;
		return stock.getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#validateItem(java.lang.Object)
	 */
	@Override
	protected IStatus validateItem(Object element) {
		return new Status(IStatus.OK, StocksPlugin.PLUGIN_ID, 0, "", null); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#createFilter()
	 */
	@Override
	protected ItemsFilter createFilter() {
		return new StockNoLongerOwnedFilter(isHidingSoldStock);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#getItemsComparator()
	 */
	@Override
	protected Comparator getItemsComparator() {
		return new Comparator() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see java.util.Comparator#compare(java.lang.Object,
			 *      java.lang.Object)
			 */
			public int compare(Object element1, Object element2) {
				Stock stock1 = (Stock) element1;
				Stock stock2 = (Stock) element2;
				
				String name1 = stock1.getName();
				String name2 = stock2.getName();
				int nameComparison = name1.compareToIgnoreCase(name2);
				if (nameComparison != 0) {
					return nameComparison;
				}

				String symbol1 = stock1.getSymbol();
				String symbol2 = stock2.getSymbol();
				return symbol1 == null
				? symbol2 == null ? 0 : 1
						: symbol2 == null ? -1 : symbol1.compareToIgnoreCase(symbol2);
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#fillContentProvider(org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.AbstractContentProvider,
	 *      org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter,
	 *      org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider,
			ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
			throws CoreException {

		for (Stock stock : totals.keySet()) {
			contentProvider.add(stock, itemsFilter);
		}

		if (progressMonitor != null)
			progressMonitor.done();

	}

	/**
	 * Sets flag on the StockNoLongerOwnedFilter instance
	 */
	private class HideSoldStockAction extends Action {

		/**
		 * Creates a new instance of the action.
		 */
		public HideSoldStockAction() {
			super(
					"Hide Stock No Longer Owned",
					IAction.AS_CHECK_BOX);
		}

		@Override
		public void run() {
			FilteredStocksSelectionDialog.this.isHidingSoldStock = isChecked();
			applyFilter();
		}
	}

	/**
	 * A label provider for Stock objects. This label provider is used when
	 * showing the list of stocks that match the pattern.
	 */
	private class StockInListLabelProvider extends LabelProvider implements	IStyledLabelProvider {

		WorkbenchLabelProvider provider = new WorkbenchLabelProvider();

		/**
		 * Creates a new instance of the class
		 */
		public StockInListLabelProvider() {
			super();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
		 */
		@Override
		public Image getImage(Object element) {
			return provider.getImage(element);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
		 */
		@Override
		public String getText(Object element) {
			Stock stock = (Stock) element;
			return stock.getName();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider#getStyledText(java.lang.Object)
		 */
		public StyledString getStyledText(Object element) {
			Stock stock = (Stock) element;
			// We seem to get a null element here after restricting selection to stock
			// that is still owned.  Not sure why.
			if (stock == null) return new StyledString("null value!!!");
			
			return new StyledString(stock.getName());
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.LabelProvider#dispose()
		 */
		@Override
		public void dispose() {
			provider.dispose();
			super.dispose();
		}
	}

	/**
	 * A label provider for Stock objects. This label provider is used when
	 * showing the details of the selected stock in the status line.
	 */
	private class StockInStatusLineLabelProvider extends StockInListLabelProvider {
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
		 */
		@Override
		public String getText(Object element) {
			Stock stock = (Stock) element;
			
			StringBuffer text = new StringBuffer();
			text.append(stock.getName());
			
			if (stock.getSymbol() != null) {
				text.append(" (").append(stock.getSymbol()).append(")");
			}
			
			if (stock.getNominalValue() != null) {
				text.append(" at ").append(stock.getNominalValue());
			}

			StockSummary wrapper = totals.get(stock);
			if (wrapper.total == 0) {
				text.append(" (stock is no longer owned)");
			} else {
				text.append(" (").append(stock.format(wrapper.total)).append(" shares in account)");
			}
			
			return text.toString();
		}
	}

	/**
	 * Filters resources using pattern and showDerived flag. It overrides
	 * ItemsFilter.
	 */
	protected class StockNoLongerOwnedFilter extends ItemsFilter {

		private boolean isFilterHidingSoldStock = false;

		public StockNoLongerOwnedFilter(boolean isHidingSoldStock) {
			super();
			this.isFilterHidingSoldStock = isHidingSoldStock;
		}

		public StockNoLongerOwnedFilter() {
			super();
			this.isFilterHidingSoldStock = isHidingSoldStock;
		}

		/**
		 * @param item
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#isConsistentItem(java.lang.Object)
		 */
		@Override
		public boolean isConsistentItem(Object item) {
			return true;
		}

		/**
		 * @param item
		 *            Must be instance of Stock, otherwise
		 *            <code>ClassCastException</code> will be returned.
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#matchItem(java.lang.Object)
		 */
		@Override
		public boolean matchItem(Object item) {
			Stock stock = (Stock) item;

			StockSummary wrapper = totals.get(stock);
			if (isFilterHidingSoldStock && wrapper.total == 0) {
				return false;
			}
			
			return matches(stock.getName());
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#isSubFilter(org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter)
		 */
		@Override
		public boolean isSubFilter(ItemsFilter filter) {
			if (!super.isSubFilter(filter))
				return false;
			if (filter instanceof StockNoLongerOwnedFilter)
				if (this.isFilterHidingSoldStock == ((StockNoLongerOwnedFilter) filter).isFilterHidingSoldStock)
					return true;
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#equalsFilter(org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter)
		 */
		@Override
		public boolean equalsFilter(ItemsFilter iFilter) {
			if (!super.equalsFilter(iFilter))
				return false;
			if (iFilter instanceof StockNoLongerOwnedFilter)
				if (this.isFilterHidingSoldStock == ((StockNoLongerOwnedFilter) iFilter).isFilterHidingSoldStock)
					return true;
			return false;
		}

		/**
		 * 
		 * @return true if filter is removing stock that is no longer
		 * 		owned in the account
		 */
		public boolean isHidingSoldStock() {
			return isFilterHidingSoldStock;
		}

	}

	/**
	 * <code>StockSelectionHistory</code> provides behavior specific to
	 * resources - storing and restoring <code>Stock</code>s state
	 * to/from XML (memento).
	 */
	private class StockSelectionHistory extends SelectionHistory {

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.SelectionHistory#restoreItemFromMemento(org.eclipse.ui.IMemento)
		 */
		@Override
		protected Object restoreItemFromMemento(IMemento stockMemento) {
			String symbol = stockMemento.getString("symbol");
			if (symbol != null) {
				for (Commodity commodity: account.getSession().getCommodityCollection()) {
					if (commodity instanceof Stock) {
						Stock stock = (Stock)commodity;
						if (stock.getSymbol().equals(symbol)) {
							return stock;
						}
					}
				}
			}
        		
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.SelectionHistory#storeItemToMemento(java.lang.Object,
		 *      org.eclipse.ui.IMemento)
		 */
		@Override
		protected void storeItemToMemento(Object item, IMemento element) {
			Stock stock = (Stock) item;
			element.putString("symbol", stock.getSymbol());
		}
	}
}
