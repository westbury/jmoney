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

package net.sf.jmoney.jdbcdatastore;

import java.sql.SQLException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 *
 * @author Nigel Westbury
 */
public class JDBCDatastorePlugin extends AbstractUIPlugin {

	public static final boolean DEBUG = "true".equalsIgnoreCase(Platform.getDebugOption("net.sf.jmoney.jdbcdatastore/debug"));

	//The shared instance.
	private static JDBCDatastorePlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
	
	/**
	 * The constructor.
	 */
	public JDBCDatastorePlugin() {
		super();
		plugin = this;
		try {
			resourceBundle = ResourceBundle.getBundle("net.sf.jmoney.jdbcdatastore.Language");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}
	
	/**
	 * This method is called upon plug-in activation
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}
	
	/**
	 * This method is called when the plug-in is stopped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
	}
	
	/**
	 * Returns the shared instance.
	 */
	public static JDBCDatastorePlugin getDefault() {
		return plugin;
	}
	
	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = JDBCDatastorePlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}
	
	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	
	/**
	 * @param window
	 * @return
	 */
	public SessionManager readSession(IWorkbenchWindow window) {
		SessionManager result = null;

    	// The following lines cannot return a null value because if
    	// no value is set then the default value set in
    	// the above initializeDefaultPreferences method will be returned.
		String driver = getPreferenceStore().getString("driver");
		String subprotocol = getPreferenceStore().getString("subProtocol");
		String subprotocolData = getPreferenceStore().getString("subProtocolData");
		
		String url = "jdbc:" + subprotocol + ":" + subprotocolData;
		
		String user = getPreferenceStore().getString("user");
		String password = getPreferenceStore().getString("password");
		
		if (getPreferenceStore().getBoolean("promptEachTime")) {
			// TODO: Put up a dialog box so the user can change
			// the connection options for this connection only.
		}
		
		try {
			Class.forName(driver).newInstance();
		} catch (InstantiationException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IllegalAccessException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (ClassNotFoundException e2) {
			String title = JDBCDatastorePlugin.getResourceString("errorTitle");
			String message = JDBCDatastorePlugin.getResourceString("driverNotFound");
			MessageDialog waitDialog =
				new MessageDialog(
						window.getShell(), 
						title, 
						null, // accept the default window icon
						message, 
						MessageDialog.ERROR, 
						new String[] { IDialogConstants.OK_LABEL }, 0);
			waitDialog.open();
			return null;
		}
		
		try {
			result = new SessionManager(url, user, password);
		} catch (SQLException e3) {
			if (e3.getSQLState().equals("08000")) {
				// A connection error which means the database server is probably not running.
				String title = JDBCDatastorePlugin.getResourceString("errorTitle");
				String message = JDBCDatastorePlugin.getResourceString("connectionFailed") + e3.getMessage() + "  Check that the database server is running.";
				MessageDialog waitDialog =
					new MessageDialog(
							window.getShell(), 
							title, 
							null, // accept the default window icon
							message, 
							MessageDialog.ERROR, 
							new String[] { IDialogConstants.OK_LABEL }, 0);
				waitDialog.open();
			} else if (e3.getSQLState().equals("S1000")) {
					// The most likely cause of this error state is that the database is not attached.
					String title = JDBCDatastorePlugin.getResourceString("errorTitle");
					String message = e3.getMessage() + " " + JDBCDatastorePlugin.getResourceString("databaseNotFound");
					MessageDialog waitDialog =
						new MessageDialog(
								window.getShell(), 
								title, 
								null, // accept the default window icon
								message, 
								MessageDialog.ERROR, 
								new String[] { IDialogConstants.OK_LABEL }, 0);
					waitDialog.open();
			} else {
				throw new RuntimeException(e3);
			}
		}
		
		return result;
	}

}
