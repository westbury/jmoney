package net.sf.jmoney.importer.wizards;

import java.text.MessageFormat;

import net.sf.jmoney.associations.AssociationMetadata;
import net.sf.jmoney.associations.IAssociatedAccountInfoProvider;
import net.sf.jmoney.importer.model.ImportAccountInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

public class CsvImportToAccountAssociations implements
IAssociatedAccountInfoProvider {

	@Override
	public AssociationMetadata[] getAssociationMetadata(Account account) {
		/*
		 * The importDataExtensionId property in the account is an id for a configuration element
		 * in plugin.xml.  This configuration element in turn gives us the id of a suitable import
		 * wizard.  The wizard gives us the set of valid associations.
		 */
		if (account instanceof CapitalAccount) {
			IConfigurationElement wizardElement = findWizard((CapitalAccount)account);
			if (wizardElement != null) {
				try {
					Object executableExtension = wizardElement.createExecutableExtension("class"); //$NON-NLS-1$
					return ((IAccountImportWizard)executableExtension).getAssociationMetadata();
				} catch (CoreException e) {
					throw new RuntimeException("Cannot create import wizard for " + account.getName() + ".", e);
				}
			}
		}
		
		return new AssociationMetadata[0];
	}

	@Override
	public String getGroupName(Account account) {
		/*
		 * The importDataExtensionId property in the account is an id for a configuration element
		 * in plugin.xml.  This configuration element in turn gives us the id of a suitable import
		 * wizard.  The wizard gives us the set of valid associations.
		 */
		if (account instanceof CapitalAccount) {
			IConfigurationElement wizardElement = findWizard((CapitalAccount)account);
			if (wizardElement != null) {
				String importLabel = wizardElement.getAttribute("label");
				return MessageFormat.format("Accounts for Importing {0}", importLabel);
			}
		}
		
		// Should not get here if no associations.  Perhaps needs a little
		// tidy up.
		return null;
	}

	private IConfigurationElement findWizard(CapitalAccount account) {
		// Note: This method must track getters.  Hence we go through an observable in the following line.
		String importDataExtensionId = ImportAccountInfo.getImportDataExtensionIdAccessor().observe((CapitalAccount)account).getValue();
		if (importDataExtensionId != null) {
			// Find the wizard by reading the registry.
			IAccountImportWizard wizard = null;
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.importer.importdata")) { //$NON-NLS-1$ $NON-NLS-2$
				if (element.getName().equals("import-format") //$NON-NLS-1$
						&& element.getAttribute("id").equals(importDataExtensionId)) { //$NON-NLS-1$
					return element;
				}
			}
		}
		return null;
	}
}
