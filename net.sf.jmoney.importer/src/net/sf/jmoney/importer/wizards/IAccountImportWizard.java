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

package net.sf.jmoney.importer.wizards;

import java.util.List;

import net.sf.jmoney.associations.AssociationMetadata;
import net.sf.jmoney.importer.matcher.BaseEntryData;
import net.sf.jmoney.importer.model.TransactionType;
import net.sf.jmoney.model2.Account;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.ui.IWorkbenchWindow;

public interface IAccountImportWizard<T extends BaseEntryData> extends IWizard {

	void init(IWorkbenchWindow window, Account account);

	AssociationMetadata[] getAssociationMetadata();

	List<TransactionType<T>> getApplicableTransactionTypes();

	/**
	 * This form is used when in a context where a date range is
	 * known.  For example when importing a bank statement then by default
	 * the date range will be restricted to the range of dates covered
	 * by the bank statement.
	 * 
	 * @param workbenchWindow
	 * @param account
	 * @param defaultStartDate
	 * @param defaultEndDate
	 */
//	void init(IWorkbenchWindow workbenchWindow, CurrencyAccount account,
//			Date defaultStartDate, Date defaultEndDate);
}
