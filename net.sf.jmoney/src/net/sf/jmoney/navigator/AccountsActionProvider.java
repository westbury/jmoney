package net.sf.jmoney.navigator;

import java.util.Vector;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CapitalAccountInfo;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.Session;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.DrillDownAdapter;

public class AccountsActionProvider extends CommonActionProvider {

	private OpenAccountsAction openAction = new OpenAccountsAction();

	private DrillDownAdapter drillDownAdapter;

	private Vector<BaseSelectionListenerAction> newAccountActions = new Vector<BaseSelectionListenerAction>();
	private BaseSelectionListenerAction newCategoryAction;
	private BaseSelectionListenerAction deleteAccountAction;

	private boolean fHasContributedToViewMenu = false;

	@Override
	public void init(ICommonActionExtensionSite site) {
		super.init(site);

		drillDownAdapter = new DrillDownAdapter((TreeViewer) site
				.getStructuredViewer());
		DatastoreManager sessionManager = (DatastoreManager)site.getStructuredViewer().getInput();
		
		makeActions(sessionManager.getSession());
	}

	@Override
	public void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) getContext()
				.getSelection();

		openAction.selectionChanged(selection);
		if (openAction.isEnabled()) {
			manager.insertAfter(ICommonMenuConstants.GROUP_OPEN, openAction);
		}

		manager.add(new Separator());

		for (Action newAccountAction : newAccountActions) {
			if (newAccountAction.isEnabled()) {
				manager.add(newAccountAction);
			}
		}
		if (newCategoryAction.isEnabled()) {
			manager.add(newCategoryAction);
		}
		if (deleteAccountAction.isEnabled()) {
			manager.add(deleteAccountAction);
		}

		manager.add(new Separator());

		drillDownAdapter.addNavigationActions(manager);

		// Other plug-ins can contribute there actions here
		manager.add(new Separator(ICommonMenuConstants.GROUP_ADDITIONS));
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		IStructuredSelection selection = (IStructuredSelection) getContext()
				.getSelection();
		if (selection.size() == 1
				&& selection.getFirstElement() instanceof Account) {
			openAction.selectionChanged(selection);
			actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN,
					openAction);
		}

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
		for (Action newAccountAction : newAccountActions) {
			manager.add(newAccountAction);
		}
		manager.add(newCategoryAction);
		manager.add(new Separator());
		manager.add(deleteAccountAction);
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		drillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions(final Session session) {
		/*
		 * For each class of object derived (directly or indirectly) from the
		 * capital account class, and that is not itself derivable, add a menu
		 * item to create a new account of that type.
		 */
		for (final ExtendablePropertySet<? extends CapitalAccount> derivedPropertySet : CapitalAccountInfo
				.getPropertySet().getDerivedPropertySets()) {

			BaseSelectionListenerAction newAccountAction = new NewAccountAction(getActionSite().getViewSite().getShell(), getActionSite().getStructuredViewer(), derivedPropertySet, session);
			getActionSite().getStructuredViewer().addSelectionChangedListener(newAccountAction);
			newAccountActions.add(newAccountAction);
		}

		newCategoryAction = new NewCategoryAction(getActionSite().getViewSite().getShell(), getActionSite().getStructuredViewer(), session);
		getActionSite().getStructuredViewer().addSelectionChangedListener(newCategoryAction);

		deleteAccountAction = new DeleteAccountAction(session);
		getActionSite().getStructuredViewer().addSelectionChangedListener(deleteAccountAction);
	}
}
