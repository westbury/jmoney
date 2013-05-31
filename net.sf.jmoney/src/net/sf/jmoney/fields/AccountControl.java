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

package net.sf.jmoney.fields;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Session;

import org.eclipse.core.databinding.bind.Bind;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


/**
 * A control for entering accounts.
 *
 * This control contains both a text box and a list box that appears when the
 * text box gains focus.
 *
 * @author Nigel Westbury
 */
public class AccountControl<A extends Account> extends AccountComposite<A> {

	Text textControl;

	private Session session;
	private Class<A> accountClass;

    /**
     * List of accounts put into account list.
     */
    private Vector<A> allAccounts;

	/**
	 * Currently selected account, or null if no account selected
	 */
	public WritableValue<A> account = new WritableValue<A>();

	/**
	 * @param parent
	 * @param style
	 */
	public AccountControl(final Composite parent, Session session, Class<A> accountClass) {
		super(parent, SWT.NONE);
		this.session = session;
		this.accountClass = accountClass;

		setBackgroundMode(SWT.INHERIT_FORCE);

		setLayout(new FillLayout(SWT.VERTICAL));

		textControl = new Text(this, SWT.LEFT);

		IConverter<A,String> accountToTextConverter = new Converter<A,String>(Commodity.class, String.class) {
			@Override
			public String convert(A account) {
				return (account == null) ? "" : account.getName();
			}
		};
		Bind.oneWay(account)
			.convert(accountToTextConverter)
			.to(SWTObservables.observeText(textControl, SWT.Modify));

		textControl.addFocusListener(new FocusListener() {

			Shell shell;
			boolean closingShell = false;

			@Override
			public void focusGained(FocusEvent e) {
				if (closingShell) {
					return;
				}

				shell = new Shell(parent.getShell(), SWT.ON_TOP);
		        shell.setLayout(new RowLayout());

		        final List listControl = new List(shell, SWT.SINGLE | SWT.V_SCROLL);
		        listControl.setLayoutData(new RowData(SWT.DEFAULT, 100));

		        // Important we use the field for the session and accountClass.  We do not use the parameters
		        // (the parameters may be null, but fields should always have been set by
		        // the time control gets focus).
		        allAccounts = new Vector<A>();
		        addAccounts("", AccountControl.this.session.getAccountCollection(), listControl, AccountControl.this.accountClass); //$NON-NLS-1$

//		        shell.setSize(listControl.computeSize(SWT.DEFAULT, listControl.getItemHeight()*10));

                // Set the currently set account into the list control.
    	        listControl.select(allAccounts.indexOf(account));

    	        listControl.addSelectionListener(
                		new SelectionAdapter() {
                		    @Override
							public void widgetSelected(SelectionEvent e) {
								int selectionIndex = listControl.getSelectionIndex();
								account.setValue(allAccounts.get(selectionIndex));
							}
                		});

    			listControl.addKeyListener(new KeyAdapter() {
    				String pattern;
    				int lastTime = 0;

    			    @Override
    				public void keyPressed(KeyEvent e) {
    					if (Character.isLetterOrDigit(e.character)) {
    						if ((e.time - lastTime) < 1000) {
    							pattern += Character.toUpperCase(e.character);
    						} else {
    							pattern = String.valueOf(Character.toUpperCase(e.character));
    						}
    						lastTime = e.time;

    						/*
    						 *
    						 Starting at the currently selected account,
    						 search for an account starting with these characters.
    						 */
    						int startIndex = listControl.getSelectionIndex();
    						if (startIndex == -1) {
    							startIndex = 0;
    						}

    						int match = -1;
    						int i = startIndex;
    						do {
    							if (allAccounts.get(i).getName().toUpperCase().startsWith(pattern)) {
    								match = i;
    								break;
    							}

    							i++;
    							if (i == allAccounts.size()) {
    								i = 0;
    							}
    						} while (i != startIndex);

    						if (match != -1) {
    							account.setValue(allAccounts.get(match));
    							listControl.select(match);
    							listControl.setTopIndex(match);
    						}

    						e.doit = false;
    					}
    				}
    			});

    			shell.pack();

    	        /*
				 * Position the shell below the text box, unless the account
				 * control is so near the bottom of the display that the shell
				 * would go off the bottom of the display, in which case
				 * position the shell above the text box.
				 */
    	        Display display = getDisplay();
    	        Rectangle rect = display.map(parent, null, getBounds());
    	        int calendarShellHeight = shell.getSize().y;
    	        if (rect.y + rect.height + calendarShellHeight <= display.getBounds().height) {
        	        shell.setLocation(rect.x, rect.y + rect.height);
    	        } else {
        	        shell.setLocation(rect.x, rect.y - calendarShellHeight);
    	        }

    	        shell.open();

    	        shell.addShellListener(new ShellAdapter() {
    			    @Override
    	        	public void shellDeactivated(ShellEvent e) {
    	        		closingShell = true;
    	        		shell.close();
    	        		closingShell = false;
    	        	}
    	        });
			}

			@Override
			public void focusLost(FocusEvent e) {
//        		shell.close();
 //       		listControl = null;
			}
		});
	}

	private void addAccounts(String prefix, Collection<? extends Account> accounts, List listControl, Class<A> accountClass) {
    	Vector<A> matchingAccounts = new Vector<A>();
        for (Account account: accounts) {
        	if (accountClass.isAssignableFrom(account.getClass())) {
        		matchingAccounts.add(accountClass.cast(account));
        	}
        }

		// Sort the accounts by name.
		Collections.sort(matchingAccounts, new Comparator<Account>() {
			@Override
			public int compare(Account account1, Account account2) {
				return account1.getName().compareTo(account2.getName());
			}
		});

		for (A matchingAccount: matchingAccounts) {
    		allAccounts.add(matchingAccount);
			listControl.add(prefix + matchingAccount.getName());
    		addAccounts(prefix + matchingAccount.getName() + ":", matchingAccount.getSubAccountCollection(), listControl, accountClass); //$NON-NLS-1$
		}

    }

    /**
	 * @param object
	 */
	@Override
	public void setAccount(A account) {
		this.account.setValue(account);
	}

	/**
	 * @return the account, or null if no account has been set in
	 * 				the control
	 */
    @Override
	public A getAccount() {
		return account.getValue();
	}

	public Control getControl() {
		return this;
	}

    @Override
	public void rememberChoice() {
		// We don't remember choices, so nothing to do
	}

    @Override
	public void init(IDialogSettings section) {
		// No state to restore
	}

    @Override
	public void saveState(IDialogSettings section) {
		// No state to save
	}

	/**
	 * Normally the session is set through the constructor. However in some
	 * circumstances (i.e. in the custom cell editors) the session is not
	 * available at construction time and null will be set. This method must
	 * then be called to set the session before the control is used (i.e. before
	 * the control gets focus).
	 */
	public void setSession(Session session, Class<A> accountClass) {
		this.session = session;
		this.accountClass = accountClass;
	}
}