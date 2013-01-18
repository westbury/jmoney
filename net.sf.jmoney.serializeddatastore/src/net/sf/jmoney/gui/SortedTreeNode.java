/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package net.sf.jmoney.gui;

import java.util.Collections;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class SortedTreeNode
	extends DefaultMutableTreeNode
	implements Comparable {

	private static final long serialVersionUID = -32238679169645975L;

	public SortedTreeNode() {
	}

	public SortedTreeNode(Object usrObj) {
		super(usrObj, true);
	}

	public int compareTo(Object o) {
		return toString().compareTo(o.toString());
	}

	@Override
	public void insert(MutableTreeNode child, int index) {
		super.insert(child, index);
		sortChildren();
	}

	@SuppressWarnings("unchecked")
	public void sortChildren() {
		if (children == null)
			return;
		Collections.sort(children);
	}

}
