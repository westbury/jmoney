/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2007 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.oda.ui.wizards;

import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.ListPropertyAccessor;

import org.eclipse.ui.IMemento;

/**
 * This class represents nodes in the item list selection tree.
 * There is one instance of this class for each node in the tree,
 * being the data set for each node.
 */
class ObjectList_EntriesInAccount implements IObjectList {
	IObjectList parentItemList;

	public ObjectList_EntriesInAccount(IObjectList parentItemList) {
		this.parentItemList = parentItemList;
	}

	public void save(IMemento memento) {
		IMemento childMemento = memento.createChild("entriesInAccount");
		parentItemList.save(childMemento);
	}

	public boolean isUsed(ListPropertyAccessor listProperty) {
		return parentItemList.isUsed(listProperty);
	}

	public ExtendablePropertySet getPropertySet() {
		return EntryInfo.getPropertySet();
	}
}