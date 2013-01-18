/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;

import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.Session;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/** 
 * The main class for the plug-in that implements the data storage
 * in an XML file.
 *
 * @author Nigel Westbury
 */
public class SerializedDatastorePlugin extends AbstractUIPlugin {

	public static final boolean DEBUG = "true".equalsIgnoreCase(Platform.getDebugOption("net.sf.jmoney.serializeddatastore/debug")); //$NON-NLS-1$ //$NON-NLS-2$

	//The shared instance.
	private static SerializedDatastorePlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
	
	private static String[] filterPatterns;

	private static String[] filterNames;
	
	/**
	 * The constructor.
	 */
	public SerializedDatastorePlugin() {
		plugin = this;
		try {
			resourceBundle = ResourceBundle.getBundle("net.sf.jmoney.serializeddatastore.Language"); //$NON-NLS-1$
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}
	
	/**
	 * Returns the shared instance.
	 */
	public static SerializedDatastorePlugin getDefault() {
		return plugin;
	}
	
	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	@Deprecated
	public static String getResourceString(String key) {
		ResourceBundle bundle = SerializedDatastorePlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}
	
	/**
	 * Returns the plugin's resource bundle,
	 */
	@Deprecated
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	
	/**
	 * This method is called upon plug-in activation
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		init();
	}

	/**
	 * Initialize the list of file types supported by the plug-ins.
	 */
	public static void init() {
		// Load the extensions
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor("net.sf.jmoney.serializeddatastore.filestores"); //$NON-NLS-1$

		/*
		 * Count the number of file-format extensions. (As it is the only valid
		 * element in these extensions, we could probably simply use
		 * elements.length, but this code is safer.
		 */
		int count = 0;
		for (IConfigurationElement element: elements) {
			if (element.getName().equals("file-format")) { //$NON-NLS-1$
				count++;
			}
		}
		
		filterPatterns = new String[count];
		filterNames = new String[count];
		
		int k = 0;
		for (IConfigurationElement element: elements) {
			if (element.getName().equals("file-format")) { //$NON-NLS-1$
				String filePattern = element.getAttribute("file-pattern"); //$NON-NLS-1$
				String formatDescription = element.getAttribute("format-description"); //$NON-NLS-1$

				filterPatterns[k] = filePattern;
				filterNames[k] = formatDescription + " (" + filePattern + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				k++;
			}
		}
	}
					
	/**
	 * @return an array of file extensions for all supported file formats
	 * 			 Each file extension is of the format '*.xxx'.
	 * 			This is the format expected by the FileDialog methods.
	 */
	public static String[] getFilterExtensions() {
		return filterPatterns;
	}

	/**
	 * @return an array of file descriptions for all supported file formats
	 * 			 Each file description is of the format 'description (*.xxx)'.
	 * 			This is the format expected by the FileDialog methods.
	 */
	public static String[] getFilterNames() {
		return filterNames;
	}
					
	/**
	 * This method returns an array of elements where the file extension matches the given
	 * file extension.  The caller will usually load the class object from one
	 * or more of the elements in an attempt to load or save the data.
	 * <P>
	 * This method returns the IConfigurationElement objects.  It does not
	 * load the classes.  The reason is that the caller may not necessarily
	 * need to load all the classes.  The Eclipse rules say that plug-ins
	 * should only be loaded as needed.
	 * 
	 * @return a collection of IConfigurationElement objects
	 */
	public static IConfigurationElement[] getElements(String fileName) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		Vector<IConfigurationElement> matchingElements = new Vector<IConfigurationElement>();
		for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.serializeddatastore.filestores")) { //$NON-NLS-1$
			if (element.getName().equals("file-format")) { //$NON-NLS-1$
				// The file pattern should start with an asterisk
				String filePattern = element.getAttribute("file-pattern"); //$NON-NLS-1$
				if (filePattern.charAt(0) == '*'
					&& fileName.endsWith(filePattern.substring(1))) {
					matchingElements.add(element);
				}
			}
		}
		
		return matchingElements.toArray(new IConfigurationElement[0]);
	}
					
	/**
	 * This method returns the IFileDatastore implementation class given the id.
	 */
	public static IFileDatastore getFileDatastoreImplementation(String id) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.serializeddatastore.filestores")) { //$NON-NLS-1$
			if (element.getName().equals("file-format")) { //$NON-NLS-1$
				String id2 = element.getNamespaceIdentifier() + '.' + element.getAttribute("id"); //$NON-NLS-1$
				if (id2.equals(id)) {
					try {
						return (IFileDatastore)element.createExecutableExtension("class"); //$NON-NLS-1$
					} catch (CoreException e) {
						e.printStackTrace();
						return null;
					}
				}
			}
		}
		
		return null;
	}
					
	/**
	 * Check that we have a current session and that the session
	 * was created by this plug-in.  If not, display an appropriate
	 * message to the user indicating that the user operation
	 * is not available and giving the reasons why the user
	 * operation is not available.
	 * @param window
	 *  
	 * @return true if the current session was created by this
	 * 			plug-in, false if no session is open
	 * 			or if the current session was created by
	 * 			another plug-in that also implements a datastore.
	 */
	public static boolean checkSessionImplementation(DatastoreManager datastoreManager, IWorkbenchWindow window) {
		if (datastoreManager == null) {
			MessageDialog dialog =
				new MessageDialog(
						window.getShell(), 
						Messages.SerializedDatastorePlugin_MessageMenu, 
						null, // accept the default window icon
						Messages.SerializedDatastorePlugin_MessageNoSession, 
						MessageDialog.ERROR, 
						new String[] { IDialogConstants.OK_LABEL }, 0);
			dialog.open();
			return false;
		} else if (datastoreManager instanceof SessionManager) {
			return true;
		} else {
			MessageDialog dialog =
				new MessageDialog(
						window.getShell(), 
						Messages.SerializedDatastorePlugin_MessageMenu, 
						null, // accept the default window icon
						Messages.SerializedDatastorePlugin_SaveProblem,
						MessageDialog.ERROR, 
						new String[] { IDialogConstants.OK_LABEL }, 0);
			dialog.open();
			return false;
		}
	}

	/**
	 * Create a new empty session.
	 * 
	 * @return A new SessionManager object.
	 */
	public SessionManager newSession() {
		// Create a session manager that has no file (and even
		// no file format) associated with it.
    	SessionManager sessionManager = new SessionManager(null, null, null);
    	
    	SimpleObjectKey sessionKey = new SimpleObjectKey(sessionManager);

    	Session newSession = new Session(sessionKey, null);
    	
    	sessionKey.setObject(newSession);
    	
    	sessionManager.setSession(newSession);

    	return sessionManager;
	}
}
