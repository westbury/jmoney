package net.sf.jmoney.reconciliation.reconcilePage;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;

import net.sf.jmoney.entrytable.CellBlock;
import net.sf.jmoney.entrytable.DeleteTransactionHandler;
import net.sf.jmoney.entrytable.DuplicateTransactionHandler;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.EntryRowControl;
import net.sf.jmoney.entrytable.NewTransactionHandler;
import net.sf.jmoney.entrytable.OpenTransactionDialogHandler;
import net.sf.jmoney.entrytable.OtherEntriesPropertyBlock;
import net.sf.jmoney.entrytable.PropertyBlock;
import net.sf.jmoney.entrytable.RowControl;
import net.sf.jmoney.entrytable.RowSelectionTracker;
import net.sf.jmoney.importer.matcher.ImportMatcher;
import net.sf.jmoney.importer.matcher.PatternMatchingDialog;
import net.sf.jmoney.importer.model.PatternMatcherAccount;
import net.sf.jmoney.importer.model.PatternMatcherAccountInfo;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.model2.TransactionManagerForAccounts;
import net.sf.jmoney.reconciliation.BankStatement;
import net.sf.jmoney.reconciliation.IBankStatementSource;
import net.sf.jmoney.reconciliation.ReconciliationEntryInfo;
import net.sf.jmoney.reconciliation.ReconciliationPlugin;
import net.sf.jmoney.views.AccountEditorInput;

import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;

public class ReconcileEditor extends EditorPart {

	static public final String ID = "net.sf.jmoney.accountEntriesEditor";
	
	/**
	 * The account being shown in this page.
	 */
	protected CurrencyAccount account = null;
	
	protected Vector<CellBlock> allEntryDataObjects = new Vector<CellBlock>();

	protected StatementsSection fStatementsSection;
    protected StatementSection fStatementSection;
    protected UnreconciledSection fUnreconciledSection;

	/**
	 * The statement currently being shown in this page.
	 * Null indicates that no statement is currently showing.
	 */
	BankStatement statement;
	
	/**
	 * the transaction currently being edited, or null
	 * if no transaction is being edited
	 */
	protected Transaction currentTransaction = null;

	/**
	 * The import implementation (which implements an import such as QFX, OFX etc.)
	 * for the last statement import, or null if the user has not yet done a statement import
	 */
	protected IBankStatementSource statementSource = null;
	
    
	@Override
	public void init(IEditorSite site, IEditorInput input) {
		setSite(site);
		setInput(input);
		
    	// Set the account that this page is viewing and editing.
		AccountEditorInput input2 = (AccountEditorInput)input;
        IDataManagerForAccounts sessionManager = (IDataManagerForAccounts)site.getPage().getInput();
        account = (CurrencyAccount)sessionManager.getSession().getAccountByFullName(input2.getFullAccountName());
        
        // Create our own transaction manager.
        // This ensures that uncommitted changes
    	// made by this page are isolated from datastore usage outside
    	// of this page.
//		DatastoreManager sessionManager = JMoneyPlugin.getDefault().getSessionManager();
//		session = sessionManager.getSession();
//
//        transactionManager = new TransactionManager(sessionManager);
	}

	@Override
	public boolean isDirty() {
		// Page is never dirty
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// Will never be called because editor is never dirty.
	}

	@Override
	public void doSaveAs() {
		// Will never be called because editor is never dirty and 'save as' is not allowed anyway.
	}

    public CurrencyAccount getAccount() {
    	return account;
    }

    public BankStatement getStatement() {
    	return statement;
    }

	@Override
	public void createPartControl(Composite parent) {
        /*
		 * Set the statement to show initially. If there are any entries in
		 * statements after the last reconciled statement, set the first such
		 * unreconciled statement in this view. Otherwise set the statement to
		 * null to indicate no statement is to be shown.
		 */
        // TODO: implement this
        statement = null;
        
    	// Build an array of all possible properties that may be
    	// displayed in the table.
        
        // Add properties from the transaction.
   		for (final ScalarPropertyAccessor propertyAccessor: TransactionInfo.getPropertySet().getScalarProperties3()) {
        	allEntryDataObjects.add(new PropertyBlock<EntryData, RowControl>(propertyAccessor, "transaction") {
    			@Override
        		public ExtendableObject getObjectContainingProperty(EntryData data) {
        			return data.getEntry().getTransaction();
        		}
        	});
        }

        // Add properties from this entry.
        // For time being, this is all the properties except the account
        // which come from the other entry, and the amount which is shown in the debit and
        // credit columns.
   		for (ScalarPropertyAccessor<?,?> propertyAccessor: EntryInfo.getPropertySet().getScalarProperties3()) {
            if (propertyAccessor != EntryInfo.getAccountAccessor() 
           		&& propertyAccessor != EntryInfo.getAmountAccessor()) {
            	allEntryDataObjects.add(new PropertyBlock<EntryData, RowControl>(propertyAccessor, "this") {
        			@Override
            		public ExtendableObject getObjectContainingProperty(EntryData data) {
            			return data.getEntry();
            		}
            	});
            }
        }

        /* Add properties that show values from the other entries.
         * These are the account, description, and amount properties.
         * 
         * I don't know what to do if there are other capital accounts
         * (a transfer or a purchase with money coming from more than one account).
         */
   		allEntryDataObjects.add(new OtherEntriesPropertyBlock(EntryInfo.getAccountAccessor()));
   		allEntryDataObjects.add(new OtherEntriesPropertyBlock(EntryInfo.getMemoAccessor(), "description"));
        
        FormToolkit toolkit = new FormToolkit(parent.getDisplay());
    	final ScrolledForm form = toolkit.createScrolledForm(parent);
        form.getBody().setLayout(new GridLayout(2, false));
        
        fStatementsSection = new StatementsSection(form.getBody(), toolkit, account);
        GridData data = new GridData(SWT.LEFT, SWT.FILL, false, true);
        data.verticalSpan = 2;
        fStatementsSection.getSection().setLayoutData(data);

		// Listen for double clicks.
		// Double clicking on a statement from the list will show
		// that statement in the statement table.
        fStatementsSection.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				StatementDetails statementDetails = (StatementDetails)e.item.getData();
		    	statement = statementDetails.statement;
		    	
		    	// Refresh the statement section
		    	fStatementSection.setStatement(statementDetails.statement, statementDetails.openingBalance);
			}
		});
		
		Composite actionbarContainer = new Composite(form.getBody(), 0);
		
		GridLayout actionbarLayout = new GridLayout();
		actionbarLayout.numColumns = 4;
		actionbarContainer.setLayout(actionbarLayout);
		
		final Combo fStatementsViewCombo = new Combo(actionbarContainer, SWT.DROP_DOWN);
		fStatementsViewCombo.setItems(new String [] {
				ReconciliationPlugin.getResourceString("ToolbarSection.hideStatements"),
				ReconciliationPlugin.getResourceString("ToolbarSection.showStatementsWithoutBalances"),
				ReconciliationPlugin.getResourceString("ToolbarSection.showStatementsWithBalances"),
		});
		fStatementsViewCombo.select(2);
		fStatementsViewCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				GridData gd = (GridData)fStatementsSection.getSection().getLayoutData();
				switch (fStatementsViewCombo.getSelectionIndex()) {
				case 0:
					gd.heightHint = 0;
					gd.widthHint = 0;
					break;
				case 1:
					gd.heightHint = SWT.DEFAULT;
					gd.widthHint = SWT.DEFAULT;
					fStatementsSection.showBalance(false);
					break;
				case 2:
					gd.heightHint = SWT.DEFAULT;
					gd.widthHint = SWT.DEFAULT;
					fStatementsSection.showBalance(true);
					break;
				}
				
				form.getBody().layout(true);
			}
		});
		
		Button newStatementButton = new Button(actionbarContainer, SWT.PUSH);
		newStatementButton.setText("New Statement...");
		newStatementButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				StatementDetails lastStatement = fStatementsSection.getLastStatement();
				NewStatementDialog messageBox = 
					new NewStatementDialog(getSite().getShell(), lastStatement==null ? null : lastStatement.statement);
				if (messageBox.open() == Dialog.OK) {
					statement = messageBox.getValue();
					long openingBalanceOfNewStatement = fStatementsSection.getStatementOpeningBalance(statement);
					fStatementSection.setStatement(statement, openingBalanceOfNewStatement);
				}				
			}
		});

		Button autoReconcileButton = new Button(actionbarContainer, SWT.PUSH);
		autoReconcileButton.setText("Auto-Reconcile...");
		autoReconcileButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (statement == null) {
					MessageDialog.openError(getSite().getShell(), "Error", "No statement selected");
					return;
				}
				
				/*
				 * Set the default start date to be the first day after the date of the previous
				 * statement (if any and if statements are dated, not numbered), and the default
				 * end date to be the date of this statement.
				 */
				Date defaultEndDate = 
					statement.isNumber() ? null : statement.getStatementDate();
				
				BankStatement priorStatement = fStatementsSection.getPriorStatement(statement);
				Date defaultStartDate = null;
				if (priorStatement != null && !priorStatement.isNumber()) {
					Calendar oneDayLater = Calendar.getInstance();
					oneDayLater.setTime(priorStatement.getStatementDate());
					oneDayLater.add(Calendar.DAY_OF_MONTH, 1);
					defaultStartDate = oneDayLater.getTime();
				}

				ImportStatementDialog dialog2 = new ImportStatementDialog(getSite().getShell(), defaultStartDate, defaultEndDate, null);
				if (dialog2.open() == Dialog.OK) {
					Date startDate = dialog2.getStartDate();
					Date endDate = dialog2.getEndDate();

					/*
					 * Create a transaction to be used to import the entries.  This allows the entries to
					 * be more efficiently written to the back-end datastore and it also groups
					 * the entire import as a single change for undo/redo purposes.
					 */
					TransactionManager transactionManager = new TransactionManagerForAccounts(account.getDataManager());
					CurrencyAccount accountInTransaction = transactionManager.getCopyInTransaction((account));
					Session sessionInTransaction = accountInTransaction.getSession();

					for (Entry entry : account.getEntries()) {
						if (entry.getPropertyValue(ReconciliationEntryInfo.getStatementAccessor()) == null
								&& entry.getValuta() != null
								&& !entry.getValuta().before(startDate)
								&& !entry.getValuta().after(endDate)){
							entry.setPropertyValue(ReconciliationEntryInfo.getStatementAccessor(), statement);
						}
					}
					
					/*
					 * All entries have been reconciled, so we
					 * can now commit the entries to the datastore.
					 */
					transactionManager.commit("Auto-Reconcile to " + statement.toLocalizedString());									
				}
			}
		});

		final ToolBar toolBar =
			new ToolBar(actionbarContainer, SWT.FLAT);
		final ToolItem importButton =
			new ToolItem(toolBar, SWT.DROP_DOWN);
		importButton.setText("Import");
		final Menu menu = new Menu(getSite().getShell(), SWT.POP_UP);

		
		// The list of sources are taken from the net.sf.jmoney.reconciliation.bankstatements
		// extension point.
		
		// Load the extensions
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint("net.sf.jmoney.reconciliation.bankstatements");
		IExtension[] extensions = extensionPoint.getExtensions();
		
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				if (elements[j].getName().equals("statement-source")) {
					String description = elements[j].getAttribute("description");
					
					MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
					menuItem.setText(description);
					
					final IConfigurationElement thisElement = elements[j];
					
					menuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent event) {
							try {
								// Load the extension point listener for the selected source
								statementSource = (IBankStatementSource)thisElement.createExecutableExtension("class");
								importStatement();
							} catch (CoreException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								throw new RuntimeException("class attribute not found");
							}
						}
					});
				}
			}
		}		  

		final PatternMatcherAccount reconciliationAccount = account.getExtension(PatternMatcherAccountInfo.getPropertySet(), true);

		importButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (!reconciliationAccount.isReconcilable()) {
			        MessageBox diag = new MessageBox(getSite().getShell());
			        diag.setText("Feature not Available");
			        diag.setMessage("Before you can import entries from your bank's servers, you must first set the rules for the initial categories for the imported entries.  Press the 'Options...' button to set this up.");
			        diag.open();
			        return;
				}
				
				if (statement == null) {
			        MessageBox diag = new MessageBox(getSite().getShell());
			        diag.setText("Feature not Available");
			        diag.setMessage("Before you can import entries from your bank's servers, you must first create or select a bank statement into which the entries will be imported.");
			        diag.open();
			        return;
				}
				
				if (event.detail == SWT.NONE) {
					/*
					 * The user pressed the import button, but not on the down-arrow that
					 * is positioned at the right side of the import button.  In this case,
					 * we import using the format that was last used, or we ignore the click
					 * if the user has not yet done an import using this button.
					 */
					if (statementSource != null) {
						importStatement();
					}
				} else if (event.detail == SWT.ARROW) {
					Rectangle rect = importButton.getBounds();
					Point pt = new Point(rect.x, rect.y + rect.height);
					menu.setLocation(toolBar.toDisplay(pt));
					menu.setVisible(true);
				}
			}
		});

		Button optionsButton = new Button(actionbarContainer, SWT.PUSH);
		optionsButton.setText("Options...");
		optionsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ImportOptionsDialog messageBox = 
					new ImportOptionsDialog(getSite().getShell(), reconciliationAccount);
				messageBox.open();
			}
		});
		
        Composite containerOfSash = new Composite(form.getBody(), 0);
        containerOfSash.setLayout(new FormLayout());

        // Create the sash first, so the other controls
        // can be attached to it.
        final Sash sash = new Sash(containerOfSash, SWT.BORDER | SWT.HORIZONTAL);
        FormData formData = new FormData();
        formData.left = new FormAttachment(0, 0); // Attach to left
        formData.right = new FormAttachment(100, 0); // Attach to right
        formData.top = new FormAttachment(50, 0); // Attach halfway down
        sash.setLayoutData(formData);

        sash.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent event) {
        		final int mimimumHeight = 61;  // In Windows, allows 3 lines minimum.  TODO: Calculate this for other OS's
        		int y = event.y;
        		if (y < mimimumHeight) {
        			y = mimimumHeight;
        		}
        		if (y + sash.getSize().y > sash.getParent().getSize().y - mimimumHeight) {
        			y = sash.getParent().getSize().y - mimimumHeight - sash.getSize().y;
        		}

        		// We re-attach to the top edge, and we use the y value of the event to
        		// determine the offset from the top
        		((FormData) sash.getLayoutData()).top = new FormAttachment(0, y);

        		// Until the parent window does a layout, the sash will not be redrawn in
        		// its new location.
        		sash.getParent().layout();
        	}
        });

        GridData gridData1 = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData1.heightHint = 200;   // TODO: tidy up???
        gridData1.widthHint = 200;   // TODO: tidy up???
        containerOfSash.setLayoutData(gridData1);
        
        /*
         * The common row tracker.  This is used by both tables, so that
         * there is only one selection in the part.
         */
	    RowSelectionTracker<EntryRowControl> rowTracker = new RowSelectionTracker<EntryRowControl>();
        
        fStatementSection = new StatementSection(containerOfSash, toolkit, this, rowTracker);

        formData = new FormData();
        formData.top = new FormAttachment(0, 0);
        formData.bottom = new FormAttachment(sash, 0);
        formData.left = new FormAttachment(0, 0);
        formData.right = new FormAttachment(100, 0);
        fStatementSection.getSection().setLayoutData(formData);
        
        fUnreconciledSection = new UnreconciledSection(containerOfSash, toolkit, this, rowTracker);

        formData = new FormData();
        formData.top = new FormAttachment(sash, 0);
        formData.bottom = new FormAttachment(100, 0);
        formData.left = new FormAttachment(0, 0);
        formData.right = new FormAttachment(100, 0);
        fUnreconciledSection.getSection().setLayoutData(formData);

        form.setText("Reconcile Entries against Bank Statement/Bank's Records");

         /*
		 * Activate the handlers. Note that the 'new' and 'duplicate' actions
		 * put the new entry into the unreconciled section. It is important that
		 * is where the new entry is put because no statement is set on the
		 * entry for a new or duplicated entry.
		 */
		IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);

		IHandler handler = new NewTransactionHandler(rowTracker, fUnreconciledSection.fUnreconciledEntriesControl);
		handlerService.activateHandler("net.sf.jmoney.newTransaction", handler);		

		handler = new DeleteTransactionHandler(rowTracker);
		handlerService.activateHandler("net.sf.jmoney.deleteTransaction", handler);		

		handler = new DuplicateTransactionHandler(rowTracker, fUnreconciledSection.fUnreconciledEntriesControl);
		handlerService.activateHandler("net.sf.jmoney.duplicateTransaction", handler);		

		handler = new OpenTransactionDialogHandler(rowTracker);
		handlerService.activateHandler("net.sf.jmoney.transactionDetails", handler);		
	}
	
	public void saveState(IMemento memento) {
//		for (String id: transactionTypes.keySet()) {
//			ITransactionTemplate transactionType = transactionTypes.get(id);
//			transactionType.saveState(memento.createChild("template", id));
//		}
	}
	
	@Override
	public void setFocus() {
		// Don't bother to do anything.  User can select as required.
	}
	/**
	 * Imports data from an external source (usually the Bank's server or a file downloaded
	 * from the Bank's server) into this bank statement.
	 * 
	 * Before this method can be called, the following must be set:
	 * 
	 * 1. statementSource must be set to an implementation of the IBankStatementSource interface.
	 * 2. A statement must be open in the editor 
	 */
	void importStatement() {
//		outer: for (Entry entry : account.getBaseObject().getEntries()) {
//		if (entry.getCheck() == null && entry.getMemo() != null) {
//			if (entry.getMemo().length() < 3) continue;
//			if (entry.getMemo().length() > 4) continue;
//			String memo = entry.getMemo();
//			if (memo.charAt(0) == '0') memo = memo.substring(1);
//			if (memo.length() < 3) continue;
//			if (memo.charAt(0) == '0') continue;
//			for (int i=0; i < memo.length(); i++) {
//				if (memo.charAt(i) < '0' || memo.charAt(i) > '9') continue outer; 
//			}
//			entry.setCheck(memo);
//			entry.setMemo("check " + memo);
//		}
//			
//		}
	
	
		/*
		 * Set the default start date to be the first day after the date of the previous
		 * statement (if any and if statements are dated, not numbered), and the default
		 * end date to be the date of this statement.
		 */
		Date defaultEndDate = 
			statement.isNumber() ? null : statement.getStatementDate();
		
		BankStatement priorStatement = fStatementsSection.getPriorStatement(statement);
		Date defaultStartDate = null;
		if (priorStatement != null && !priorStatement.isNumber()) {
			Calendar oneDayLater = Calendar.getInstance();
			oneDayLater.setTime(priorStatement.getStatementDate());
			oneDayLater.add(Calendar.DAY_OF_MONTH, 1);
			defaultStartDate = oneDayLater.getTime();
		}
		
		/*
		 * Create an import wizard that wraps the import source.
		 * 
		 * This wizard is the same wizard that is used when the user selects
		 * import from the 'Import Tabular Data...' item on the file menu.  
		 */
//		ImportAccount accountImporterExtension = account.getExtension(ImportAccountInfo.getPropertySet(), true);
//		IAccountImportWizard wizard = accountImporterExtension.getImportWizard();
//		if (wizard == null) {
//			MessageDialog.openError(getSite().getShell(), "Import not Available", "In order to import here, you must set up an import method for this account.  This is done by setting the table structure property of the account.");
//			return;
//		}

		/*
		 * We listen for entries that have been added to the account and we
		 * set them to be in this bank statement.
		 * 
		 * This is done asynchronously because modifying the model within a
		 * model change listener is a bad idea.
		 */
		
		Collection<net.sf.jmoney.importer.matcher.EntryData> importedEntries = statementSource.importEntries(getSite().getShell(), getAccount(), defaultStartDate, defaultEndDate);
		if (importedEntries != null) {
			/*
			 * Open a dialog that allows the user to interactively review and edit the pattern matching
			 * rules, seeing how they apply against these imported entries.
			 */
			PatternMatcherAccount matcherAccount = account.getExtension(PatternMatcherAccountInfo.getPropertySet(), true);
			Dialog dialog = new PatternMatchingDialog(this.getSite().getShell(), matcherAccount, importedEntries);
			if (dialog.open() == Dialog.OK) {
				/*
				 * Create a transaction to be used to import the entries.  This allows the entries to
				 * be more efficiently written to the back-end datastore and it also groups
				 * the entire import as a single change for undo/redo purposes.
				 */
				TransactionManager transactionManager = new TransactionManagerForAccounts(account.getDataManager());
				CurrencyAccount accountInTransaction = transactionManager.getCopyInTransaction((account));
				Session sessionInTransaction = accountInTransaction.getSession();

				ImportMatcher matcher = new ImportMatcher(accountInTransaction.getExtension(PatternMatcherAccountInfo.getPropertySet(), true));

				for (net.sf.jmoney.importer.matcher.EntryData entryData: importedEntries) {
					Entry entry = matcher.process(entryData, sessionInTransaction);
					entry.setPropertyValue(ReconciliationEntryInfo.getStatementAccessor(), statement);
				}

				/*
				 * All entries have been imported and all the properties
				 * have been set and should be in a valid state, so we
				 * can now commit the imported entries to the datastore.
				 */
				transactionManager.commit("Import Entries");
			}
		}
	}
}
