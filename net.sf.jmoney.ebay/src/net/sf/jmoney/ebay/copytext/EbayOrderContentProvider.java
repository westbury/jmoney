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

package net.sf.jmoney.ebay.copytext;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import analyzer.EbayOrder;

public class EbayOrderContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() { // do nothing
		}

		@Override
		public Object[] getChildren(Object element) {
			if (element instanceof EbayOrder) {
				EbayOrder order = (EbayOrder)element;
				return order.getItems().toArray();
			}

			return new Object[0];
		}

		@Override
		public Object[] getElements(Object element) {
			EbayOrder [] orders = (EbayOrder [])element;
			return orders;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof EbayOrder) {
				return true;
			}
			return false;
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { // do nothing
		}
	}
