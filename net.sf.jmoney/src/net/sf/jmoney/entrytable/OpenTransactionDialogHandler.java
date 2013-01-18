/**
 * 
 */
package net.sf.jmoney.entrytable;

import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.resources.Messages;
import net.sf.jmoney.transactionDialog.TransactionDialog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class OpenTransactionDialogHandler extends AbstractHandler {
	private RowSelectionTracker<? extends BaseEntryRowControl> rowTracker;
	
	public OpenTransactionDialogHandler(RowSelectionTracker<? extends BaseEntryRowControl> rowTracker) {
		this.rowTracker = rowTracker;
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);
		
		BaseEntryRowControl selectedRowControl = rowTracker.getSelectedRow();
		
		if (selectedRowControl != null) {
			Entry selectedEntry = selectedRowControl.uncommittedEntryData.getEntry();

			TransactionDialog dialog = new TransactionDialog(shell, selectedEntry);
			dialog.open();
		} else {
			MessageDialog.openInformation(shell, Messages.EntriesTable_InformationTitle, Messages.EntriesTable_MessageSelect);
		}

		return null;
	}
}