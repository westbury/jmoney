package net.sf.jmoney.property.navigator;

import net.sf.jmoney.model2.Session;
import net.sf.jmoney.property.model.RealPropertyInfo;
import net.sf.jmoney.property.views.RealPropertyTypeNode;
import net.sf.jmoney.property.wizards.NewPropertyWizard;
import net.sf.jmoney.resources.Messages;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

class NewRealPropertyAction extends
		BaseSelectionListenerAction {
	private Shell shell;
	private ISelectionProvider viewer;
	private final Session session;

	NewRealPropertyAction(
			Shell shell, ISelectionProvider viewer, 
			Session session) {
		super(null);

		this.shell = shell;
		this.viewer = viewer;
		this.session = session;
		
		Object[] messageArgs = new Object[] { RealPropertyInfo.getPropertySet()
				.getObjectDescription() };

		setText(NLS.bind(Messages.AccountsActionProvider_NewAccount, messageArgs));
		setToolTipText(NLS.bind(Messages.AccountsActionProvider_CreateNewAccount, messageArgs));
		setImageDescriptor(RealPropertyInfo.getPropertySet().getIconImageDescriptor());
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		if (selection.size() != 1) {
			return false;
		}
		Object selectedObject = selection.getFirstElement();
		return 	selectedObject instanceof RealPropertyTypeNode;
	}

	@Override
	public void run() {
		NewPropertyWizard wizard = new NewPropertyWizard(session);
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.setPageSize(600, 300);
		int result = dialog.open();
		if (result == Dialog.OK && viewer != null) {
			/*
			 * Having added the new account, and if this wizard was
			 * initiated from a table or tree, set this new account as the
			 * selected account in the viewer.
			 */
			viewer.setSelection(
					new StructuredSelection(wizard.getNewAsset()));
		}
	}
}