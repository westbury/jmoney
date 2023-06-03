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

package net.sf.jmoney.updater.actions;

import java.text.MessageFormat;
import java.util.Collection;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionManagerForAccounts;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.pages.StockEntryFacadeOriginal;

/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
public class RunUpdaterAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	/**
	 * The constructor.
	 */
	public RunUpdaterAction() {
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	@Override
	public void run(IAction action) {
		IDataManagerForAccounts sessionManager = JMoneyPlugin.getDefault().getSessionManager2();

		if (sessionManager == null) {
			MessageDialog waitDialog =
				new MessageDialog(
						window.getShell(), 
						"Menu item unavailable", 
						null, // accept the default window icon
						"No session is open.  "
						+ "This action is used to run a one-time 'updater' against a datastore.",
						MessageDialog.ERROR, 
						new String[] { IDialogConstants.OK_LABEL }, 0);
			waitDialog.open();
			return;
		}

		ObjectCollection<Account> accounts = sessionManager.getSession().getAccountCollection();
		for (Account account : accounts) {
			if (account instanceof StockAccount) {
				System.out.println("processing " + account.getName());
				StockAccount stockAccountOutsideTransaction = (StockAccount)account;
				
				try {
					/*
					 * Create a transaction to be used to import the entries.  This allows the entries to
					 * be more efficiently written to the back-end datastore and it also groups
					 * the entire import as a single change for undo/redo purposes.
					 */
					TransactionManagerForAccounts transactionManager = new TransactionManagerForAccounts(sessionManager);

					StockAccount stockAccount = transactionManager.getCopyInTransaction(stockAccountOutsideTransaction);

					if (stockAccount.getName().equals("London Stock Broker") || stockAccount.getName().equals("London Stock Broker (ISA)")) {
						
						// Here we have to scan the child accounts to find the cash entries.
						
//						for (CapitalAccount cashAccount : stockAccount.getSubAccountCollection()) {
//							Collection<Entry> entries = cashAccount.getEntries();
						Collection<Entry> entries = stockAccount.getEntries();
							for (Entry entry : entries) {

								if (needToProcess(entry.getTransaction())) {	
									StockEntryFacadeOriginal facade = new StockEntryFacadeOriginal(entry, stockAccount);

									// check for entries with no type
									boolean is = false;
									boolean isNot = false;
									for (Entry eachEntry : entry.getTransaction().getEntryCollection()) {
										String type = eachEntry.getType();
										is |= type == null || type.isEmpty();
										isNot |= !(type == null || type.isEmpty());
									}
									if (isNot) {
										if (is) {
											System.out.println("partially set " + entry.getTransaction().getDate());
										} else {
											System.out.println("not set at all " + entry.getTransaction().getDate());
										}
									}
								}
							}
//						}
					} else if (false) {
						/*
						 * We want only cash entries, not stock entries.
						 */

						Collection<Entry> entries = stockAccount.getEntries();
						for (Entry entry : entries) {
							if (entry.getCommodityInternal() == stockAccount.getCurrency() && needToProcess(entry.getTransaction())) {	

								StockEntryFacadeOriginal facade = new StockEntryFacadeOriginal(entry, stockAccount);

								// check for entries with no type
								boolean is = false;
								boolean isNot = false;
								for (Entry eachEntry : entry.getTransaction().getEntryCollection()) {
									String type = eachEntry.getType();
									is |= type == null || type.isEmpty();
									isNot |= !(type == null || type.isEmpty());
								}
								if (isNot) {
									if (is) {
										System.out.println("partially set " + entry.getTransaction().getDate());
									} else {
										System.out.println("not set at all " + entry.getTransaction().getDate());
									}
								}
							}
						}
					}

					if (transactionManager.hasChanges()) {
						String transactionDescription = MessageFormat.format("Update {0}", stockAccount.getName());
						transactionManager.commit(transactionDescription);

						StringBuffer combined = new StringBuffer()
								.append(stockAccount.getName())
								.append(" was successfully updated.");
						MessageDialog.openInformation(window.getShell(), "Account Updated", combined.toString());
					} else {
						System.out.println("No changes for " + stockAccount.getName());
//						MessageDialog.openWarning(window.getShell(), "Account not updated",
//								MessageFormat.format(
//										"{0} was not updated because there was nothing to update.",
//										stockAccount.getName()));
					}
				} catch (Exception e) {
					MessageDialog.openError(window.getShell(), "Unable to update account", e.getLocalizedMessage());
				}
			}
		}
	}

	private boolean needToProcess(Transaction transaction) {
		boolean had = false;
		boolean hadNot = false;
		for (Entry eachEntry : transaction.getEntryCollection()) {
			String type = eachEntry.getType();
			hadNot |= type == null || type.isEmpty();
			had |= !(type == null || type.isEmpty());
		}

		if (had && hadNot) {
			System.out.println("account had a mixture of set and not set - needs investigation");
		}
			
		return !had;
	}

	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	@Override
	public void dispose() {
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}