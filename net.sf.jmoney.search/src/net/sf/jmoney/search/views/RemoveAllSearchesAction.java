package net.sf.jmoney.search.views;

import net.sf.jmoney.search.Activator;

import org.eclipse.jface.action.Action;

class RemoveAllSearchesAction extends Action {

	private SearchView fSearchView;

	public RemoveAllSearchesAction(SearchView searchView) {
		super("Clear History"); 
		setImageDescriptor(Activator.imageDescriptorFromPlugin("net.sf.jmoney.search", "icons/elcl16/search_remall.gif"));
		fSearchView= searchView;
	}
	
	public void run() {
		fSearchView.removeAllSearches();
	}
}
