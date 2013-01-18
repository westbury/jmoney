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

package net.sf.jmoney.fields;

import java.util.LinkedList;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Session;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;

public class AccountControlWithMruList<A extends Account> extends AccountComposite<A> {
	protected Session session;
    protected List accountList;
    protected AccountControl<A> accountControl;
    protected Class<A> accountClass;
    
    protected LinkedList<A> recentlyUsedList = new LinkedList<A>();
    
	public AccountControlWithMruList(Composite parent, Session session, Class<A> accountClass) {
		super(parent, SWT.NONE);
		this.session = session;
		this.accountClass = accountClass;
		
		setLayout(new GridLayout(1, false));
		
        accountList = new List(this, SWT.NONE);
        accountControl = new AccountControl<A>(this, session, accountClass);

        accountList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        accountControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        accountList.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				int selection = accountList.getSelectionIndex();
				if (selection >= 0) {
					accountControl.setAccount(recentlyUsedList.get(selection));
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
	}

    @Override	
	public void rememberChoice() {
    	A account = accountControl.getAccount();
    	if (account != null) {
    		if (recentlyUsedList.size() != 0) {
    			int index = recentlyUsedList.indexOf(account);
    			if (index == -1) {
    				// Drop off head if list is already full
    	    		if (recentlyUsedList.size() >= 10) {
    	    			recentlyUsedList.removeFirst();
    	    			accountList.remove(0);
    	    		}
    			} else {
    				recentlyUsedList.remove(account);
    				accountList.remove(index);
    			}
    		}
    		recentlyUsedList.addLast(account);
    		accountList.add(account.getName());
    	}
	}

    @Override	
	public A getAccount() {
	    return accountControl.getAccount();
	}
	
    @Override	
	public void setAccount(A account) {
		accountControl.setAccount(account);
	}

    @Override	
	public void init(IDialogSettings section) {
		if (section != null) {
			String [] mruAccountNames = section.getArray("mruAccount"); //$NON-NLS-1$
			for (String fullAccountName : mruAccountNames) {
				Account account = session.getAccountByFullName(fullAccountName);
				if (accountClass.isInstance(account)) {
					recentlyUsedList.addLast(accountClass.cast(account));
					accountList.add(account.getName());
				}
			}
		}
	}

	@Override
	public void saveState(IDialogSettings section) {
		String [] mruAccountNames = new String[recentlyUsedList.size()];
		int i = 0;
		for (Account account: recentlyUsedList) {
			mruAccountNames[i++] = account.getFullAccountName();
		}
		section.put("mruAccount", mruAccountNames); //$NON-NLS-1$
	}
}
