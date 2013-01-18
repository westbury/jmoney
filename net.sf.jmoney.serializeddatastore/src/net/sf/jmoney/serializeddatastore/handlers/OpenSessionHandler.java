package net.sf.jmoney.serializeddatastore.handlers;

import java.io.File;
import java.util.Map;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.serializeddatastore.IFileDatastore;
import net.sf.jmoney.serializeddatastore.Messages;
import net.sf.jmoney.serializeddatastore.SerializedDatastorePlugin;
import net.sf.jmoney.serializeddatastore.SessionManager;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.services.IEvaluationService;

/**
 * Opens a new session. The command has two boolean parameters, so this handler
 * is really handling four different commands.
 * <P>
 * The parameters are:
 * <UL>
 * <LI>
 * net.sf.jmoney.serializeddatastore.openSession.newWindow <BR>
 * true if the session is to be opened in a new window <BR>
 * false if the session is to be opened in the current window, first closing any
 * session that may already be open in the current window</LI>
 * <LI>
 * net.sf.jmoney.serializeddatastore.openSession.newSession <BR>
 * true if a new session is to be created <BR>
 * false if an existing session is to be opened from file, in which case the
 * user will be prompted for the file name</LI>
 * </UL>
 */
public class OpenSessionHandler extends AbstractHandler {

	/**
	 * True/false value to open the session in a new window.
	 */
	private static final String PARAMETER_NEW_WINDOW = "net.sf.jmoney.serializeddatastore.openSession.newWindow"; //$NON-NLS-1$

	/**
	 * True/false value to create a new session (rather than open one from a
	 * file)
	 */
	private static final String PARAMETER_NEW_SESSION = "net.sf.jmoney.serializeddatastore.openSession.newSession"; //$NON-NLS-1$

	public final Object execute(final ExecutionEvent event)
			throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil
				.getActiveWorkbenchWindowChecked(event);

		final Map parameters = event.getParameters();
		final String newWindow = (String) parameters.get(PARAMETER_NEW_WINDOW);
		final String newSession = (String) parameters
				.get(PARAMETER_NEW_SESSION);

		if (newWindow == null || newWindow.equalsIgnoreCase("false")) { //$NON-NLS-1$
			if (!JMoneyPlugin.getDefault().saveOldSession(window)) {
				/*
				 * Cancelled by user or the save failed. If the save failed the
				 * user will have been notified, so we simply exit here.
				 */
				return null;
			}

			IWorkbenchPage activePage = window.getActivePage();
			try {
				activePage.close();
			} catch (RuntimeException re) {
				System.out.println("OpenSessionHandler : " + re.getMessage());
			}
		}

		try {
			SessionManager newSessionManager;
			if (newSession != null && newSession.equalsIgnoreCase("true")) { //$NON-NLS-1$
				newSessionManager = SerializedDatastorePlugin.getDefault()
						.newSession();
			} else {
				newSessionManager = openSession(window);
				if (newSessionManager == null) {
					return null;
				}
			}

			// This call needs to be cleaned up, but is still needed
			// to ensure a default currency is set.
			JMoneyPlugin.getDefault().initializeNewSession(newSessionManager);

			/*
			 * This call will open the session in the current window if there is
			 * no page (i.e. if we closed the previous page above), or it will
			 * open the page in a new window if there is already a page in this
			 * window (i.e. if we did not close the previous page above).
			 */
			IWorkbenchPage newPage = window.openPage(newSessionManager);

			// Update the title
			String productName = Platform.getProduct().getName();
			newPage.getWorkbenchWindow().getShell().setText(
					productName + " - "
							+ newSessionManager.getBriefDescription());

			/*
			 * The state of the 'isSessionOpen' property may have changed, so we
			 * force a re-evaluation which will update any UI items whose state
			 * depends on this property.
			 */
			IEvaluationService service = (IEvaluationService) PlatformUI
					.getWorkbench().getService(IEvaluationService.class);
			service.requestEvaluation("net.sf.jmoney.core.isSessionOpen"); //$NON-NLS-1$

		} catch (WorkbenchException e) {
			ErrorDialog.openError(window.getShell(),
					Messages.OpenSessionHandler_OpenSessionFailed, e
							.getMessage(), e.getStatus());
			throw new ExecutionException("Session could not be opened. " + e.getLocalizedMessage(), e); //$NON-NLS-1$
		} catch (OpenSessionException e) {
			MessageDialog.openError(window.getShell(),
					Messages.OpenSessionAction_ErrorTitle,
					e.getMessage());
			throw new ExecutionException("Session could not be opened.", e); //$NON-NLS-1$
		} finally {
			/*
			 * Regardless of any exception that may have been thrown, we cannot leave the workbench
			 * window without a page.  That looks silly and other code assumes we always have a page.
			 */
			if (window.getActivePage() == null) {
				try {
					window.openPage(null);
					//Update title
					String productName = Platform.getProduct().getName();
					window.getShell().setText(productName);
				} catch (WorkbenchException e) {
					throw new ExecutionException("Workbench exception occured while closing window.", e); //$NON-NLS-1$
				}

				/*
				 * The state of the 'isSessionOpen' property may have changed, so we
				 * force a re-evaluation which will update any UI items whose state
				 * depends on this property.
				 */
				IEvaluationService service = (IEvaluationService) PlatformUI
				.getWorkbench().getService(IEvaluationService.class);
				service.requestEvaluation("net.sf.jmoney.core.isSessionOpen"); //$NON-NLS-1$
			}
		}

		return null;
	}

	/**
	 * 
	 * @param window
	 * @return the session, or null if user canceled
	 * @throws OpenSessionException
	 */
	private SessionManager openSession(IWorkbenchWindow window)
			throws OpenSessionException {
		FileDialog dialog = new FileDialog(window.getShell());
		dialog.setFilterExtensions(SerializedDatastorePlugin
				.getFilterExtensions());
		dialog.setFilterNames(SerializedDatastorePlugin.getFilterNames());
		String fileName = dialog.open();

		if (fileName != null) {
			File sessionFile = new File(fileName);

			IConfigurationElement elements[] = SerializedDatastorePlugin
					.getElements(fileName);

			if (elements.length == 0) {
				/*
				 * The user has entered an extension that is not recognized.
				 */
				throw new OpenSessionException(Messages.SessionManager_UnknownFileExtension);
			}

			// TODO: It is possible that multiple plug-ins may
			// use the same file extension. There are two possible
			// approaches to this: either ask the user which is
			// the format of the file, or we try to load the file
			// using each in turn until one works.

			// For time being, we simply use the first entry.
			IFileDatastore fileDatastore;
			String fileFormatId;
			try {
				fileDatastore = (IFileDatastore) elements[0]
						.createExecutableExtension("class"); //$NON-NLS-1$
				fileFormatId = elements[0].getDeclaringExtension()
						.getNamespaceIdentifier()
						+ '.' + elements[0].getAttribute("id"); //$NON-NLS-1$
			} catch (CoreException e) {
				throw new OpenSessionException(e);
			}

			SessionManager sessionManager = new SessionManager(fileFormatId,
					fileDatastore, sessionFile);
			boolean isGoodFileRead = fileDatastore.readSession(sessionFile,
					sessionManager, window);
			if (!isGoodFileRead) {
				throw new OperationCanceledException();
			}

			return sessionManager;
		} else {
			return null;
		}
	}
}
