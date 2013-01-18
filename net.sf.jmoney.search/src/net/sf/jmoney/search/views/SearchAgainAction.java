package net.sf.jmoney.search.views;

import net.sf.jmoney.search.Activator;
import net.sf.jmoney.search.IEntrySearch;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;

class SearchAgainAction extends Action {
	private SearchView fView;
	
	public SearchAgainAction(SearchView view) {
		setToolTipText("Run the Current Search Again"); 
		setImageDescriptor(Activator.imageDescriptorFromPlugin("net.sf.jmoney.search", "icons/elcl16/search_again.gif"));
		fView = view;	
	}

	public void run() {
		// This action should not be enabled if there is no search.
		final IEntrySearch search = fView.getCurrentSearch();
		try {
			search.executeSearch();
			fView.showSearch(search);
		} catch (SearchException e) {
			MessageDialog.openError(fView.getSite().getShell(), "Search Error", e.getLocalizedMessage());
		}
	}
}
