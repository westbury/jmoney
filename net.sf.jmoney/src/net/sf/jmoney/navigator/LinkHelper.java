package net.sf.jmoney.navigator;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.views.AccountEditorInput;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.navigator.ILinkHelper;

public class LinkHelper implements ILinkHelper {

	@Override
	public void activateEditor(IWorkbenchPage page,	IStructuredSelection selection) {
	    if (selection == null || selection.isEmpty()) {
            return;
        }
        Object element = selection.getFirstElement();
        if (element instanceof Account) {
        	Account account = (Account)element;

        	IEditorInput procInput = new AccountEditorInput(account);
        	IEditorPart editor = page.findEditor(procInput);
        	if (editor != null) {
        		page.bringToTop(editor);
        	}
        }
	}

	@Override
	public IStructuredSelection findSelection(IEditorInput input) {
        if (input instanceof AccountEditorInput) {
            String accountName = ((AccountEditorInput)input).getFullAccountName();
            Account account = JMoneyPlugin.getDefault().getSession().getAccountByFullName(accountName);	
            return new StructuredSelection(account);
        }
        return StructuredSelection.EMPTY;
 	}

}
