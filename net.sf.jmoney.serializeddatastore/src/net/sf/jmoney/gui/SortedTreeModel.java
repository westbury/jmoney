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

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

public class SortedTreeModel extends DefaultTreeModel {

	private static final long serialVersionUID = -7070395954201502578L;

	public SortedTreeModel(TreeNode root) {
		super(root, false);
	}

	@Override
	public void insertNodeInto(
		MutableTreeNode newChild,
		MutableTreeNode parent,
		int index) {
		parent.insert(newChild, 0);
		nodeStructureChanged(parent);
	}

	public void sortChildren(SortedTreeNode parent) {
		parent.sortChildren();
		nodeStructureChanged(parent);
	}

}
