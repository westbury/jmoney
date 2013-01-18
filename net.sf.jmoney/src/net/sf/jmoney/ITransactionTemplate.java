/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2006 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney;

import java.util.Collection;

import net.sf.jmoney.isolation.IObjectKey;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Interface that must be implemented by all implementations of a transaction
 * type in the tabbed transaction edit area.
 * 
 * Some data entry views have tabbed controls with a tab for each
 * type of transaction.  Plug-ins can add further types of transactions,
 * allowing for rapid data entry.  All such implementations of a tabbed
 * control must implement this interface.
 * 
 * @author Nigel Westbury
 *
 */
public interface ITransactionTemplate {

	void addTransaction(Collection<IObjectKey> ourEntryList);

	String getDescription();

	/**
	 * Not all templates are applicable in all situations.  This method
	 * is called to determine if a template is applicable.  A tab is created
	 * for the template only if the template is applicable.
	 * 
	 * @param account the account being listed in the editor view, or null if
	 * 			the editor view is of a type that does not display information
	 * 			for an account
	 * @return true if the template is applicable, false if not applicable
	 */
	boolean isApplicable(Account account);
	
	/**
	 * Templates may be used to edit existing transactions.  This method loads
	 * an existing entry into the controls.  
	 * 
	 * This method will succeed only if the entry is in a transaction that exactly
	 * matches the template.  If the transaction does not exactly match the template
	 * then none of the controls will be set and false will be returned.
	 * 
	 * If Not all templates are applicable in all situations.  This method
	 * is called to determine if a template is applicable.  A tab is created
	 * for the template only if the template is applicable.
	 * 
	 * @param account the account being listed in the editor view, or null if
	 * 			the editor view is of a type that does not display information
	 * 			for an account
	 * @param entry the entry that was selected to be edited.  If account is not
	 * 			null then this entry will always be in the given account
	 * @return true if the entry matched the template, false otherwise
	 */
	boolean loadEntry(Account account, Entry entry);
	
	/**
	 * 
	 * @param parent
	 * @param session 
	 * @param expandedControls true if speed of user entry is more important,
	 * 			false if conservation of screen space is more important.
	 * 			For example, a list box may be appropriate when expandedControls
	 * 			is true while a drop-down combo box may be appropriate when
	 * 			expandedControls is false
	 * @param account the account into which the entry is to be added,
	 * 			or null if the account is not known and must be entered
	 * 			by the user 
	 * @return
	 */
	Control createControl(Composite parent, Session session, boolean expandedControls, Account account, Collection<IObjectKey> ourEntryList);

	void init(IDialogSettings section);
	void saveState(IDialogSettings section);
}
