/**
 * 
 */
package net.sf.jmoney.navigator;

import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.IncomeExpenseAccountInfo;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.resources.Messages;
import net.sf.jmoney.views.CategoriesNode;
import net.sf.jmoney.wizards.NewIncomeExpenseAccountWizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

class NewCategoryAction extends BaseSelectionListenerAction {
	private Shell shell;
	private StructuredViewer viewer;
	private Session session;

	NewCategoryAction(Shell shell, StructuredViewer viewer, Session session) {
		super(null);

		this.shell = shell;
		this.viewer = viewer;
		this.session = session;

		Object[] messageArgs = new Object[] { 
				IncomeExpenseAccountInfo.getPropertySet().getObjectDescription()
		};

		setText(NLS.bind(Messages.AccountsActionProvider_NewAccount, messageArgs));
		setToolTipText(NLS.bind(Messages.AccountsActionProvider_CreateNewAccount, messageArgs));
		setImageDescriptor(IncomeExpenseAccountInfo.getPropertySet().getIconImageDescriptor());
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		if (selection.size() != 1) {
			return false;
		}
		Object selectedObject = selection.getFirstElement();
		return 
		selectedObject instanceof CategoriesNode
		|| selectedObject instanceof IncomeExpenseAccount;
	}

	@Override
	public void run() {
		// This action is enabled only when a single object is selected.
		Object selectedObject = getStructuredSelection().getFirstElement();
		
		IncomeExpenseAccount account = (selectedObject instanceof IncomeExpenseAccount)
			? (IncomeExpenseAccount)selectedObject
					: null;

		NewIncomeExpenseAccountWizard wizard = new NewIncomeExpenseAccountWizard(session, account);
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