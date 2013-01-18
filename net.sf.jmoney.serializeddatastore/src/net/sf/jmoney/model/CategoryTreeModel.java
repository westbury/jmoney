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

package net.sf.jmoney.model;

import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sf.jmoney.gui.SortedTreeModel;

/**
 * The Category model.
 */
public class CategoryTreeModel extends SortedTreeModel {

	private static final long serialVersionUID = 6457332494138567318L;

	protected CategoryNode rootNode = (new RootCategory()).getCategoryNode();

	protected CategoryNode transferNode =
		(new TransferCategory()).getCategoryNode();

	protected CategoryNode splitNode = (new SplitCategory()).getCategoryNode();

	/**
	 * Used by XMLDecoder.
	 */
	public CategoryTreeModel() {
		super(new DefaultMutableTreeNode());
		setRoot(rootNode);
	}

	/**
	 * Creates a new CategoryTreeModel.
	 */
	public CategoryTreeModel(int dummy) {
		this();

		// This cannot be done in the parameterless constructor above,
		// XMLDecoder would add those nodes twice:
		rootNode.add(transferNode);
		rootNode.add(splitNode);
	}

	/**
	 * @return the root node.
	 */
	public CategoryNode getRootNode() {
		return rootNode;
	}

	/**
	 * @return the transfer node.
	 */
	public CategoryNode getTransferNode() {
		return transferNode;
	}

	/**
	 * @return the split node.
	 */
	public CategoryNode getSplitNode() {
		return splitNode;
	}

	public void setRootNode(CategoryNode aRootNode) {
		rootNode = aRootNode;
		setRoot(aRootNode);
	}

	public void setSplitNode(CategoryNode aSplitNode) {
		splitNode = aSplitNode;
	}

	public void setTransferNode(CategoryNode aTransferNode) {
		transferNode = aTransferNode;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		EventListenerList tmp = listenerList;
		listenerList = new EventListenerList();
		out.defaultWriteObject();
		listenerList = tmp;
	}

}
