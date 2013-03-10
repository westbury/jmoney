package net.sf.jmoney.importer.propertyPages;

import java.util.HashMap;
import java.util.Map;

import net.sf.jmoney.fields.AccountControl;
import net.sf.jmoney.importer.model.AccountAssociation;
import net.sf.jmoney.importer.model.AccountAssociationInfo;
import net.sf.jmoney.importer.model.ImportAccount;
import net.sf.jmoney.importer.model.ImportAccountInfo;
import net.sf.jmoney.importer.wizards.AssociationMetadata;
import net.sf.jmoney.importer.wizards.IAccountImportWizard;
import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Session;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

public class AssociationsForImportSection
extends AbstractPropertySection {

	private CapitalAccount account;

	private ModifyListener listener = new ModifyListener() {

		@Override
		public void modifyText(ModifyEvent arg0) {
			IPropertySource properties = (IPropertySource) account
			.getAdapter(IPropertySource.class);
			//            properties.setPropertyValue(IPropertySource.PROPERTY_TEXT,
			//                labelText.getText());
		}
	};

	private Composite composite;

	private Map<AssociationMetadata, AccountControl<Account>> labelTexts;

	private HashMap<String, AccountAssociation> existingAssociations;

	@Override
	public void createControls(Composite parent,
			TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);
		System.out.println("createControls");
		composite = getWidgetFactory().createComposite(parent);
		composite.setLayout(new GridLayout(2, false));
	}

	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
		super.setInput(part, selection);
		System.out.println("setInput");
		Object input = ((IStructuredSelection) selection).getFirstElement();
		this.account = (CapitalAccount) input;

		/*
		 * Find all associations set in this account and move into a map.
		 */
		existingAssociations = new HashMap<String, AccountAssociation>();
		ImportAccount a = account.getExtension(ImportAccountInfo.getPropertySet(), false);
		if (a != null) {
			for (AccountAssociation aa : a.getAssociationCollection()) {
				existingAssociations.put(aa.getId(), aa);
			}
		}
		
		for (Control child : composite.getChildren()) {
			child.dispose();
		}
		
		labelTexts = new HashMap<AssociationMetadata, AccountControl<Account>>();

		/*
		 * The importDataExtensionId property in the account is an id for a configuration element
		 * in plugin.xml.  This configuration element in turn gives us the id of a suitable import
		 * wizard.  We start the given wizard, first setting the account into it.
		 */
		String importDataExtensionId = ImportAccountInfo.getImportDataExtensionIdAccessor().getValue(account);
		if (importDataExtensionId == null) {
			CLabel labelLabel = getWidgetFactory().createCLabel(composite, "The importer for this account does not require any associated accounts.");
		} else {

			// Find the wizard by reading the registry.
			IAccountImportWizard wizard = null;
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.importer.importdata")) { //$NON-NLS-1$ $NON-NLS-2$
				if (element.getName().equals("import-format") //$NON-NLS-1$
						&& element.getAttribute("id").equals(importDataExtensionId)) { //$NON-NLS-1$
					try {
						Object executableExtension = element.createExecutableExtension("class"); //$NON-NLS-1$
						wizard = (IAccountImportWizard)executableExtension;
					} catch (CoreException e) {
						throw new RuntimeException("Cannot create import wizard for " + account.getName() + ".", e);
					}
				}
			}

			Session session = account.getSession();

			final ImportAccount aNotNull = account.getExtension(ImportAccountInfo.getPropertySet(), true);

			for (final AssociationMetadata association : wizard.getAssociationMetadata()) {
				getWidgetFactory().createCLabel(composite, association.getLabel() + ":"); //$NON-NLS-1$

				final AccountControl<Account> accountControl = new AccountControl<Account>(composite, session, Account.class);
				labelTexts.put(association, accountControl);

				AccountAssociation aa2 = existingAssociations.get(association.getId());
				if (aa2 != null) {
					accountControl.setAccount(aa2.getAccount());
				}
				
				accountControl.addSelectionListener(new SelectionAdapter() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						Account a2 = accountControl.getAccount();
						AccountAssociation aa2 = existingAssociations.get(association.getId());
						if (a2 == null) {
							if (aa2 != null) {
								try {
									aNotNull.getAssociationCollection().deleteElement(aa2);
									existingAssociations.remove(aa2.getId());
								} catch (ReferenceViolationException e1) {
									/* No one references the account associations so this should not happen.
									 * But if it does happen, some one has added a plug-in that does have a reference
									 * to this association so we tell the user that the association cannot be altered.
									 */
									MessageDialog.openError(getPart().getSite().getShell(), "data violation", e1.getMessage());
								}
							}
						} else {
							if (aa2 == null) {
								aa2 = aNotNull.getAssociationCollection().createNewElement(AccountAssociationInfo.getPropertySet());
								aa2.setId(association.getId());
								existingAssociations.put(aa2.getId(), aa2);
							}
							aa2.setAccount(a2);
						}
					}
				});
				
//				accountControl.addModifyListener(listener);
//				accountControl.
			}
		}
	
	}

	@Override
	public void refresh() {
		System.out.println("refresh");
		for (AssociationMetadata association : labelTexts.keySet()) {
			AccountControl<Account> accountControl = labelTexts.get(association);

//			accountControl.removeModifyListener(listener);
			IPropertySource properties = (IPropertySource) account
			.getAdapter(IPropertySource.class);
//			accountControl.setAccount(association.).setText("hello"/*properties.strText*/);
//			accountControl.addModifyListener(listener);
		}
	}

	@Override
	public void aboutToBeHidden() {
		System.out.println("aboutToBeHidden");
		/* empty default implementation */
	}
}

