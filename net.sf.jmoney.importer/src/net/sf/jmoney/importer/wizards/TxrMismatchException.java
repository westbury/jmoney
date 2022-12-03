/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2022 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.importer.wizards;

import java.net.URL;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import txr.debug3x.TxrDebugView;


/**
 * This exception indicates that the TXR match failed. We handle this situation by showing
 * the mismatch in the TXR debugging view.
 *
 * @author Nigel Westbury
 *
 */
public class TxrMismatchException extends Exception {
	private static final long serialVersionUID = 1L;

	private URL resource;
	private String inputText;
	private String sourceDescription;
	
	public TxrMismatchException(URL resource, String inputText, String sourceDescription) {
		this.resource = resource;
		this.inputText = inputText;
		this.sourceDescription = sourceDescription;
	}

	public void showInDebugView(IWorkbenchWindow window) {
		MessageDialog dialog = new MessageDialog(
				window.getShell(),
				"Data Match Failure",
				null, // accept the default window icon
				"Data in clipboard does not appear to be copied from " + sourceDescription + ".",
				MessageDialog.ERROR,
				new String[] { "Debug", IDialogConstants.CANCEL_LABEL },
				1);
		int resultCode = dialog.open();
		if (resultCode == 0) {
			try {
				TxrDebugView view = (TxrDebugView)window.getActivePage().showView(TxrDebugView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);
				view.setTxrAndData(resource, inputText.split("\n"));
			} catch (PartInitException e) {
				throw new RuntimeException(e);
			}
		}		
	}

}
