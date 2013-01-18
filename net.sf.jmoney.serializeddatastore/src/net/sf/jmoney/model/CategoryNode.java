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

import net.sf.jmoney.gui.SortedTreeNode;

public class CategoryNode extends SortedTreeNode {

	private static final long serialVersionUID = -1672550964245685408L;

	/**
     * Used by XMLDecoder.
     */
    public CategoryNode() { }

    /**
     * Creates a new category node
     * @param cat a category
     */
    public CategoryNode(Category cat) { super(cat); }

    /**
     * @return the category.
     */
    public Category getCategory() { return (Category) getUserObject(); }

}
