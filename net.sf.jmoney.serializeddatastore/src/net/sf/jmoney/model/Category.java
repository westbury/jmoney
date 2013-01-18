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

import java.io.Serializable;

/**
 * Interface for those classes that can be a category of an entry.
 */
public interface Category extends Serializable, Comparable {

	/**
	 * @return the name of the category.
	 */
	public String getCategoryName();

	/**
	 * @return the full qualified name of the category.
	 */
	public String getFullCategoryName();

	/**
	 * @return the node that will be used to insert the category into the tree.
	 */
	public CategoryNode getCategoryNode();

}
