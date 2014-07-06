/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2010 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.importer.model;

import net.sf.jmoney.importer.wizards.IAccountImportWizard;
import net.sf.jmoney.model2.CapitalAccountExtension;
import net.sf.jmoney.model2.ExtendableObject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

/**
 * An extension object that extends BankAccount objects.
 * This extension object maintains the values of the properties
 * that have been added by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class ImportAccount extends CapitalAccountExtension {
	
	protected String importDataExtensionId = null;
	
	/**
	 * A default constructor is mandatory for all extension objects.
	 * The default constructor sets the extension properties to
	 * appropriate default values.
	 */
	public ImportAccount(ExtendableObject extendedObject) {
		super(extendedObject);
	}
	
	/**
	 * A Full constructor is mandatory for all extension objects.
	 * This constructor is called by the datastore to construct
	 * the extension objects when loading data.
	 * 
	 */
	public ImportAccount(
			ExtendableObject extendedObject,
			String importDataExtensionId) {
		super(extendedObject);
		this.importDataExtensionId = importDataExtensionId;
	}
	
	public String getImportDataExtensionId() {
		return importDataExtensionId;
	}
	
	public void setImportDataExtensionId(String importDataExtensionId) {
		String oldImportDataExtensionId = this.importDataExtensionId;
		this.importDataExtensionId = importDataExtensionId;
		processPropertyChange(ImportAccountInfo.getImportDataExtensionIdAccessor(), oldImportDataExtensionId, importDataExtensionId);
	}
	
	public IAccountImportWizard getImportWizard() {
		/*
		 * The importDataExtensionId property in the account is an id for a configuration element
		 * in plugin.xml.  This configuration element in turn gives us the id of a suitable import
		 * wizard.
		 */
		if (importDataExtensionId == null) {
			return null;
		}

		// Find the wizard by reading the registry.
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.importer.importdata")) { //$NON-NLS-1$ $NON-NLS-2$
			if (element.getName().equals("import-format") //$NON-NLS-1$
					&& element.getAttribute("id").equals(importDataExtensionId)) { //$NON-NLS-1$
				try {
					Object executableExtension = element.createExecutableExtension("class"); //$NON-NLS-1$
					return  (IAccountImportWizard)executableExtension;
				} catch (CoreException e) {
					throw new RuntimeException("Cannot create import wizard for " + getName() + ".", e);
				}
			}
		}
		
		/*
		 * The account has an import table structure set but it cannot be found.
		 * Probably the plug-in is no longer installed.
		 */
		return null;
	}
}
