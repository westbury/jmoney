package net.sf.jmoney.stocks.gains;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;


public class ActivityMap extends TreeMap<ActivityKey, Collection<ActivityNode>> {
	private static final long serialVersionUID = 1L;

	void add(ActivityNode node) {
		Collection<ActivityNode> nodesWithGivenKey = get(node.getKey());
		if (nodesWithGivenKey == null) {
			nodesWithGivenKey = new ArrayList<ActivityNode>();
			put(node.getKey(), nodesWithGivenKey);
		}
		nodesWithGivenKey.add(node);
	}
}