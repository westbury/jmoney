/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.property.views;

import java.util.ArrayList;
import java.util.Collection;

import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.property.model.RealProperty;
import net.sf.jmoney.views.IDynamicTreeNode;

public class RealPropertyTypeNode implements IDynamicTreeNode {

	private DatastoreManager sessionManager;
	private ExtendablePropertySet<? extends RealProperty> propertySet;
	
	public RealPropertyTypeNode(DatastoreManager sessionManager, ExtendablePropertySet<? extends RealProperty> propertySet) {
		this.sessionManager = sessionManager;
		this.propertySet = propertySet;
	}

	public boolean hasChildren() {
		Session session = sessionManager.getSession();
		for (Commodity commodity: session.getCommodityCollection()) {
			if (commodity.getClass() == propertySet.getImplementationClass()) {
				return true;
			}
		}
		return false;
	}

	public Collection<Object> getChildren() {
		Session session = sessionManager.getSession();
		ArrayList<Object> children = new ArrayList<Object>();
		for (Commodity commodity: session.getCommodityCollection()) {
			if (commodity.getClass() == propertySet.getImplementationClass()) {
				children.add(commodity);
			}
		}
		return children;
	}

	public String getLabel() {
		return propertySet.getObjectDescription();
	}
}

