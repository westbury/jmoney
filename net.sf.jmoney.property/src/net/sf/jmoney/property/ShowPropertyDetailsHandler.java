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

package net.sf.jmoney.property;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.property.model.RealProperty;
import net.sf.jmoney.property.model.RealPropertyAccount;
import net.sf.jmoney.property.pages.AssetDetailsEditor;
import net.sf.jmoney.views.AccountEditor;
import net.sf.jmoney.views.AccountEditorInput;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

public class ShowPropertyDetailsHandler extends AbstractHandler {
	
	private AccountEditor editor;

	public ShowPropertyDetailsHandler(AccountEditor editor) {
		this.editor = editor;
	}
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);

        AccountEditorInput cInput = (AccountEditorInput)editor.getEditorInput();
        DatastoreManager sessionManager = (DatastoreManager)editor.getSite().getPage().getInput();
        Account account = sessionManager.getSession().getAccountByFullName(cInput.getFullAccountName());
		RealPropertyAccount stockAccount = (RealPropertyAccount)account;

		// TODO: complete this
		
		return null;
	}

	public static void showRealPropertyDetails(AccountEditor editor, RealProperty property) throws PartInitException {
		/*
		 * This may not be the best way.  Inputs are not set for the editors within
		 * the multi-page editor, so they all match the input for the containing editor.
		 * We then look through and test the class implementation.
		 */
		IEditorPart realPropertyDetailsEditor = null;
		IEditorPart [] editors = editor.findEditors(editor.getEditorInput());
		for (IEditorPart childEditor : editors) {
			if (childEditor instanceof AssetDetailsEditor) {
				AssetDetailsEditor eachEditor = (AssetDetailsEditor) childEditor;
				if (eachEditor.getAsset() == property) {
					realPropertyDetailsEditor = eachEditor;
					break;
				}
			}
		}
		if (realPropertyDetailsEditor == null) {
			realPropertyDetailsEditor = new AssetDetailsEditor(property);
			editor.addPage(realPropertyDetailsEditor, property.getName());
		}

		editor.setActiveEditor(realPropertyDetailsEditor);
	}
}