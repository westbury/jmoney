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

package net.sf.jmoney;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.CurrencyInfo;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.Propagator;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.views.TreeNode;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class JMoneyPlugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "net.sf.jmoney"; //$NON-NLS-1$

    public static final boolean DEBUG = "true".equalsIgnoreCase(Platform.getDebugOption("net.sf.jmoney/debug")); //$NON-NLS-1$ //$NON-NLS-2$

	//The shared instance.
	private static JMoneyPlugin plugin;

	// Bit of a hack, used for cut and paste of transactions
	public static Transaction cutTransaction = null;
	
	//Resource bundle.
	@Deprecated
	private ResourceBundle resourceBundle;
	
//    private DatastoreManager sessionManager = null;

    /**
	 * The constructor.
	 */
	public JMoneyPlugin() {
		super();
		plugin = this;
		try {
			resourceBundle   = ResourceBundle.getBundle("net.sf.jmoney.resources.Language"); //$NON-NLS-1$
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
		
		PropertySet.init();
		Propagator.init();
		TreeNode.init();
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
	public static JMoneyPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 * @deprecated Use Messages in package net.sf.jmoney.resources.
	 */
	@Deprecated
	public static String getResourceString(String key) {
		ResourceBundle bundle = JMoneyPlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	public static Image createImage(String name) {
		//control for icons
		String finalName = name;
		if(!name.startsWith("icons/")){//$NON-NLS-1$
			finalName = "icons/"+finalName;//$NON-NLS-1$
		}
//		String iconPath = "icons/";
		String iconPath = ""; //$NON-NLS-1$
		try {
			URL installURL = getDefault().getBundle().getEntry("/"); //$NON-NLS-1$
			//URL url = new URL(installURL, iconPath + name);
			URL url = new URL(installURL, finalName);
			return ImageDescriptor.createFromURL(url).createImage();
		} catch (MalformedURLException e) {
			// should not happen
			return ImageDescriptor.getMissingImageDescriptor().createImage();
		}
	}

	/**
	 * Given the name of an image file in the icons directory of
	 * this bundle, creates an image descriptor.  This method does
	 * not use a cache but will create a new descriptor on each call.
	 * 
	 * @param name
	 * @return
	 */
	public static ImageDescriptor createImageDescriptor(String name) {
		try {
			URL installURL = getDefault().getBundle().getEntry("icons/"); //$NON-NLS-1$
			URL url = new URL(installURL, name);
			return ImageDescriptor.createFromURL(url);
		} catch (MalformedURLException e) {
			// should not happen
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}

	/**
	 * Log status to log the of this plug-in.
	 */	
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	/**
	 * Log exception to the log of this plug-in.
	 * 
	 * @param e Exception to log
	 */
	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, JMoneyPlugin.PLUGIN_ID, IStatus.ERROR, "Internal errror", e)); //$NON-NLS-1$
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	@Deprecated
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	
    public DatastoreManager getSessionManager() {
		return (DatastoreManager)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getInput();
    }
   
    /**
	 * Saves the old session.
	 * Returns false if canceled by user or the save fails.
	 */
	public boolean saveOldSession(IWorkbenchWindow window) {
		DatastoreManager sessionManager = getSessionManager();
		if (sessionManager == null) {
			return true;
		} else {
			return sessionManager.canClose(window);
		}
	}
	
	// Helper method
    // TODO: see if we really need this method.
    public Session getSession() {
		DatastoreManager sessionManager = getSessionManager();
        return sessionManager == null 
			? null 
			: sessionManager.getSession();
    }
    
    /**
     * Initializes a new session with stuff that all sessions must have.
     */
    public void initializeNewSession(DatastoreManager newSessionManager) {
    	/*
    	 * JMoney depends on having at least one currency, which must also be
    	 * set as the default currency. If there is no default currency then
    	 * this must be a new datastore and we must set a default currency.
    	 */
    	if (newSessionManager.getSession().getDefaultCurrency() == null) {
    		initSystemCurrency(newSessionManager.getSession());
    	}
    }

    /**
     * Get the corresponding ISO currency for "code". If "session" already
     * contains such a currency this currency is returned. Otherwise, we
     * check our list of ISO 4217 currencies and we create a new currency
     * instance for "session".
     * 
     * @param session Session object which will contain the currency
     * @param code ISO currency code
     * @return Currency for "code"
     */
    public static Currency getIsoCurrency(Session session, String code) {
        // Check if the currency already exists for this session.
        Currency result = session.getCurrencyForCode(code);
        if (result != null) return result;

        // Find the currency in our list of ISO 4217 currencies
        ResourceBundle res = ResourceBundle.getBundle("net.sf.jmoney.resources.Currency"); //$NON-NLS-1$
        byte decimals = 2;
        String name = null;
        try{
        	name = res.getString(code);
        }catch (Exception e){
        	//Problem when retrieve the name return null
        	return null;
        }
        try {
            InputStream in = JMoneyPlugin.class.getResourceAsStream("Currencies.txt"); //$NON-NLS-1$
            BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
            for (String line = buffer.readLine(); line != null; line = buffer.readLine()) {
                if (line.substring(0, 3).equals(code)) {
                	// The Currencies.txt file does not contain the number of decimals
                	// for every currency.  If no number is in the file then a StringIndexOutOfBoundsException
                	// will be thrown and we assume two decimal places.
                	try {
                		decimals = Byte.parseByte(line.substring(4, 5));
                	} catch (StringIndexOutOfBoundsException e) {
                		decimals = 2;
                	}
                }
            }
        } catch (IOException ioex) {
            log(ioex);
        } catch (NumberFormatException nfex) {
            log(nfex);
        }

        result = session.createCommodity(CurrencyInfo.getPropertySet());
        result.setCode(code);
        result.setName(name);
        result.setDecimals(decimals);

        return result;
    }

    /**
     * Whenever a new session is created, JMoney will set a single initial
     * currency.  The currency is taken from our list of ISO 4217
     * currencies and chosen using information from the default locale.
     * This currency is also set as the default currency.
     * <P>
     * By doing this, we minimize the number of steps that a new JMoney
     * user must take to get started.  If a user only ever uses a single
     * currency then the user may never have to worry about currencies
     * and may never see a currency selection control.
     * 
     * @param session
     */
	public static void initSystemCurrency(Session session) {
        Locale defaultLocale = Locale.getDefault();
        NumberFormat format = NumberFormat.getCurrencyInstance(defaultLocale);
        String code = format.getCurrency().getCurrencyCode();
        Currency currency = getIsoCurrency(session, code);
        if (currency == null) {
        	// JMoney depends on a default currency
        	currency = getIsoCurrency(session, "USD"); //$NON-NLS-1$
        }
        
        /*
         * Note that although we are modifying the datastore,
         * we do not make this an undoable operation.  The user
         * did not set this currency and the user should not be
         * able to undo it.
         */
        session.setDefaultCurrency(currency);
    }

    // Preferences
    
    /**
     * Get the format to be used for dates.  This format is
     * compatible with the VerySimpleDateFormat class.
     * The format is read from the preference store.
     */
    public String getDateFormat() {
    	/*
		 * The following line cannot return a null value, even if the user did
		 * not set a value, because a default value is set. The default value is
		 * set by by JMoneyPreferenceInitializer (an extension to the
		 * org.eclipse.core.runtime.preferences extension point).
		 */
    	return getPreferenceStore().getString("dateFormat"); //$NON-NLS-1$
    }

	/**
	 * Helper method to compare two objects.  Either or both
	 * the objects may be null.  If both objects are null,
	 * they are considered equal.
	 * 
	 * @param object
	 * @param object2
	 * @return
	 */
	public static boolean areEqual(Object object1, Object object2) {
		return (object1 == null)
			? (object2 == null)
					: object1.equals(object2);
	}
}
