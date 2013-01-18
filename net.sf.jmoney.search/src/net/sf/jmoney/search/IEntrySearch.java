package net.sf.jmoney.search;

import java.util.List;

import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.search.views.SearchException;

import org.eclipse.jface.resource.ImageDescriptor;

public interface IEntrySearch {

	String getLabel();

	ImageDescriptor getImageDescriptor();

	// TODO: rename: used for view content description
	String getTooltip();

	boolean isQueryRunning();

	List<Entry> getEntries();

	void executeSearch() throws SearchException;

}
