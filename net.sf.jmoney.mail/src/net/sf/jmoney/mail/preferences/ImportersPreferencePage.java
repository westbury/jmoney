package net.sf.jmoney.mail.preferences;

import net.sf.jmoney.email.Activator;
import net.sf.jmoney.email.IMailReader;
import net.sf.jmoney.model2.MalformedPluginException;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.set.WritableSet;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.property.set.ISetProperty;
import org.eclipse.core.databinding.property.set.SetProperty;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.databinding.property.value.ValueProperty;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ObservableMapCellLabelProvider;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Base class of a <code>PreferencePage</code> for using <code>ListsSelector</code>.
 */
public class ImportersPreferencePage extends PreferencePage implements
IWorkbenchPreferencePage {

	private static final String MAILBOXES_KEY = "mailboxes";

	private static final String MAIL_IMPORTERS_KEY = "mailimporters";

	private IObservableList<String> mailboxes;

	private TableViewer importersViewer;

	protected IObservableSet<Importer> importers;

	private DataBindingContext bindingContext = new DataBindingContext();

	private IViewerObservableValue selectedImporter;

	private IValueProperty addressProperty = new ValueProperty() {
		@Override
		public Object getValueType() {
			return String.class;
		}

		@Override
		public IObservableValue<?> observe(Realm realm, final Object source) {
			/*
			 * Note that we can't return the address observable directly because the
			 * caller will dispose the returned observable.
			 */
			IObservableValue<String> result = new WritableValue<String>(null, String.class);
			new DataBindingContext().bindValue(result, ((Importer)source).address);
			return result;
		}
	};

	private ISetProperty foldersProperty = new SetProperty() {
		@Override
		public Object getElementType() {
			return String.class;
		}

		@Override
		public IObservableSet<?> observe(Realm realm, final Object source) {
			/*
			 * Note that we can't return the address observable directly because the
			 * caller will dispose the returned observable.
			 */
			IObservableSet<String> result = new WritableSet<String>(null, String.class);
			new DataBindingContext().bindSet(result, ((Importer)source).folders);
			return result;
		}
	};
	
	public ImportersPreferencePage() {
		importers = new WritableSet<Importer>();
		
		/*
		 * Read the list of importers.
		 */
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.mail.mailimporter")) { //$NON-NLS-1$
			
			if (element.getName().equals("mail")) { //$NON-NLS-1$
				String id = element.getAttribute("id"); //$NON-NLS-1$
				String name = element.getAttribute("name"); //$NON-NLS-1$
				if (name == null || name.trim().length() == 0) {
					throw new MalformedPluginException(
							"Plug-in " + element.getContributor().getName() //$NON-NLS-1$
							+ " extends the net.sf.jmoney.mail extension point. " //$NON-NLS-1$
							+ "However, no 'name' attribute is specified."); //$NON-NLS-1$
				}
				try {
					Object extensionImpl = element.createExecutableExtension("class"); //$NON-NLS-1$
					if (!(extensionImpl instanceof IMailReader)) {
						throw new MalformedPluginException(
								"Plug-in " + element.getContributor().getName() //$NON-NLS-1$
								+ " extends the net.sf.jmoney.mail extension point. " //$NON-NLS-1$
								+ "However, the class specified by the class attribute in the node element " //$NON-NLS-1$
								+ "(" + extensionImpl.getClass().getName() + ") " //$NON-NLS-1$ //$NON-NLS-2$
								+ "does not implement the IMailReader interface. " //$NON-NLS-1$
								+ "This interface must be implemented by all classes referenced " //$NON-NLS-1$
								+ "by the class attribute."); //$NON-NLS-1$
					}

					Importer importer = new Importer(
							element.getNamespaceIdentifier() + '.' + id, 
							name, 
							(IMailReader)extensionImpl);
					importers.add(importer);
				} catch (CoreException e) {
					if (e.getStatus().getException() instanceof ClassNotFoundException) {
						ClassNotFoundException e2 = (ClassNotFoundException)e.getStatus().getException();
						throw new MalformedPluginException(
								"Plug-in " + element.getContributor().getName() //$NON-NLS-1$
								+ " extends the net.sf.jmoney.mail extension point. " //$NON-NLS-1$
								+ "However, the class specified by the class attribute in the <mail> element " //$NON-NLS-1$
								+ "(" + e2.getMessage() + ") " //$NON-NLS-1$ //$NON-NLS-2$
								+ "could not be found. " //$NON-NLS-1$
								+ "The class attribute must specify a class that implements the " //$NON-NLS-1$
								+ "IMailReader interface."); //$NON-NLS-1$
					}
					e.printStackTrace();
					continue;
				}
			}
		}
		
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());

		mailboxes = new WritableList<String>();
        fetchMailboxes();
        getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				mailboxes.clear();
		        fetchMailboxes();
			}
		});
		
		for (Importer importer : importers) {
			String address = getPreferenceStore().getString(MAIL_IMPORTERS_KEY + "." + importer.id + ".address");
			if (address.length() != 0) {
				importer.address.setValue(address);
			}

			String folderValue = getPreferenceStore().getString(MAIL_IMPORTERS_KEY + "." + importer.id + ".folders");
			if (!folderValue.isEmpty()) {
				for (String folder : folderValue.split("~")) {
					importer.folders.add(folder);
				}
			}
		}
	}

	private void fetchMailboxes() {
		String value = getPreferenceStore().getString(MAILBOXES_KEY);
        if (!value.isEmpty()) {
        	String mailboxValues [] = value.split("~");
        	for (String mailboxValue : mailboxValues) {
        		String parts [] = mailboxValue.split(",");
       			mailboxes.add(parts[1]);
        	}
        }
	}

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.None);
		composite.setLayout(new GridLayout());

		if (importers.isEmpty()) {
			new Label(composite, SWT.WRAP).setText("No mail importers are installed.  If you want to import accounting data from e-mail messages then "
					+ "you must have at least one plug-in installed that extends the mail import extension point.");
		} else {
		Control table = createTable(composite);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createConfigurationControls(composite);
		}
		
		return composite;
	}

	private Control createTable(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		importersViewer = new TableViewer(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.widthHint = 300;
		gridData.heightHint = 100;
		importersViewer.getTable().setLayoutData(gridData);
		
		importersViewer.getTable().setHeaderVisible(true);
		importersViewer.getTable().setLinesVisible(true);

		importersViewer.setContentProvider(ArrayContentProvider.getInstance());
		
		// Sort by the displayable name
		importersViewer.setComparator(new ViewerComparator() {
			@Override
		    public int compare(Viewer viewer, Object element1, Object element2) {
				Importer importer1 = (Importer)element1;
				Importer importer2 = (Importer)element2;
				return importer1.name.compareToIgnoreCase(importer2.name);
			}
		});
		
		TableViewerColumn nameColumn = new TableViewerColumn(importersViewer, SWT.LEFT);
		nameColumn.getColumn().setText("Mail Importer");
		nameColumn.getColumn().setWidth(150);
		nameColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Importer importer = (Importer)cell.getElement();
				cell.setText(importer.name);
			}
		});

		TableViewerColumn addressColumn = new TableViewerColumn(importersViewer, SWT.LEFT);
		addressColumn.getColumn().setText("E-Mail Address");
		addressColumn.getColumn().setWidth(150);

		IObservableMap addressDetailMap = addressProperty.observeDetail(importers);
		addressColumn.setLabelProvider(new ObservableMapCellLabelProvider(addressDetailMap));

		// Create the pop-up menu
//		MenuManager menuMgr = new MenuManager();
//		// TODO We are making assumptions about where this editor is placed when
//		// we make the following cast to AccountEditor.  Can this be cleaned up?
//		menuMgr.add(new ShowDetailsAction(addressesViewer));
//		menuMgr.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
//		getSite().registerContextMenu(menuMgr, addressesViewer);
//			
//		Control control = addressesViewer.getControl();
//		Menu menu = menuMgr.createContextMenu(control);
//		control.setMenu(menu);		
		
		importersViewer.setInput(importers);

		return composite;
	}

	private Control createConfigurationControls(Composite parent) {
		final Group composite = new Group(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		importersViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Importer importer = (Importer)((IStructuredSelection)importersViewer.getSelection()).getFirstElement();
				if (importer == null) {
					composite.setText("");
				} else {
					composite.setText("Mailbox details for " + importer.name);
				}
			}
		});
		
		selectedImporter = ViewersObservables.observeSingleSelection(importersViewer);
		
		new Label(composite, SWT.NONE).setText("Receiving E-Mail Address:");
		ComboViewer mailboxesViewer = new ComboViewer(composite);
		mailboxesViewer.setContentProvider(new ObservableListContentProvider());
		bindingContext.bindValue(
				ViewersObservables.observeSingleSelection(mailboxesViewer),
				addressProperty.observeDetail(selectedImporter));
		mailboxesViewer.setInput(mailboxes);		
		
//		new Label(composite, SWT.NONE).setText("POP3 Host:");
//		Text hostField = new Text(composite, SWT.BORDER);
//		hostField.setLayoutData(new GridData(300, SWT.DEFAULT));
//		
//		new Label(composite, SWT.NONE).setText("E-Mail Address:");
//		Text addressField = new Text(composite, SWT.BORDER);
//		addressField.setLayoutData(new GridData(300, SWT.DEFAULT));
//		
//		new Label(composite, SWT.NONE).setText("Password:");
//		Text passwordField = new Text(composite, SWT.BORDER);
//		passwordField.setLayoutData(new GridData(200, SWT.DEFAULT));

//		bindingContext.bindValue(
//				WidgetProperties.text(SWT.Modify).observe(hostField),
//				hostProperty.observeDetail(selectedImporter));
//		bindingContext.bindValue(
//				WidgetProperties.text(SWT.Modify).observe(addressField),
//				addressProperty.observeDetail(selectedImporter));
//		bindingContext.bindValue(
//				WidgetProperties.text(SWT.Modify).observe(passwordField),
//				foldersProperty.observeDetail(selectedImporter));
		
		return composite;
	}

	/**
	 * This implementation will set all items as chosen by default.
	 * Override for different defaults.
	 */
	@Override
	protected void performDefaults() {
		// No defaults here
	}

	@Override
	public boolean performOk() {
        IPreferenceStore preferenceStore = getPreferenceStore();
        
		for (Importer importer : importers) {

			preferenceStore.setValue(MAIL_IMPORTERS_KEY + "." + importer.id + ".address", importer.address.getValue());

			// Folders
			StringBuffer buffer = new StringBuffer();
			String separator = "";
			for (String folder : importer.folders) {
				buffer
				.append(separator)
				.append(folder);
				separator = "~";
			}
		
			preferenceStore.setValue(MAIL_IMPORTERS_KEY + "." + importer.id + ".folders", buffer.toString());
		}
		
		return super.performOk();
	}

	static class Importer {
		final String id;
		final String name;
		final IMailReader reader;
		
		IObservableValue<String> address = new WritableValue<String>();
		IObservableSet<String> folders = new WritableSet<String>();

		public Importer(String id, String name, IMailReader reader) {
			this.id = id;
			this.name = name;
			this.reader = reader;
		}
	}
}
