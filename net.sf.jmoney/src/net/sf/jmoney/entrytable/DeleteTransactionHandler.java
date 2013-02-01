/**
 * 
 */
package net.sf.jmoney.entrytable;

import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionManagerForAccounts;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class DeleteTransactionHandler extends AbstractHandler {
	private RowSelectionTracker<? extends BaseEntryRowControl> rowTracker;
	
	public DeleteTransactionHandler(RowSelectionTracker<? extends BaseEntryRowControl> rowTracker) {
		this.rowTracker = rowTracker;
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);
		
		BaseEntryRowControl selectedRowControl = rowTracker.getSelectedRow();
		
		if (selectedRowControl != null) {
    		Entry selectedEntry = selectedRowControl.committedEntryData.getEntry();
			
    		if (selectedEntry == null) {
    			// This is the empty row control.
    			// TODO: Should we just clear the control contents?
    			MessageDialog.openInformation(shell, Messages.EntriesTable_InformationTitle, Messages.EntriesTable_MessageDeleteNotRegistered);
    		} else {
    			// Does this need be so complex??? It is only in a transaction
    			// so we can undo it.  A more efficient way would be to make the change
    			// in a callback.

				try {
	    			TransactionManager transactionManager = new TransactionManagerForAccounts(selectedEntry.getDataManager());
	    			Entry selectedEntry2 = transactionManager.getCopyInTransaction(selectedEntry); 
	    			Transaction transaction = selectedEntry2.getTransaction();
	    			transaction.getSession().deleteTransaction(transaction);
	    			transactionManager.commit("Delete Transaction"); //$NON-NLS-1$
				} catch (ReferenceViolationException e) {
					MessageDialog.openError(shell, "Delete Failed", "There are references to this transaction or to entries within this transaction.  Unfortunately there is no easy way to find where the transaction is referenced.");
				}
    		}
		} else {
			MessageDialog.openInformation(shell, Messages.EntriesTable_InformationTitle, Messages.EntriesTable_MessageDelete);
		}
		return null;
	}
}