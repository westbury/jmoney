package net.sf.jmoney.mail.preferences;

import net.sf.jmoney.email.Activator;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.databinding.property.value.ValueProperty;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.ViewerSupport;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Base class of a <code>PreferencePage</code> for using <code>ListsSelector</code>.
 */
public class MailboxesPreferencePage extends PreferencePage implements
IWorkbenchPreferencePage {

	private static final String MAILBOXES_KEY = "mailboxes";

	private TableViewer mailboxesViewer;

	private IObservableList<Mailbox> mailboxes;

	private DataBindingContext bindingContext = new DataBindingContext();

	private IViewerObservableValue selectedMailbox;

	private IValueProperty hostProperty = new ValueProperty() {
		@Override
		public Object getValueType() {
			return String.class;
		}

		@Override
		public IObservableValue<String> observe(Realm realm, final Object source) {
			/*
			 * Note that we can't return the address observable directly because the
			 * caller will dispose the returned observable.
			 */
			IObservableValue<String> result = new WritableValue<String>(null, String.class);
			new DataBindingContext().bindValue(result, ((Mailbox)source).host);
			return result;
		}
	};

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
			new DataBindingContext().bindValue(result, ((Mailbox)source).address);
			return result;
		}
	};

	private IValueProperty passwordProperty = new ValueProperty() {
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
			new DataBindingContext().bindValue(result, ((Mailbox)source).password);
			return result;
		}
	};
	
	public MailboxesPreferencePage() {
		mailboxes = new WritableList<Mailbox>();
	}

	@Override
	public void init(IWorkbench workbench) {
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        
        String value = getPreferenceStore().getString(MAILBOXES_KEY);
        if (!value.isEmpty()) {
        	String mailboxValues [] = value.split("~");
        	for (String mailboxValue : mailboxValues) {
        		String parts [] = mailboxValue.split(",");
        		if (parts.length == 3) {
        			mailboxes.add(new Mailbox(parts[0], parts[1], parts[2]));
        		} else if (parts.length == 2) {
        			mailboxes.add(new Mailbox(parts[0], parts[1], ""));
        		}
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

		Control table = createTableAndButtons(composite);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createConfigurationControls(composite);
		
		return composite;
	}

	private Control createTableAndButtons(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		Control table = createTable(composite);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createButtons(composite).setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		return composite;
	}

	private Control createTable(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		mailboxesViewer = new TableViewer(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.widthHint = 300;
		gridData.heightHint = 100;
		mailboxesViewer.getTable().setLayoutData(gridData);
		
		mailboxesViewer.getTable().setHeaderVisible(true);
		mailboxesViewer.getTable().setLinesVisible(true);

//		ObservableListContentProvider provider = new ObservableListContentProvider();
//		mailboxesViewer.setContentProvider(provider);
		
//		IObservableSet<?> knownElements = provider.getKnownElements();
		
		
		
		// Sort by host name then e-mail address
//		mailboxesViewer.setComparator(new ViewerComparator() {
//			@Override
//		    public int compare(Viewer viewer, Object element1, Object element2) {
//				Mailbox mailbox1 = (Mailbox)element1;
//				Mailbox mailbox2 = (Mailbox)element2;
//				int compare =  mailbox1.host.compareTo(mailbox2.host);
//				if (compare == 0) {
//					compare = mailbox1.address.compareTo(mailbox2.address);
//				}
//				return compare;
//			}
//		});
		
		TableViewerColumn hostColumn = new TableViewerColumn(mailboxesViewer, SWT.LEFT);
		hostColumn.getColumn().setText("POP3 Host");
		hostColumn.getColumn().setWidth(150);

//		hostColumn.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(ViewerCell cell) {
//				Mailbox mailbox = (Mailbox)cell.getElement();
//				cell.setText(mailbox.host.getValue());
//			}
//		});

//		IObservableMap<?,?> hostDetailMap = hostProperty.observeDetail(knownElements);
//		hostColumn.setLabelProvider(new ObservableMapCellLabelProvider(hostDetailMap));
		
		TableViewerColumn addressColumn = new TableViewerColumn(mailboxesViewer, SWT.LEFT);
		addressColumn.getColumn().setText("E-Mail Address");
		addressColumn.getColumn().setWidth(150);

//		addressColumn.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(ViewerCell cell) {
//				Mailbox mailbox = (Mailbox)cell.getElement();
//				cell.setText(mailbox.address.getValue());
//			}
//		});

//		IObservableMap<?,?> addressDetailMap = addressProperty.observeDetail(knownElements);
//		addressColumn.setLabelProvider(new ObservableMapCellLabelProvider(addressDetailMap));

		TableViewerColumn passwordColumn = new TableViewerColumn(mailboxesViewer, SWT.LEFT);
		passwordColumn.getColumn().setText("Password");
		passwordColumn.getColumn().setWidth(100);

//		passwordColumn.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(ViewerCell cell) {
//				Mailbox mailbox = (Mailbox)cell.getElement();
//				cell.setText(mailbox.password.getValue());
//			}
//		});

//		IObservableMap<?,?> passwordDetailMap = passwordProperty.observeDetail(knownElements);
//		passwordColumn.setLabelProvider(new ObservableMapCellLabelProvider(passwordDetailMap));

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
		
//		mailboxesViewer.setInput(mailboxes);

		// Sets content provider, all label providers and input
		ViewerSupport.bind(
				mailboxesViewer, 
				mailboxes, 
				new IValueProperty [] { hostProperty, addressProperty, passwordProperty }
		);

		return composite;
	}

	private Control createButtons(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		RowLayout layout = new RowLayout(SWT.VERTICAL);
		layout.pack = false;
		composite.setLayout(layout);

		Button addButton = new Button(composite, SWT.PUSH);
		addButton.setText("Add");
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Mailbox newMailbox = new Mailbox("pop.gmail.com", "<user>", "<password>");
				mailboxes.add(newMailbox);
				selectedMailbox.setValue(newMailbox);
			}
		});
		
		Button deleteButton = new Button(composite, SWT.PUSH);
		deleteButton.setText("Delete");
		deleteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (selectedMailbox.getValue() != null) {
					Mailbox selectedAddress = (Mailbox)selectedMailbox.getValue();
					mailboxes.remove(selectedAddress);
				}
			}
		});
		
		return composite;
	}

	private Control createConfigurationControls(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		new Label(composite, SWT.NONE).setText("POP3 Host:");
		Text hostField = new Text(composite, SWT.BORDER);
		hostField.setLayoutData(new GridData(300, SWT.DEFAULT));
		
		new Label(composite, SWT.NONE).setText("E-Mail Address:");
		Text addressField = new Text(composite, SWT.BORDER);
		addressField.setLayoutData(new GridData(300, SWT.DEFAULT));
		
		new Label(composite, SWT.NONE).setText("Password:");
		Text passwordField = new Text(composite, SWT.BORDER);
		passwordField.setLayoutData(new GridData(200, SWT.DEFAULT));

		selectedMailbox = ViewersObservables.observeSingleSelection(mailboxesViewer);
		
		bindingContext.bindValue(
				WidgetProperties.text(SWT.Modify).observe(hostField),
				hostProperty.observeDetail(selectedMailbox));
		bindingContext.bindValue(
				WidgetProperties.text(SWT.Modify).observe(addressField),
				addressProperty.observeDetail(selectedMailbox));
		bindingContext.bindValue(
				WidgetProperties.text(SWT.Modify).observe(passwordField),
				passwordProperty.observeDetail(selectedMailbox));
		
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
		StringBuffer buffer = new StringBuffer();
		String separator = "";
		for (Mailbox mailbox : mailboxes) {
			buffer
			.append(separator)
			.append(mailbox.host.getValue())
			.append(',')
			.append(mailbox.address.getValue())
			.append(',')
			.append(mailbox.password.getValue());
			separator = "~";
		}
        IPreferenceStore preferenceStore = getPreferenceStore();

        preferenceStore.setValue(MAILBOXES_KEY, buffer.toString());
        
		return super.performOk();
	}

	static class Mailbox {
		final IObservableValue<String> address = new WritableValue<String>();
		final IObservableValue<String> host = new WritableValue<String>();
		final IObservableValue<String> password = new WritableValue<String>();
//		String address;
//		String host;
//		String password;

		public Mailbox(String host, String address, String password) {
//			this.host = host;
//			this.address = address;
//			this.password = password;
			this.host.setValue(host);
			this.address.setValue(address);
			this.password.setValue(password);
		}

//		String getAddress() {
//			return address;
//		}
//		
//		void setAddress(String address) {
//			this.address = address;
//		}
//
//		String getHost() {
//			return host;
//		}
//		
//		void setHost(String host) {
//			this.host = host;
//		}
//		
//		void setPassword(String password) {
//			this.password = password;
//		}
//
//		String getPassword() {
//			return password;
//		}
	}
}
