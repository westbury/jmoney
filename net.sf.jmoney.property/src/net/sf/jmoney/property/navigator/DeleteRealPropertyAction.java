/**
 * 
 */
package net.sf.jmoney.property.navigator;

import net.sf.jmoney.isolation.AbstractDataOperation;
import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.property.model.RealProperty;
import net.sf.jmoney.property.resources.Messages;

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

class DeleteRealPropertyAction extends BaseSelectionListenerAction {
	private final Session session;

	DeleteRealPropertyAction(Session session) {
		super(Messages.SecuritiesActionProvider_DeleteSecurity);
		setToolTipText(Messages.SecuritiesActionProvider_DeleteSecurity);
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		this.session = session;
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		if (selection.size() != 1) {
			return false;
		}
		Object selectedObject = selection.getFirstElement();
		return selectedObject instanceof RealProperty;
	}

	@Override
	public void run() {
		// This action is enabled only when a single security is selected.
		final RealProperty realProperty = (RealProperty)getStructuredSelection().getFirstElement();

		IOperationHistory history = PlatformUI.getWorkbench()
		.getOperationSupport().getOperationHistory();

		IUndoableOperation operation = new AbstractDataOperation(
				session.getDataManager().getChangeManager(), "delete security") {
			@Override
			public IStatus execute() throws ExecutionException {
				try {
					session.getCommodityCollection().deleteElement(realProperty);
				} catch (ReferenceViolationException e) {
					MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Delete Failed", "The security is in use.  Unfortunately there is no easy way to find where the security was referenced.");
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