package net.sf.jmoney.pages.entries;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.entrytable.EntryRowControl;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.OtherEntriesPropertyBlock;
import net.sf.jmoney.entrytable.PropertyBlock;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.resources.Messages;
import net.sf.jmoney.views.AccountEditorInput;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;

public class AccountEntriesEditor extends EditorPart {

	static public final String ID = "net.sf.jmoney.accountEntriesEditor"; //$NON-NLS-1$
	
	protected Vector<IndividualBlock> allEntryDataObjects = new Vector<IndividualBlock>();

    protected EntriesFilterSection fEntriesFilterSection;

	final EntriesFilter filter = new EntriesFilter();

	/**
	 * The account being shown in this page.
	 */
	private Account account;
    
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		
		setSite(site);
		setInput(input);

		IDataManagerForAccounts sessionManager = (IDataManagerForAccounts)site.getPage().getInput();

    	// Set the account that this page is viewing and editing.
		AccountEditorInput accountEditorInput = (AccountEditorInput)input;
		account = sessionManager.getSession().getAccountByFullName(accountEditorInput.getFullAccountName());
		if (account == null) {
			throw new PartInitException("Account " + accountEditorInput.getFullAccountName() + " no longer exists.");
		}
		
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

	@Override
	public void createPartControl(Composite parent) {
    	// Build an array of all possible properties that may be
    	// displayed in the table.
        
        // Add properties from the transaction.
        for (final ScalarPropertyAccessor<?, ? super Transaction> propertyAccessor: TransactionInfo.getPropertySet().getScalarProperties3()) {
        	allEntryDataObjects.add(new PropertyBlock<EntryRowControl, Transaction>(propertyAccessor, "transaction") { //$NON-NLS-1$
    		    @Override	
        		public Transaction getObjectContainingProperty(EntryRowControl data) {
        			return data.getUncommittedMainEntry().getTransaction();
        		}
        	});
        }

        // Add properties from this entry.
        // For time being, this is all the entry properties except the account
        // which come from the other entry, and the amount which is shown in the debit and
        // credit columns.
   		for (final ScalarPropertyAccessor<?, ? super Entry> propertyAccessor: EntryInfo.getPropertySet().getScalarProperties3()) {
            if (propertyAccessor != EntryInfo.getAccountAccessor() 
           		&& propertyAccessor != EntryInfo.getIncomeExpenseCurrencyAccessor()
        		&& propertyAccessor != EntryInfo.getAmountAccessor()) {
            	if (propertyAccessor.isScalar() && propertyAccessor.isEditable()) {
            		allEntryDataObjects.add(new PropertyBlock<EntryRowControl, Entry>(propertyAccessor, "this") { //$NON-NLS-1$
            		    @Override	
    					public Entry getObjectContainingProperty(EntryRowControl data) {
    						return data.getUncommittedMainEntry();
    					}
                	});
            	}
            }
        }

        /* Add properties that show values from the other entries.
         * These are the account, description, and amount properties.
         * 
         * I don't know what to do if there are other capital accounts
         * (a transfer or a purchase with money coming from more than one account).
         */
   		allEntryDataObjects.add(new OtherEntriesPropertyBlock(EntryInfo.getAccountAccessor()));
   		allEntryDataObjects.add(new OtherEntriesPropertyBlock(EntryInfo.getMemoAccessor(), Messages.EntriesPage_EntryDescription));
   		allEntryDataObjects.add(new OtherEntriesPropertyBlock(EntryInfo.getAmountAccessor()));

        /*
		 * Add the currency column. This is placed just before the amount, which
		 * is the logical place as it is the currency for the amount. The
		 * currency has special processing because the currency is not always
		 * applicable. The currency applies only if the account is an income and
		 * expense account with the multi-currency flag set or if the account is
		 * a capital account but not a currency account. In either case, the
		 * list of currencies that may exist in the account are fetched from the
		 * account object.
		 */
   		// TODO: This is not correct at all...
        allEntryDataObjects.add(new PropertyBlock<EntryRowControl,Entry>(EntryInfo.getIncomeExpenseCurrencyAccessor(), "common2") { //$NON-NLS-1$
		    @Override	
        	public Entry getObjectContainingProperty(EntryRowControl data) {
        		Entry entry = data.getUncommittedMainEntry();
        		if (entry != null
        				&& entry.getAccount() instanceof IncomeExpenseAccount) {
        			IncomeExpenseAccount account = (IncomeExpenseAccount)entry.getAccount();
        			if (account.isMultiCurrency()) {
        				return entry;
        			}
        		}
        		
        		// Not a multi-currency account, so property not applicable.
        		return null;
        	}
        });

        /*
		 * Add the column which shows the amount of the foreign currency.
		 * This field is blank unless:
		 * - The transaction is a simple transaction (i.e. is displayed on
		 * a single line with no child rows).
		 * - The currency of the income and expense amount is different
		 * from the currency in this bank account.  This may be because
		 * the income and expense account is limited to entries in a single
		 * currency or it may be that the user chose a different currency
		 * for the income and expense amount.
		 * 
		 * This amount is always show without a sign (the sign can be
		 * determined from whether the other currency amount is in
		 * the credit or debit column).
		 */
        if (account instanceof CurrencyAccount) {
        	allEntryDataObjects.add(new PropertyBlock<EntryRowControl, Entry>(EntryInfo.getAmountAccessor(), "other") { //$NON-NLS-1$
        		@Override	
        		public Entry getObjectContainingProperty(EntryRowControl data) {
        			if (!data.getUncommittedMainEntry().hasSplitEntries()) {
        				Entry entry = data.getUncommittedMainEntry().getOtherEntry();
        				if (entry.getAccount() instanceof IncomeExpenseAccount
        						&& !JMoneyPlugin.areEqual(entry.getCommodityInternal(), ((CurrencyAccount)account).getCurrency())) {
        					return entry;
        				}
        			}

        			// If we get here, the property is not applicable for this entry.
        			return null;
        		}
        	});
        }

        FormToolkit toolkit = new FormToolkit(parent.getDisplay());
    	ScrolledForm form = toolkit.createScrolledForm(parent);
        form.getBody().setLayout(new GridLayout());
        
        fEntriesFilterSection = new EntriesFilterSection(form.getBody(), filter, allEntryDataObjects, toolkit);
        fEntriesFilterSection.getSection().setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
		// Get the handler service and pass it on so that handlers can be activated as appropriate
		IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);

		if (account instanceof CurrencyAccount) {
        	final EntriesSection fEntriesSection = new EntriesSection(form.getBody(), account, filter, toolkit, handlerService);
            fEntriesSection.getSection().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            filter.addPropertyChangeListener(new PropertyChangeListener() {
    			@Override
				public void propertyChange(PropertyChangeEvent event) {
    				fEntriesSection.refreshEntryList();
    			}
    		});
        } else {
//        	final CategoryEntriesSection fEntriesSection = new CategoryEntriesSection(form.getBody(), (IncomeExpenseAccount)account, filter, toolkit, handlerService);
        	final EntriesSection fEntriesSection = new EntriesSection(form.getBody(), account, filter, toolkit, handlerService);
            fEntriesSection.getSection().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            filter.addPropertyChangeListener(new PropertyChangeListener() {
    			@Override
				public void propertyChange(PropertyChangeEvent event) {
    				fEntriesSection.refreshEntryList();
    			}
    		});
        }

        form.setText(Messages.EntriesPage_Text);
	}
	
//	private void init(IMemento memento) {
//		if (memento != null) {
//			IMemento [] templateMementos = memento.getChildren("template");
//			for (int i = 0; i < templateMementos.length; i++) {
//				ITransactionTemplate transactionType = transactionTypes.get(templateMementos[i].getID());
//				if (transactionType != null) {
//					transactionType.init(templateMementos[i]);
//				}
//			}
//		}
//	}
	
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
}
