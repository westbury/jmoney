package net.sf.jmoney.serializeddatastore.handlers;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.IDatastoreManager;
import net.sf.jmoney.serializeddatastore.SerializedDatastorePlugin;
import net.sf.jmoney.serializeddatastore.SessionManager;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler to save the session.
 */
public final class SaveSessionHandler extends AbstractHandler {

	public final Object execute(final ExecutionEvent event)
			throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		IDatastoreManager datastoreManager = (IDatastoreManager)window.getActivePage().getInput();
		if (SerializedDatastorePlugin.checkSessionImplementation(datastoreManager, window)) { 
			SessionManager myDatastoreManager = (SessionManager)JMoneyPlugin.getDefault().getSessionManager();
			myDatastoreManager.saveSession(window);
		}

		return null;
	}
}
