package net.sf.jmoney.search.views;

import java.util.List;

import net.sf.jmoney.search.Activator;
import net.sf.jmoney.search.IEntrySearch;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

class SearchHistoryDropDownAction extends Action implements IMenuCreator {

	private class ShowSearchFromHistoryAction extends Action {
		private IEntrySearch fSearch;

		public ShowSearchFromHistoryAction(IEntrySearch search) {
	        super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			fSearch= search;
			
			String label= escapeAmp(search.getLabel());

			// fix for bug 38049
			// TODO: Try searching for something containing '@' in memo and
			// see if this is necessary.
			if (label.indexOf('@') >= 0)
				label+= '@';
			setText(label);
			setImageDescriptor(search.getImageDescriptor());
			setToolTipText(search.getTooltip());
		}
		
		private String escapeAmp(String label) {
			StringBuffer buf= new StringBuffer();
			for (int i= 0; i < label.length(); i++) {
				char ch= label.charAt(i);
				buf.append(ch);
				if (ch == '&') {
					buf.append('&');
				}
			}
			return buf.toString();
		}
		
//		public void runWithEvent(Event event) {
//			InternalSearchUI.getInstance().showSearchResult(fSearchView, fSearch, event.stateMask == SWT.CTRL);
//		}
		
		public void run() {
			fSearchView.showSearch(fSearch);
		}
	}

	public static final int RESULTS_IN_DROP_DOWN= 10;

	private Menu fMenu;
	private SearchView fSearchView;
	
	public SearchHistoryDropDownAction(SearchView searchView) {
		setToolTipText("Show Previous Searches"); 
		setImageDescriptor(Activator.imageDescriptorFromPlugin("net.sf.jmoney.search", "icons/elcl16/search_history.gif"));
		fSearchView= searchView;
		setMenuCreator(this);
	}
	
	public void updateEnablement() {
		boolean hasQueries = fSearchView.hasQueries();
		setEnabled(hasQueries);
	}

	public void dispose() {
		disposeMenu();
	}

	void disposeMenu() {
		if (fMenu != null)
			fMenu.dispose();
	}

	public Menu getMenu(Menu parent) {
		return null;
	}

	public Menu getMenu(Control parent) {
		IEntrySearch currentSearch = fSearchView.getCurrentSearch();
		disposeMenu();
		
		fMenu= new Menu(parent);
				
		List<IEntrySearch> searches= fSearchView.getQueries();
		if (searches.size() > 0) {			
			for (IEntrySearch search : searches) {
				ShowSearchFromHistoryAction action= new ShowSearchFromHistoryAction(search);
				action.setChecked(search.equals(currentSearch));
				addActionToMenu(fMenu, action);
			}
			new MenuItem(fMenu, SWT.SEPARATOR);
			addActionToMenu(fMenu, new ShowSearchHistoryDialogAction(fSearchView));
			addActionToMenu(fMenu, new RemoveAllSearchesAction(fSearchView));
		}
		return fMenu;
	}

	protected void addActionToMenu(Menu parent, Action action) {
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(parent, -1);
	}
	
	public void run() {
		new ShowSearchHistoryDialogAction(fSearchView).run();
	}
}
