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

package net.sf.jmoney.reconciliation;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import net.sf.jmoney.importer.matcher.EntryData;
import net.sf.jmoney.importer.matcher.ImportEntryProperty;
import net.sf.jmoney.importer.model.TransactionType;
import net.sf.jmoney.model2.CurrencyAccount;

import org.eclipse.swt.widgets.Shell;

/**
 * An implementation of this interface must be provided by all
 * extensions to the net.sf.jmoney.bankstatements extension point.
 * 
 * @author Nigel Westbury
 */
public interface IBankStatementSource {

	/**
	 * @param account
	 *            the account into which the entries are being imported.
	 *            Implementations of this method do not generally need to know
	 *            the account because the entry data are returned in a
	 *            collection of EntryData objects and it is up to the caller to
	 *            merge the data into the datastore. However, there are
	 *            instances where information from the account is needed. For
	 *            example, knowing the currency of the account may affect the
	 *            way amounts are interpreted or implementations may add
	 *            properties to the account objects that affect the import
	 *            process.
	 * @param defaultEndDate 
	 * @param defaultStartDate 
	 * @return a collection of EntryData objects if entries are available for
	 *         importing, or null if the user cancelled the operation or if an
	 *         error occured.
	 */
	Collection<EntryData> importEntries(Shell shell, CurrencyAccount account, Date defaultStartDate, Date defaultEndDate);

	ImportEntryProperty[] getImportEntryProperties();

	List<TransactionType> getApplicableTransactionTypes();
}
