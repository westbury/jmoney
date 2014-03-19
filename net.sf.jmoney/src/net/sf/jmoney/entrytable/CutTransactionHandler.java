/**
 * 
 */
package net.sf.jmoney.entrytable;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class CutTransactionHandler extends AbstractHandler {
	private RowSelectionTracker<? extends BaseEntryRowControl> rowTracker;
	
	public CutTransactionHandler(RowSelectionTracker<? extends BaseEntryRowControl> rowTracker) {
		this.rowTracker = rowTracker;
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);
		
		BaseEntryRowControl<?,?> selectedRowControl = rowTracker.getSelectedRow();
		
		if (selectedRowControl != null) {
    		Entry selectedEntry = selectedRowControl.rowInput.getValue().getEntry();
			
    		if (selectedEntry == null) {
				/*
				 * This is the empty row control. We could potentially put the
				 * uncommitted transaction into the clipboard and allow the
				 * entries to be pasted. However there is probably not any
				 * requirement for such a feature, so just ignore.
				 */
    			MessageDialog.openInformation(shell, Messages.EntriesTable_InformationTitle, Messages.EntriesTable_MessageDeleteNotRegistered);
    		} else {
    			// TODO remove this hack
    			JMoneyPlugin.cutTransaction = selectedEntry.getTransaction();
    		}
		} else {
			MessageDialog.openInformation(shell, Messages.EntriesTable_InformationTitle, Messages.EntriesTable_MessageDelete);
		}
		return null;
	}
}