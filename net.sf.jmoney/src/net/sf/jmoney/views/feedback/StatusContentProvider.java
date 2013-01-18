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

package net.sf.jmoney.views.feedback;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class StatusContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() { // do nothing
		}

		@Override
		public Object[] getChildren(Object element) {
			IStatus status = (IStatus)element;
			return status.getChildren();
		}

		@Override
		public Object[] getElements(Object element) {
			/*
			 * If the input is a single status (not a multi-status)
			 * then we return that as the element.  It would be more
			 * consistent if we just returned an empty list, as the
			 * root status is not normally displayed in the tree,
			 * but for time being, leave it like this.
			 */
			IStatus status = (IStatus)element;
			if (status.isMultiStatus()) {
				return status.getChildren();
			} else {
				return new Object [] { status };
			}
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			IStatus status = (IStatus)element;
			return status.getChildren().length != 0;
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { // do nothing
		}
	}
