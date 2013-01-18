package net.sf.jmoney.jdbcdatastore;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

/** 
 * Initializes a preference store with default preference values 
 * for this plug-in.
 * <P>
 * This class is an implementation class for the <code>initializer</code>
 * element in the org.eclipse.core.runtime.preferences extension point.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {
	public PreferenceInitializer() {
		super();
	}
	
	@Override
	public void initializeDefaultPreferences() {
		Preferences store = JDBCDatastorePlugin.getDefault().getPluginPreferences();
		
    	store.setDefault("promptEachTime", true);
		store.setDefault("driverOption", "Other");

		/*
		 * List of known fragments.  If we find one and only one of these
		 * driver classes in the class path then the default value are set
		 * to values appropriate for that driver.  If none are found or more
		 * than one is found then the user is on his own and must set the
		 * values from scratch.
		 */
		
		class DriverDefaultValues {
			String className;
			String subProtocol;
			String subProtocolData;
			
			DriverDefaultValues(String className, String subProtocol, String subProtocolData) {
				this.className = className;
				this.subProtocol = subProtocol;
				this.subProtocolData = subProtocolData;
			}
		}
		
		/*
		 * These default values for both Derby and HSQLDB will use the database
		 * in-process. These are good default values because there is no
		 * database setup and so this plug-in will work 'out-of-the-box'.
		 * 
		 * If you want to use HSQLDB not in-process, allowing you to inspect the
		 * database while debugging, then set subProtocolData to something like
		 * "hsql://localhost/accounts".
		 */
		DriverDefaultValues[] knownDrivers = new DriverDefaultValues[] {
				new DriverDefaultValues("org.hsqldb.jdbcDriver", "hsqldb", "file:accounts"),
				new DriverDefaultValues("org.apache.derby.jdbc.EmbeddedDriver", "derby", "JmoneyAccounts;create=true"),
				new DriverDefaultValues("net.sourceforge.jtds.jdbc.Driver", "jtds", "sqlserver://localhost/JMoneyAccounts;instance=SQLEXPRESS"),
		};
		
		List<DriverDefaultValues> installedDrivers = new ArrayList<DriverDefaultValues>();
		for (DriverDefaultValues knownDriver : knownDrivers) {
			try {
				Class.forName(knownDriver.className);
				installedDrivers.add(knownDriver);
			} catch (ClassNotFoundException e) {
				// The fragment for this driver is not installed.
			}
		}
		
		if (installedDrivers.size() == 1) {
			DriverDefaultValues driver = installedDrivers.get(0);
			
			store.setDefault("driver", driver.className);
			store.setDefault("subProtocol", driver.subProtocol);
			store.setDefault("subProtocolData", driver.subProtocolData);
		}
		
		store.setDefault("user", "sa");
		store.setDefault("password", "");
	}
}