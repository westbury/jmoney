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

package net.sf.jmoney.handlers;

import java.util.Comparator;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.IDataManagerForAccounts;

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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Shows a list of accounts to the user with a text entry field for a string
 * pattern used to filter the list of accounts.
 */
public class FilteredAccountsSelectionDialog extends FilteredItemsSelectionDialog {

	private static final String DIALOG_SETTINGS = "org.eclipse.ui.dialogs.FilteredAccountsSelectionDialog"; //$NON-NLS-1$

	private static final String SHOW_CATEGORIES = "ShowCategories"; //$NON-NLS-1$

	private IDataManagerForAccounts sessionManager;
	
	private ShowCategoriesAction showCategoriesAction;

	private AccountInListLabelProvider accountInListLabelProvider;

	private AccountInStatusLineLabelProvider accountInStatusLineLabelProvider;

	private boolean isShowingCategories;

	/**
	 * Creates a new instance of the class
	 * 
	 * @param shell
	 *            the parent shell
	 * @param sessionManager
	 *            the session from which accounts are to be selected
	 */
	public FilteredAccountsSelectionDialog(Shell shell, IDataManagerForAccounts sessionManager) {
		super(shell, true);

		this.sessionManager = sessionManager;
		
		setSelectionHistory(new AccountSelectionHistory());

		setTitle("Open Account");
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell,
//				IStockHelpContextIds.SHOW_STOCK_DETAILS_DIALOG);

		accountInListLabelProvider = new AccountInListLabelProvider();

		accountInStatusLineLabelProvider = new AccountInStatusLineLabelProvider();

		setListLabelProvider(accountInListLabelProvider);
		setDetailsLabelProvider(accountInStatusLineLabelProvider);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#getDialogSettings()
	 */
	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = JMoneyPlugin.getDefault()
				.getDialogSettings().getSection(DIALOG_SETTINGS);

		if (settings == null) {
			settings = JMoneyPlugin.getDefault().getDialogSettings()
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

		settings.put(SHOW_CATEGORIES, showCategoriesAction.isChecked());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#restoreDialog(org.eclipse.jface.dialogs.IDialogSettings)
	 */
	@Override
	protected void restoreDialog(IDialogSettings settings) {
		super.restoreDialog(settings);

		boolean showDerived = settings.getBoolean(SHOW_CATEGORIES);
		showCategoriesAction.setChecked(showDerived);
		this.isShowingCategories = showDerived;

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

		showCategoriesAction = new ShowCategoriesAction();
		menuManager.add(showCategoriesAction);
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
		Account account = (Account) element;
		return account.getFullAccountName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#validateItem(java.lang.Object)
	 */
	@Override
	protected IStatus validateItem(Object element) {
		return new Status(IStatus.OK, JMoneyPlugin.PLUGIN_ID, 0, "", null); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#createFilter()
	 */
	@Override
	protected ItemsFilter createFilter() {
		return new ShowAccountTypesFilter(isShowingCategories);
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
			@Override
			public int compare(Object element1, Object element2) {
				Account account1 = (Account) element1;
				Account account2 = (Account) element2;
				
				String name1 = account1.getName();
				String name2 = account2.getName();
				int nameComparison = name1.compareToIgnoreCase(name2);
				if (nameComparison != 0) {
					return nameComparison;
				}

				String fullName1 = account1.getFullAccountName();
				String fullName2 = account2.getFullAccountName();
				return fullName1 == null
				? fullName2 == null ? 0 : 1
						: fullName2 == null ? -1 : fullName1.compareToIgnoreCase(fullName2);
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
	protected void fillContentProvider(final AbstractContentProvider contentProvider,
			final ItemsFilter itemsFilter, final IProgressMonitor progressMonitor)
			throws CoreException {
		/*
		 * This is done in a job so will not be on the UI thread.  Access to
		 * the model must be on the UI thread.  Must be done synchronously, though,
		 * because the job will use the contents present when this method returns.
		 */
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				for (Account account : sessionManager.getSession().getAccountCollection()) {
					contentProvider.add(account, itemsFilter);
					addSubAccounts(contentProvider, itemsFilter, account);
				}

				if (progressMonitor != null)
					progressMonitor.done();
			}
		});
	}

	private void addSubAccounts(AbstractContentProvider contentProvider,
			ItemsFilter itemsFilter, Account parentAccount) {
		for (Account account : parentAccount.getAllSubAccounts()) {
			contentProvider.add(account, itemsFilter);
			addSubAccounts(contentProvider, itemsFilter, account);
		}
	}

	/**
	 * Sets the derived flag on the ResourceFilter instance
	 */
	private class ShowCategoriesAction extends Action {

		/**
		 * Creates a new instance of the action.
		 */
		public ShowCategoriesAction() {
			super(
					"Show Categories",
					IAction.AS_CHECK_BOX);
		}

		@Override
		public void run() {
			FilteredAccountsSelectionDialog.this.isShowingCategories = isChecked();
			applyFilter();
		}
	}

	/**
	 * A label provider for Account objects. This label provider is used when
	 * showing the list of accounts that match the pattern.
	 */
	private class AccountInListLabelProvider extends LabelProvider implements	IStyledLabelProvider {

		WorkbenchLabelProvider provider = new WorkbenchLabelProvider();

		/**
		 * Creates a new instance of the class
		 */
		public AccountInListLabelProvider() {
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
			Account account = (Account) element;
			return account.getName();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider#getStyledText(java.lang.Object)
		 */
		@Override
		public StyledString getStyledText(Object element) {
			Account account = (Account) element;
			// We seem to get a null element here after restricting selection to stock
			// that is still owned.  Not sure why.
			if (account == null) return new StyledString("null value!!!");
			
			return new StyledString(account.getName());
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
	 * A label provider for Account objects. This label provider is used when
	 * showing the details of the selected account in the status line.
	 */
	private class AccountInStatusLineLabelProvider extends AccountInListLabelProvider {
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
		 */
		@Override
		public String getText(Object element) {
			Account account = (Account) element;
			
			StringBuffer text = new StringBuffer();
			text.append(account.getFullAccountName());
			
			return text.toString();
		}
	}

	/**
	 * Filters resources using pattern and showDerived flag. It overrides
	 * ItemsFilter.
	 */
	protected class ShowAccountTypesFilter extends ItemsFilter {

		private boolean isShowingCategories = false;

		public ShowAccountTypesFilter(boolean isShowingCategories) {
			super();
			this.isShowingCategories = isShowingCategories;
		}

		/**
		 * Creates new ResourceFilter instance
		 */
		public ShowAccountTypesFilter() {
			super();
			this.isShowingCategories = FilteredAccountsSelectionDialog.this.isShowingCategories;
		}

		/**
		 * @param item
		 *            Must be instance of IResource, otherwise
		 *            <code>false</code> will be returned.
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#isConsistentItem(java.lang.Object)
		 */
		@Override
		public boolean isConsistentItem(Object item) {
			return true;
		}

		/**
		 * @param item
		 *            Must be instance of Account, otherwise
		 *            <code>ClassCastException</code> will be returned.
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#matchItem(java.lang.Object)
		 */
		@Override
		public boolean matchItem(Object item) {
			Account account = (Account) item;

//			if (isFilterHidingSoldStock && wrapper.total == 0) {
//				return false;
//			}
			
			return matches(account.getName());
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
			if (filter instanceof ShowAccountTypesFilter)
				if (this.isShowingCategories == ((ShowAccountTypesFilter) filter).isShowingCategories)
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
			if (iFilter instanceof ShowAccountTypesFilter)
				if (this.isShowingCategories == ((ShowAccountTypesFilter) iFilter).isShowingCategories)
					return true;
			return false;
		}

		/**
		 * 
		 * @return true if filter is showing category accounts
		 */
		public boolean isShowingCategories() {
			return isShowingCategories;
		}

	}

	/**
	 * <code>AccountSelectionHistory</code> provides behavior specific to
	 * resources - storing and restoring <code>Account</code>s state
	 * to/from XML (memento).
	 */
	private class AccountSelectionHistory extends SelectionHistory {

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.SelectionHistory#restoreItemFromMemento(org.eclipse.ui.IMemento)
		 */
		@Override
		protected Object restoreItemFromMemento(IMemento accountMemento) {
			String accountName = accountMemento.getString("name");
			if (accountName != null) {
				return sessionManager.getSession().getAccountByFullName(accountName);
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
			Account account = (Account) item;
			element.putString("name", account.getFullAccountName());
		}
	}
}
