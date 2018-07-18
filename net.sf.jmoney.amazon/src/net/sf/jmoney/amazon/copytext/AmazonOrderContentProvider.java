/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2010 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.amazon.copytext;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import analyzer.AmazonOrder;
import analyzer.AmazonShipment;

public class AmazonOrderContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() { // do nothing
		}

		@Override
		public Object[] getChildren(Object element) {
			if (element instanceof AmazonOrder) {
				AmazonOrder order = (AmazonOrder)element;
				if (order.getShipments().size() == 1) {
					return order.getShipments().get(0).getItems().toArray();
				} else {
					return order.getShipments().toArray();
				}
			} else if (element instanceof AmazonShipment) {
				AmazonShipment shipment = (AmazonShipment)element;
				return shipment.getItems().toArray();
			}

			return new Object[0];
		}

		@Override
		public Object[] getElements(Object element) {
			AmazonOrder [] orders = (AmazonOrder [])element;
			return orders;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof AmazonOrder) {
				return true;
			} else if (element instanceof AmazonShipment) {
				return true;
			}
			return false;
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { // do nothing
		}
	}
