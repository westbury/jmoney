package net.sf.jmoney.search.views;

import java.util.List;

import net.sf.jmoney.search.IEntrySearch;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;



/**
 * Invoke the resource creation wizard selection Wizard.
 * This action will retarget to the active view.
 */
class ShowSearchHistoryDialogAction extends Action {
	private SearchView fSearchView;


	/*
	 *	Create a new instance of this class
	 */
	public ShowSearchHistoryDialogAction(SearchView searchView) {
		super("History..."); 
		fSearchView= searchView;
	}
	 
	public void run() {
		List<IEntrySearch> input = fSearchView.getQueries();
		SearchHistorySelectionDialog dlg= new SearchHistorySelectionDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), input, fSearchView);
		
		IEntrySearch current= fSearchView.getCurrentSearch();
		if (current != null) {
			Object[] selected= new Object[1];
			selected[0]= current;
			dlg.setInitialSelections(selected);
		}
		if (dlg.open() == Window.OK) {
			Object[] result= dlg.getResult();
			if (result != null && result.length == 1) {
				IEntrySearch searchResult= (IEntrySearch) result[0];
				if (dlg.isOpenInNewView()) {
					// TODO: open in new view
				} else {
					fSearchView.showSearch(searchResult);
				}
			}
		}

	}
}
