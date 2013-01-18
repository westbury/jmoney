/**
 * 
 */
package net.sf.jmoney.entrytable;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class NewTransactionHandler extends AbstractHandler {
	private RowSelectionTracker<? extends BaseEntryRowControl> rowTracker;
	private EntriesTable<?> entriesTable;

	// TODO: We shouldn't really have to pass the row tracker to this constructor
	// because it must always be the one used by the given table, so better would
	// be just to get it from the table.
	public NewTransactionHandler(RowSelectionTracker<? extends BaseEntryRowControl> rowTracker, EntriesTable<?> entriesTable) {
		this.rowTracker = rowTracker;
		this.entriesTable = entriesTable;
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		/*
		 * This action is not absolutely necessary because there is
		 * always an empty row at the end of the table that can be used
		 * to add a new transaction.
		 * 
		 * This action is helpful, though, because it does one of two
		 * things:
		 * 
		 * 1. If the 'new entry' row is not the currently selected row
		 * then it sets the 'new entry' row to be the selected row,
		 * scrolling if necessary.
		 * 
		 * 2. If the 'new entry' row is the currently selected row then
		 * the entry is committed and, if the commit succeeded, the
		 * newly created 'new entry' row is selected.
		 */
		RowControl selectedRowControl = rowTracker.getSelectedRow();
		if (selectedRowControl != null) {
			if (!selectedRowControl.canDepart()) {
				return null;
			}
		}
		
		/*
		 * Regardless of whether the 'new entry' row was previously
		 * selected or not, and regardless of whether any attempt to
		 * commit the row failed, we select what is now the 'new entry'
		 * row.
		 */
		entriesTable.selectNewEntryRow();

		return null;
	}
}