/**
 * 
 */
package net.sf.jmoney.entrytable;

import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class DuplicateTransactionHandler extends AbstractHandler {
	private RowSelectionTracker<? extends BaseEntryRowControl> rowTracker;
	private EntriesTable<?> entriesTable;
	
	/**
	 * 
	 * @param rowTracker
	 * @param entriesTable the table, the 'new entry' row of which is the target into
	 * 			which data from the source transaction is copied 
	 */
	public DuplicateTransactionHandler(RowSelectionTracker<? extends BaseEntryRowControl> rowTracker, EntriesTable<?> entriesTable) {
		this.rowTracker = rowTracker;
		this.entriesTable = entriesTable;
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);
		
		BaseEntryRowControl<?,?> selectedRowControl = rowTracker.getSelectedRow();
		
		if (selectedRowControl != null) {
			Entry selectedEntry = selectedRowControl.rowInput.getValue().getEntry();

			if (selectedEntry == null) {
				// This is the empty row control.
				// TODO: Should we attempt to commit this first, then duplicate it if it committed?
				MessageDialog.openInformation(shell, Messages.EntriesTable_InformationTitle, Messages.EntriesTable_MessageDuplicateNotRegistered);
			} else {
				/*
				 * The 'new entry' row was not the selected row, so we
				 * know that there can't be any changes in the 'new
				 * entry' row. We copy properties from the selected
				 * entry into the new 'entry row' and then select the
				 * 'new entry' row.
				 */

//				EntryData uncommittedEntryData = entriesTable.getNewEntryRowData();
				EntryRowControl newEntryRow = (EntryRowControl) entriesTable.selectNewEntryRow();
				newEntryRow.copyFrom(selectedRowControl.rowInput.getValue());
				
				// The 'new entry' row control should be listening for changes to
				// its uncommitted data, so we have nothing more to do. 
			}
		} else {
			MessageDialog.openInformation(shell, Messages.EntriesTable_InformationTitle, Messages.EntriesTable_MessageDuplicate);
		}

		return null;
	}
}