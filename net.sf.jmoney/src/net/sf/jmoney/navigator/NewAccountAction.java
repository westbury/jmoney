/**
 * 
 */
package net.sf.jmoney.navigator;

import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.resources.Messages;
import net.sf.jmoney.views.AccountsNode;
import net.sf.jmoney.wizards.NewAccountWizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

class NewAccountAction extends
		BaseSelectionListenerAction {
	private Shell shell;
	private StructuredViewer viewer;
	private final Session session;
	private final ExtendablePropertySet<? extends CapitalAccount> propertySet;

	NewAccountAction(
			Shell shell, StructuredViewer viewer, 
			ExtendablePropertySet<? extends CapitalAccount> propertySet,
			Session session) {
		super(null);

		this.shell = shell;
		this.viewer = viewer;
		this.session = session;
		this.propertySet = propertySet;
		
		Object[] messageArgs = new Object[] { propertySet
				.getObjectDescription() };

		setText(NLS.bind(Messages.AccountsActionProvider_NewAccount, messageArgs));
		setToolTipText(NLS.bind(Messages.AccountsActionProvider_CreateNewAccount, messageArgs));
		setImageDescriptor(propertySet.getIconImageDescriptor());
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		if (selection.size() != 1) {
			return false;
		}
		Object selectedObject = selection.getFirstElement();
		return 
		selectedObject instanceof AccountsNode
		|| selectedObject instanceof CapitalAccount;
	}

	@Override
	public void run() {
		// This action is enabled only when a single object is selected.
		Object selectedObject = getStructuredSelection().getFirstElement();
		
		CapitalAccount account = (selectedObject instanceof CapitalAccount)
			? (CapitalAccount)selectedObject
					: null;

		NewAccountWizard wizard = new NewAccountWizard(session,
				account, propertySet);
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.setPageSize(600, 300);
		int result = dialog.open();
		if (result == WizardDialog.OK && viewer != null) {
			/*
			 * Having added the new account, and if this wizard was
			 * initiated from a table or tree, set this new account as the
			 * selected account in the viewer.
			 */
			viewer.setSelection(
					new StructuredSelection(wizard.getNewAccount()),
					true);
		}
	}
}