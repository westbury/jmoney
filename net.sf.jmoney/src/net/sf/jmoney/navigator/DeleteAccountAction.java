/**
 * 
 */
package net.sf.jmoney.navigator;

import net.sf.jmoney.isolation.AbstractDataOperation;
import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

class DeleteAccountAction extends BaseSelectionListenerAction {
	private final Session session;

	DeleteAccountAction(Session session) {
		super(Messages.AccountsActionProvider_DeleteAccount);
		setToolTipText(Messages.AccountsActionProvider_DeleteAccount);
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		this.session = session;
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		if (selection.size() != 1) {
			return false;
		}
		Object selectedObject = selection.getFirstElement();
		return selectedObject instanceof Account;
	}

	@Override
	public void run() {
		// This action is enabled only when a single account is selected.
		final Account account = (Account)getStructuredSelection().getFirstElement();

		IOperationHistory history = PlatformUI.getWorkbench()
		.getOperationSupport().getOperationHistory();

		IUndoableOperation operation = new AbstractDataOperation(
				session.getDataManager().getChangeManager(), "delete account") { //$NON-NLS-1$
			@Override
			public IStatus execute() throws ExecutionException {
				try {
					session.deleteAccount(account);
				} catch (ReferenceViolationException e) {
					MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Delete Failed", "The account is in use.  "
							+ "The reference is a reference to this account in its capacity as a '" + ((ExtendablePropertySet)e.getPropertySet()).getObjectDescription()
							+ "'.  Further information: " + e.getSqlErrorMessage());
					return Status.CANCEL_STATUS;
				}

				return Status.OK_STATUS;
			}
		};

		operation.addContext(session.getDataManager().getUndoContext());
		try {
			history.execute(operation, null, null);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}