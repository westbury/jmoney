package net.sf.jmoney.stocks.navigator;

import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.Session;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.eclipse.ui.operations.UndoRedoActionGroup;

public class SecuritiesActionProvider extends CommonActionProvider {

	private BaseSelectionListenerAction deleteStockAction;

	private boolean fHasContributedToViewMenu = false;

	@Override
	public void init(ICommonActionExtensionSite site) {
		super.init(site);

		DatastoreManager sessionManager = (DatastoreManager)site.getStructuredViewer().getInput();
		
		makeActions(sessionManager.getSession());
	}

	@Override
	public void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) getContext()
				.getSelection();

		deleteStockAction.selectionChanged(selection);
		if (deleteStockAction.isEnabled()) {
			manager.add(deleteStockAction);
		}

		// Other plug-ins can contribute there actions here
		manager.add(new Separator(ICommonMenuConstants.GROUP_ADDITIONS));
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		IStructuredSelection selection = (IStructuredSelection) getContext()
				.getSelection();

		if (!fHasContributedToViewMenu) {
			fillLocalPullDown(actionBars.getMenuManager());
			fillLocalToolBar(actionBars.getToolBarManager());
			fHasContributedToViewMenu = true;
		}

		/*
		 * This action provider is used for navigation trees in view parts only.
		 * The CommonViewerSite object will therefore in fact implement
		 * ICommonViewerWorkbenchSite. ICommonViewerWorkbenchSite would not be
		 * implemented if the tree is in a dialog.
		 */
		ICommonViewerWorkbenchSite site = (ICommonViewerWorkbenchSite) getActionSite()
				.getViewSite();

		ActionGroup undoRedoActionGroup = new UndoRedoActionGroup(site
				.getSite(), site.getWorkbenchWindow().getWorkbench()
				.getOperationSupport().getUndoContext(), true);
		undoRedoActionGroup.fillActionBars(actionBars);
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(deleteStockAction);
	}

	private void fillLocalToolBar(IToolBarManager manager) {
	}

	private void makeActions(final Session session) {
		deleteStockAction = new DeleteSecurityAction(session);
		getActionSite().getStructuredViewer().addSelectionChangedListener(deleteStockAction);
	}
}
