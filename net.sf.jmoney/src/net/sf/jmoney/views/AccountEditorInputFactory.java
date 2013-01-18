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

package net.sf.jmoney.views;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.AccountInfo;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.PropertySetNotFoundException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public class AccountEditorInputFactory implements IElementFactory {
	public static final String ID = "net.sf.jmoney.accountEditor"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IElementFactory#createElement(org.eclipse.ui.IMemento)
	 * 
	 */
	// While debugging this code, one can inspect the memento data
	// in the file <runtime-workspace>\.metadata\.plugins\org.eclipse.ui.workbench\workbench.xml.
	@Override
	public IAdaptable createElement(IMemento memento) {
		// Only account object are supported here.
		String fullAccountName = memento.getString("account"); //$NON-NLS-1$
		String accountName = memento.getString("label"); //$NON-NLS-1$
		String propertySetId = memento.getString("propertySet"); //$NON-NLS-1$

		ExtendablePropertySet<? extends Account> propertySet;
		try {
			propertySet = AccountInfo.getPropertySet().getDerivedPropertySet(propertySetId);
		} catch (PropertySetNotFoundException e) {
			/*
			 * The property set is no longer valid. This could happen if the
			 * plug-in that added the property set (account type) is no longer
			 * installed. We return null which means the editor will not be
			 * restored.
			 */
			return null;
		}
		
		if (fullAccountName != null) {
			return new AccountEditorInput(fullAccountName, accountName, propertySet, memento);
		}
		
		// null indicates the element could not be re-created.
		return null;
	}
}