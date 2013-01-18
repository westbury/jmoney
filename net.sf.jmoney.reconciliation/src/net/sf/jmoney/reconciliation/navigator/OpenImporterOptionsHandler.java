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

package net.sf.jmoney.reconciliation.navigator;

import net.sf.jmoney.importer.model.PatternMatcherAccount;
import net.sf.jmoney.importer.model.PatternMatcherAccountInfo;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.reconciliation.reconcilePage.ImportOptionsDialog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class OpenImporterOptionsHandler extends AbstractHandler {
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);

        if (selection instanceof IStructuredSelection) {
        	IStructuredSelection structuredSelection = (IStructuredSelection)selection;
        	if (structuredSelection.size() == 1) {
        		Object element = structuredSelection.getFirstElement();
        		if (element instanceof CapitalAccount) {
        			CapitalAccount account = (CapitalAccount)element;
        			PatternMatcherAccount account2 = account.getExtension(PatternMatcherAccountInfo.getPropertySet(), true);
    				ImportOptionsDialog messageBox = 
    					new ImportOptionsDialog(shell, account2);
    				messageBox.open();
        		}
        	}
        }
		
		return null;
	}
}