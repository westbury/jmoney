/**
 * 
 */
package net.sf.jmoney.entrytable;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Pastes the cut transaction into the selected transaction, thus resulting
 * in a transaction with at least four entries, being all the entries from
 * both transactions.
 * 
 * The cut transaction is deleted as part of the same transaction, so if this
 * transaction is never saved then the cut transaction is never deleted.
 * 
 * @author Nigel Westbury
 *
 */
public class PasteCombineTransactionHandler extends AbstractHandler {
	private RowSelectionTracker<? extends BaseEntryRowControl> rowTracker;

	public PasteCombineTransactionHandler(RowSelectionTracker<? extends BaseEntryRowControl> rowTracker) {
		this.rowTracker = rowTracker;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);

		BaseEntryRowControl selectedRowControl = rowTracker.getSelectedRow();

		if (selectedRowControl != null) {
			Entry selectedEntry = selectedRowControl.uncommittedEntryData.getEntry();

			if (selectedEntry == null) {
				/*
				 * This is the empty row control. We could potentially put the
				 * uncommitted transaction into the clipboard and allow the
				 * entries to be pasted. However there is probably not any
				 * requirement for such a feature, so just ignore.
				 */
				MessageDialog.openInformation(shell, Messages.EntriesTable_InformationTitle, Messages.EntriesTable_MessageDeleteNotRegistered);
			} else if (JMoneyPlugin.cutTransaction == null) {
			} else {
				Transaction selectedTransaction = selectedEntry.getTransaction();

				TransactionManager transactionManager = (TransactionManager)selectedTransaction.getDataManager();

				/*
				 * Check the dates in the two transaction are the same.  The known use-cases
				 * for this feature are to simplify complex series of transactions that are
				 * created when data is imported from online banks.  In all of those situations,
				 * the dates will match, and this avoids the issue of deciding which date to
				 * use for the combined transaction. 
				 */
				if (!selectedTransaction.getDate().equals(JMoneyPlugin.cutTransaction.getDate())) {
					MessageDialog.openError(shell, Messages.EntriesTable_InformationTitle, "Dates don't match.  Transactions can only be combined if they have the same date.");
				} else {

					// Copy the entries
					for (Entry sourceEntry : JMoneyPlugin.cutTransaction.getEntryCollection()) {
						Entry newEntry = selectedTransaction.createEntry();
						for (ScalarPropertyAccessor accessor : EntryInfo.getPropertySet().getScalarProperties3() ) {
							Object value = sourceEntry.getPropertyValue(accessor);
							if (value instanceof ExtendableObject) {
								value = transactionManager.getCopyInTransaction((ExtendableObject)value); 
							}
							newEntry.setPropertyValue(accessor, value);
						}
					}

					// TODO list properties are not copied.  However at the time of writing there
					// are no Entry list properties defined in any known plug-ins.

					Transaction cutTransaction = transactionManager.getCopyInTransaction(JMoneyPlugin.cutTransaction); 
					try {
						cutTransaction.getSession().deleteTransaction(cutTransaction);
					} catch (ReferenceViolationException e) {
						/*
						 * This should not happen because we are in a
						 * transaction. This exception is not thrown until the
						 * changes are committed to the data-store.
						 */
						throw new RuntimeException("Should not happen", e);
					}

					JMoneyPlugin.cutTransaction = null;
				}
			}
		} else {
			MessageDialog.openInformation(shell, Messages.EntriesTable_InformationTitle, Messages.EntriesTable_MessageDelete);
		}
		return null;
	}
}