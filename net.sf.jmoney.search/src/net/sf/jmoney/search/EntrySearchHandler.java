package net.sf.jmoney.search;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

public class EntrySearchHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);
		IWorkbenchPage workbenchPage = HandlerUtil.getActiveWorkbenchWindowChecked(event).getActivePage();
		
		EntrySearchDialog dialog = new EntrySearchDialog(shell, workbenchPage);
		dialog.open();

		return null;
	}

}
