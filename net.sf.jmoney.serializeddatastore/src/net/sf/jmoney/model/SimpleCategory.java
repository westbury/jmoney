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

/**
 * An implementation of the Category interface
 */
public class SimpleCategory extends AbstractCategory {

	private static final long serialVersionUID = 3057882967522350415L;

	private String categoryName;

	private String fullCategoryName = null;

	public SimpleCategory() {
	}

	public SimpleCategory(String aCategoryName) {
		setCategoryName(aCategoryName);
	}

	public String getCategoryName() {
		return categoryName;
	}

	@Override
	public String getFullCategoryName() {
		if (fullCategoryName == null) {
			Object[] path = getCategoryNode().getUserObjectPath();
			if (path.length > 1) {
				fullCategoryName = path[1].toString();
				for (int i = 2; i < path.length; i++)
					fullCategoryName += ":" + path[i]; //$NON-NLS-1$
			} else
				fullCategoryName = categoryName;
		}
		return fullCategoryName;
	}

	public void setCategoryName(String aCategoryName) {
		categoryName = aCategoryName;
		fullCategoryName = null;
	}

}
