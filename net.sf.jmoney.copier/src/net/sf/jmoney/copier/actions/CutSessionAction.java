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

package net.sf.jmoney.copier.actions;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.copier.CopierPlugin;
import net.sf.jmoney.model2.DatastoreManager;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.WorkbenchException;

/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
public class CutSessionAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	/**
	 * The constructor.
	 */
	public CutSessionAction() {
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		DatastoreManager sessionManager = JMoneyPlugin.getDefault().getSessionManager();
		
		if (sessionManager == null) {
			MessageDialog waitDialog =
				new MessageDialog(
						window.getShell(), 
						"Menu item unavailable", 
						null, // accept the default window icon
						"No session is open.  "
						+ "This action is used to copy session data from one session to another.  "
						+ "You must first use this action to save the contents of the current session.  "
						+ "You must then open another session and then select the 'Paste Contents' action.  "
						+ "The contents of the session will then be copied into the new session.",
						MessageDialog.ERROR, 
						new String[] { IDialogConstants.OK_LABEL }, 0);
			waitDialog.open();
			return;
		}

		// We call canClose now so that we are sure
		// that we can later close the session without
		// further user input.  If the user does not
		// provide at this time all the information required
		// to close the session then we terminate the operation
		// now.
		if (!sessionManager.canClose(window)) {
			return;
		}
		
		// Save the session in a static location.
		// The session is left open so that we can
		// later read the data from it.
		CopierPlugin.setSessionManager(sessionManager);
		
		/*
		 * Close this window but leave the session open.
		 * 
		 * This ensures that the session cannot be closed before
		 * it is pasted into the new location.
		 */ 
		// TODO: There is a problem with this.  If the session is
		// never pasted then it is never closed.  Better may be to
		// leave the window open, requiring the user to create a
		// target session in another window, and giving an error if
		// a paste is done after the source session was closed.
		try {
			window.getActivePage().close();
			window.openPage(null);
		} catch (WorkbenchException e) {
			// TODO: Uncomment this when this becomes a handler
//			throw new ExecutionException("Workbench exception occured while closing window.", e); //$NON-NLS-1$
		}
	}

	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}