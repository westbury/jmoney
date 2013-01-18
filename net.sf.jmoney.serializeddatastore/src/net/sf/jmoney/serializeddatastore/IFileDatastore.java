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

import net.sf.jmoney.serializeddatastore.handlers.OpenSessionException;

import org.eclipse.ui.IWorkbenchWindow;

/**
 * Interface that must be implemented by all classes referenced by the 'class'
 * attribute of the 'file-format' element in the 'net.sf.serializeddatastore.filestores'
 * extension point.
 */
public interface IFileDatastore {

	/**
	 * Read the session data from file. The session is set as the open session
	 * in the given session manager.
	 * <P>
	 * The opened session is set as the current open JMoney session. If no
	 * session can be opened then an appropriate message is displayed to the
	 * user and the previous session, if any, is left open.
	 * <P>
	 * If this method returns false then any previous session will be left open.
	 * The caller will not display any error message. This method must display
	 * an appropriate error message if the file cannot be read.
	 * 
	 * @return true if the file was successfully read and the session was set in
	 *         the given session manager, false if the user canceled the
	 *         operation
	 */
	boolean readSession(File sessionFile, SessionManager sessionManager, IWorkbenchWindow window) throws OpenSessionException;
    
    /**
     * Write data to a file
     */
    void writeSession(SessionManager sessionManager, File sessionFile, IWorkbenchWindow window);
}
