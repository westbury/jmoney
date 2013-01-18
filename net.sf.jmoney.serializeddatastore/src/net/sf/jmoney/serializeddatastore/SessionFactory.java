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

package net.sf.jmoney.serializeddatastore;

import java.io.File;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.serializeddatastore.handlers.OpenSessionException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchWindow;

public class SessionFactory implements IElementFactory {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IElementFactory#createElement(org.eclipse.ui.IMemento)
	 */
	public IAdaptable createElement(IMemento memento) {
		String fileFormatId = memento.getString("fileFormatId"); //$NON-NLS-1$
		String fileName = memento.getString("fileName"); //$NON-NLS-1$
		if (fileFormatId != null && fileName != null) {
			IFileDatastore fileDatastore = SerializedDatastorePlugin
					.getFileDatastoreImplementation(fileFormatId);
			File sessionFile = new File(fileName);

			// TODO this should be silent, simply returning null if the
			// element can not be recreated.
			IWorkbenchWindow window = JMoneyPlugin.getDefault().getWorkbench()
					.getActiveWorkbenchWindow();
			SessionManager sessionManager = new SessionManager(fileFormatId,
					fileDatastore, sessionFile);
			// Update the title
			String productName = Platform.getProduct().getName();
			window.getShell().setText(
					productName + " - " + sessionManager.getBriefDescription());
			// Read the session from file. If the read fails, we do nothing,
			// which will have the effect of starting JMoney with no open
			// session. If the read fails then the readSession method is
			// responsible for displaying an appropriate message to the user.
			try {
				boolean isGoodFileRead = fileDatastore.readSession(sessionFile,
						sessionManager, window);
				if (isGoodFileRead) {
					return sessionManager;
				} else {
					/*
					 * A false return value indicates the operation was canceled
					 * by the user. In that case, we return null which will
					 * simply mean the session is not restored.
					 */
					return null;
				}
			} catch (OpenSessionException e) {
				/*
				 * This exception indicates the element could not be re-created.
				 * Perhaps the file has been deleted, renamed, or moved.
				 */
				return null;
			}
		} else {
			/*
			 * No file name is set. This should not happen. Even if the
			 * workbench was last closed with a session that had never been
			 * saved, the user would have been prompted to save the session when
			 * the workbench closed, at which point the user either had to enter
			 * a file name or cancel the save. If the save was canceled then no
			 * memento should have been saved.
			 * 
			 * In any case we certainly don't want to restore anything.
			 */
			// TODO decide if this is really right. Surely we wouldn't have
			// saved anything in the memento if the last session had no file
			// name because it had never been saved.
			return null;
		}
	}
}