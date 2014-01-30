package net.sf.jmoney.associations.propertyPages;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.sf.jmoney.associations.AssociationMetadata;
import net.sf.jmoney.associations.IAssociatedAccountInfoProvider;
import net.sf.jmoney.fields.AccountControl;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Session;

import org.eclipse.core.databinding.observable.set.ComputedSet;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.set.ISetChangeListener;
import org.eclipse.core.databinding.observable.set.SetChangeEvent;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

public class AssociatedAccountsSection
extends AbstractPropertySection {

	private IObservableValue<CapitalAccount> account = new WritableValue<CapitalAccount>();

	private Composite composite;

	private List<IAssociatedAccountInfoProvider> associationExtensions = new ArrayList<IAssociatedAccountInfoProvider>();

	@Override
	public void createControls(Composite parent,
			TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);
		System.out.println("createControls");
		composite = getWidgetFactory().createComposite(parent);
		composite.setLayout(new GridLayout());

		/*
		 * Load and interrogate all IAssociatedAccountInfoProvider extensions and create a section
		 * for each one.
		 */
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.associations.metadata")) { //$NON-NLS-1$ $NON-NLS-2$
			if (element.getName().equals("associations")) { //$NON-NLS-1$
				try {
					Object executableExtension = element.createExecutableExtension("class"); //$NON-NLS-1$
					associationExtensions.add((IAssociatedAccountInfoProvider)executableExtension);
				} catch (CoreException e) {
					throw new RuntimeException("Cannot create association extension: " + element.getNamespaceIdentifier() + ".", e);
				}
			}
		}

		final Comparator<IAssociatedAccountInfoProvider> comparator = new Comparator<IAssociatedAccountInfoProvider>() {
			@Override
			public int compare(IAssociatedAccountInfoProvider provider1,
					IAssociatedAccountInfoProvider provider2) {
				return provider1.getGroupName(account.getValue()).compareToIgnoreCase(provider2.getGroupName(account.getValue()));
			}
		};

		final IObservableSet<IAssociatedAccountInfoProvider> activeExtensions = new ComputedSet<IAssociatedAccountInfoProvider>() {
			@Override
			protected Set<IAssociatedAccountInfoProvider> calculate() {
				Set<IAssociatedAccountInfoProvider> result = new TreeSet<IAssociatedAccountInfoProvider>(comparator );
	  			for (IAssociatedAccountInfoProvider provider : associationExtensions) {
	  				if (provider.getAssociationMetadata(account.getValue()).length != 0) {
	  					result.add(provider);
	  				}
	  			}
				return result;
			}
		};
		
		// Create new
		if (activeExtensions.isEmpty()) {
			getWidgetFactory().createCLabel(composite, "The importer for this account does not require any associated accounts.");
		} else {
			for (IAssociatedAccountInfoProvider provider : activeExtensions) {
				createGroup(composite, provider).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			}
		}

		final ISetChangeListener<IAssociatedAccountInfoProvider> listener = new ISetChangeListener<IAssociatedAccountInfoProvider>() {
			@Override
			public void handleSetChange(
					SetChangeEvent<IAssociatedAccountInfoProvider> event) {
				// Dispose all
				for (Control child : composite.getChildren()) {
					child.dispose();
				}
				
				// Create new
				if (activeExtensions.isEmpty()) {
					getWidgetFactory().createCLabel(composite, "The importer for this account does not require any associated accounts.");
				} else {
					for (IAssociatedAccountInfoProvider provider : activeExtensions) {
						createGroup(composite, provider).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					}
				}
				
				// Is this a hack?
				composite.getParent().layout(true);
			}
		};
		
		activeExtensions.addSetChangeListener(listener);
		
		composite.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				activeExtensions.removeSetChangeListener(listener);
			}
		});
	}

	// TODO: This code will not update if the set of associated accounts
	// returned by the provider should change.
	private Control createGroup(Composite parent,
			IAssociatedAccountInfoProvider provider) {
		Group composite = new Group(parent, SWT.NONE);
		composite.setText(provider.getGroupName(account.getValue()));
		composite.setLayout(new GridLayout(2, false));
		
		final Session session = account.getValue().getSession();

		AssociationMetadata[] associationMetadata = provider.getAssociationMetadata(account.getValue());
		for (AssociationMetadata association : associationMetadata) {
		getWidgetFactory().createCLabel(composite, association.getLabel() + ":"); //$NON-NLS-1$

		final AccountControl<Account> accountControl = new AccountControl<Account>(composite, Account.class) {
			@Override
			protected Session getSession() {
				return session;
			}
		};

		Bind.twoWay(new AssociatedAccountProperty(association.getId()).observeDetail(account))
			.to(accountControl.account);
		}
		
		return composite;
	}

	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
		super.setInput(part, selection);
		System.out.println("setInput");
		
		IStructuredSelection objectSelection = (IStructuredSelection) selection;

		if (objectSelection.size() == 1) {
			Object input = objectSelection.getFirstElement();

			if (input instanceof CapitalAccount) {
				account.setValue((CapitalAccount) input);
			} else {
				account.setValue(null);
			}
		} else {
			account.setValue(null);
		}
	}

	@Override
	public void refresh() {
		System.out.println("refresh");
		IPropertySource properties = (IPropertySource) account.getValue()
				.getAdapter(IPropertySource.class);
	}

	@Override
	public void aboutToBeHidden() {
		System.out.println("aboutToBeHidden");
		/* empty default implementation */
	}
}

