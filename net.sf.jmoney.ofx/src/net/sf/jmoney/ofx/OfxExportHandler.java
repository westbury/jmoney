/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2021 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.ofx;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.ofx.model.OfxEntryInfo;
import net.sf.jmoney.ofx.resources.Messages;
import net.sf.jmoney.ofx.wizards.OfxExportWizard;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.pages.StockDividendFacade;
import net.sf.jmoney.stocks.pages.StockEntryFacade;
import net.sf.jmoney.views.AccountEditor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.IExportWizard;

import com.webcohesion.ofx4j.io.v2.OFXV2Writer;

public class OfxExportHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IEditorPart editor = HandlerUtil.getActiveEditorChecked(event);
//		IWorkbench workbench = HandlerUtil.getActiveWorkbenchChecked(event);

		AccountEditor accountEditor = (AccountEditor)editor;
		Account account = accountEditor.getAccount();

		if (account instanceof StockAccount) {
			IExportWizard wizard = new OfxExportWizard();
			wizard.init(window.getWorkbench(), new StructuredSelection(account));
			WizardDialog dialog = new WizardDialog(shell, wizard);
			dialog.setPageSize(600, 300);
			dialog.open();
		}

		return null;
	}
}