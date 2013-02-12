/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2009 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.stocks;

import java.text.MessageFormat;

import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.SessionInfo;
import net.sf.jmoney.model2.TransactionManagerForAccounts;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.model.SecurityInfo;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class MergeDuplicatedSecurityHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);
		IStructuredSelection selection = (IStructuredSelection)HandlerUtil.getCurrentSelectionChecked(event);
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		Security security1 = (Security)selection.toList().get(0);
		Security security2 = (Security)selection.toList().get(1);

		ExtendablePropertySet<? extends Security> propertySet1 = SecurityInfo.getPropertySet().getActualPropertySet(security1.getClass());
		ExtendablePropertySet<? extends Security> propertySet2 = SecurityInfo.getPropertySet().getActualPropertySet(security2.getClass());

		if (propertySet1 != propertySet2) {
			throw new ExecutionException(MessageFormat.format("The two securities cannot be merged because they are of types {0} and {1}.  Only securities of the same type can be merged.", propertySet1.getObjectDescription(), propertySet1.getObjectDescription()));
		}

		processSecuritiesOfSameType(
				shell,
				window,
				propertySet1,
				security1,
				security2);

		return null;
	}

	private <S extends Security> void processSecuritiesOfSameType(Shell shell, IWorkbenchWindow window, ExtendablePropertySet<S> extendablePropertySet, Security security1, Security security2) {
		S typedSecurity1 = extendablePropertySet.getImplementationClass().cast(security1);
		S typedSecurity2 = extendablePropertySet.getImplementationClass().cast(security2);

		IDataManagerForAccounts sessionManager = (IDataManagerForAccounts)window.getActivePage().getInput();

		TransactionManagerForAccounts transaction = new TransactionManagerForAccounts(sessionManager);

		int result = new MergeDuplicatedSecurityDialog<S>(
				shell,
				extendablePropertySet,
				transaction.getCopyInTransaction(typedSecurity1),
				transaction.getCopyInTransaction(typedSecurity2))
				.open();
		if (result == IDialogConstants.OK_ID) {
			replaceSecondWithFirst(transaction, SessionInfo.getPropertySet(), transaction.getSession(), extendablePropertySet, typedSecurity1, typedSecurity2);
			transaction.commit("Merge Securities");
		}
	}

	private <S extends Security, E extends ExtendableObject> void replaceSecondWithFirst(TransactionManager transaction,
			ExtendablePropertySet<E> extendablePropertySet, E session,
			ExtendablePropertySet<S> propertySet1,
			S security1, S security2) {

		/*
		 * Look through the scalar values for references to the second security.
		 * Replace with references to the first.
		 */
		for (ScalarPropertyAccessor<?,? super E> scalarAccessor: extendablePropertySet.getScalarProperties3()) {
			ScalarPropertyAccessor<? super S,? super E> x = scalarAccessor.typeIfGivenValue(session, security2);
			if (x != null) {
				x.setValue(session, security1);
			}
		}

		/*
		 * Pass through all the list properties
		 */

		for (ListPropertyAccessor<?,? super E> listAccessor: extendablePropertySet.getListProperties3()) {
			processChildElement(transaction, session,
					propertySet1, security1, security2, listAccessor);
		}

	}

	private <S extends Security, E extends ExtendableObject, C extends ExtendableObject> void processChildElement(TransactionManager transaction,
			E thisExtendableObject,
			ExtendablePropertySet<S> securityPropertySet, S security1,
			S security2, ListPropertyAccessor<C,? super E> listAccessor) {
		ExtendablePropertySet<C> childPropertySet = listAccessor.getElementPropertySet();
		ObjectCollection<C> children = listAccessor.getElements(thisExtendableObject);
		for (C childExtendableObject : children) {
			replaceSecondWithFirst(transaction, childPropertySet, childExtendableObject,
					securityPropertySet, security1, security2);
		}
	}
}