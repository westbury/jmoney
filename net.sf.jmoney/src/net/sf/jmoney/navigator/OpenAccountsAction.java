package net.sf.jmoney.navigator;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.resources.Messages;
import net.sf.jmoney.views.AccountEditor;
import net.sf.jmoney.views.AccountEditorInput;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.INavigatorContentService;

public class OpenAccountsAction extends BaseSelectionListenerAction {

	public OpenAccountsAction() {
		super(Messages.OpenAccountsAction_Text);
		setToolTipText(Messages.OpenAccountsAction_ToolTipText);
	}

	@Override
	public void run() {
		IStructuredSelection selection = super.getStructuredSelection();
		for (Object selectedObject: selection.toArray()) {
			if (selectedObject instanceof Account) {
				Account account = (Account)selectedObject;

				final IWorkbenchPart dse = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage()
				.findView(JMoneyCommonNavigator.ID);
				if (dse != null) {
					CommonNavigator navigator = (CommonNavigator) dse;
					INavigatorContentService contentService = navigator.getNavigatorContentService();
					String description = contentService.createCommonDescriptionProvider().getDescription(selectedObject);
					ILabelProvider labelProvider = contentService.createCommonLabelProvider();
					Image image = labelProvider.getImage(selectedObject);
					navigator.getViewSite().getActionBars().getStatusLineManager().setMessage(image, description);

					/*
					 * Create an editor for this node (or active if an editor is
					 * already open).
					 */
					try {
						IWorkbenchWindow window = navigator.getViewSite().getWorkbenchWindow();
						IEditorInput editorInput = new AccountEditorInput(account);
						window.getActivePage().openEditor(editorInput, AccountEditor.ID);
					} catch (PartInitException e) {
						JMoneyPlugin.log(e);
					}
				}				
			}
		}
	}
}
