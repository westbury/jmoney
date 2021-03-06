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

package net.sf.jmoney.stocks.pages;

import org.eclipse.core.commands.IHandler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.handlers.IHandlerService;

import net.sf.jmoney.IBookkeepingPageFactory;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.pages.entries.AccountEntriesEditor;
import net.sf.jmoney.pages.entries.EntriesFilter;
import net.sf.jmoney.stocks.ShowStockDetailsHandler;
import net.sf.jmoney.stocks.model.Stock;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.views.AccountEditor;
import net.sf.jmoney.views.AccountEditorInput;

/**
 * @author Nigel Westbury
 */
public class StockEntriesPage implements IBookkeepingPageFactory {

	@Override
	public void createPages(AccountEditor editor, IEditorInput input,
			IMemento memento) throws PartInitException {
		// TODO If this stock account does not directly hold cash then we should not include this page. 
		IEditorPart entriesEditor = new AccountEntriesEditor((parent, account, filter, toolkit, handlerService) -> this.createEntriesEditor(parent, (StockAccount)account, filter, toolkit, handlerService));
		editor.addPage(entriesEditor, "Entries");

		IEditorPart balancesEditor = new StockBalancesEditor(editor);
		editor.addPage(balancesEditor, "Balances");

		IEditorPart gainsEditor = new StockGainsEditor(editor);
		editor.addPage(gainsEditor, "Gains & Losses");

		AccountEditorInput input2 = (AccountEditorInput)input;
		IDataManagerForAccounts sessionManager = (IDataManagerForAccounts)editor.getSite().getPage().getInput();

		/*
		 * In addition to the main page that shows the currency balance, a page may be
		 * created to show activity related to a specific stock held in the account and
		 * the running balance of the number of the stock held in the account.
		 */
		if (memento != null) {
			for (IMemento stockMemento : memento.getChildren("stock")) {
				String symbol = stockMemento.getString("symbol");
				if (symbol != null) {
					for (Commodity commodity: sessionManager.getSession().getCommodityCollection()) {
						if (commodity instanceof Stock) {
							Stock stock = (Stock)commodity;
							if (stock.getSymbol().equals(symbol)) {
								IEditorPart stockDetailsEditor = new StockDetailsEditor(stock);
								editor.addPage(stockDetailsEditor, stock.getName());
							}
						}
					}
				}
			}
		}

		// Get the handler service and pass it on so that handlers can be activated as appropriate
		IHandlerService handlerService = editor.getSite().getService(IHandlerService.class);

		IHandler handler = new ShowStockDetailsHandler(editor);
		handlerService.activateHandler("net.sf.jmoney.stock.showStockDetails", handler);


	}

	public Control createEntriesEditor(Composite parent, StockAccount account, EntriesFilter filter, FormToolkit toolkit, IHandlerService handlerService) {
		StockEntriesSection fEntriesSection = new StockEntriesSection(parent, account, account, toolkit, handlerService);
		return fEntriesSection.getSection();
	}
}