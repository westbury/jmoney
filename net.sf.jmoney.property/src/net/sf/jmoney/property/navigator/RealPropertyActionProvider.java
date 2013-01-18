package net.sf.jmoney.property.navigator;

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

public class RealPropertyActionProvider extends CommonActionProvider {

	private BaseSelectionListenerAction newRealPropertyAction;
	
	private BaseSelectionListenerAction deleteRealPropertyAction;

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

		newRealPropertyAction.selectionChanged(selection);
		if (newRealPropertyAction.isEnabled()) {
			manager.add(newRealPropertyAction);
		}

		deleteRealPropertyAction.selectionChanged(selection);
		if (deleteRealPropertyAction.isEnabled()) {
			manager.add(deleteRealPropertyAction);
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
		manager.add(newRealPropertyAction);
		manager.add(deleteRealPropertyAction);
	}

	private void fillLocalToolBar(IToolBarManager manager) {
	}

	private void makeActions(final Session session) {
		ICommonViewerWorkbenchSite site = (ICommonViewerWorkbenchSite) getActionSite()
		.getViewSite();

		newRealPropertyAction = new NewRealPropertyAction(site.getShell(), site.getSelectionProvider(), session);
		getActionSite().getStructuredViewer().addSelectionChangedListener(newRealPropertyAction);

		deleteRealPropertyAction = new DeleteRealPropertyAction(session);
		getActionSite().getStructuredViewer().addSelectionChangedListener(deleteRealPropertyAction);
	}
}
