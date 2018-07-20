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

package net.sf.jmoney.stocks.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.core.internal.databinding.provisional.bind.IConverter;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.stocks.wizards.NewStockWizard;


/**
 * A control for selecting a security (such as a stock or a bond).
 * <P>
 * This control contains both a text box and a list box that appears when the
 * text box gains focus.
 *
 * @author Nigel Westbury
 */
public abstract class SecurityControl<A extends Security> extends Composite {

	Text textControl;

//	private Session session;
	private ExtendablePropertySet<A> securityPropertySet;

    /**
     * List of securities put into security list.
     */
    private Vector<A> allSecurities;

	/**
	 * Currently selected commodity, or null if no commodity selected
	 */
	public final WritableValue<A> commodity = new WritableValue<A>();

	/**
	 * @param parent
	 * @param style
	 */
	public SecurityControl(final Composite parent, final ExtendablePropertySet<A> securityClass) {
		super(parent, SWT.NONE);
		this.securityPropertySet = securityClass;

		setBackgroundMode(SWT.INHERIT_FORCE);

		setLayout(new FillLayout(SWT.VERTICAL));

		textControl = new Text(this, SWT.LEFT);

		IConverter<A,String> commodityToTextConverter = new IConverter<A,String>() {
			@Override
			public String convert(A commodity) {
				return (commodity == null) ? "" : commodity.getName();
			}
		};
		Bind.oneWay(commodity)
			.convert(commodityToTextConverter)
			.to(SWTObservables.observeText(textControl, SWT.Modify));

		textControl.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					openSecuritiesShell(parent, securityClass);
				}

			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub

			}
		});

//		textControl.addFocusListener(new FocusAdapter() {
//
//			@Override
//			public void focusGained(FocusEvent e) {
//				System.out.println("security text control has gained focus");
//				openSecuritiesShell(parent, securityClass);
//			}
//
//		});
	}

	private void openSecuritiesShell(final Composite parent,
			final ExtendablePropertySet<A> securityClass) {
		final Shell parentShell = parent.getShell();

		final Shell shell = new Shell(parent.getShell(), SWT.ON_TOP);
        shell.setLayout(new RowLayout(SWT.VERTICAL));
		System.out.println(shell.isDisposed() + ": " + shell.getDisplay());

        final List listControl = new List(shell, SWT.SINGLE | SWT.V_SCROLL);
        listControl.setLayoutData(new RowData(SWT.DEFAULT, 100));

        Button addSecurityButton = new Button(shell, SWT.PUSH);
        addSecurityButton.setText("Add New Stock...");
        addSecurityButton.addSelectionListener(new SelectionAdapter() {
        	@Override
			public void widgetSelected(SelectionEvent e) {
				NewStockWizard<A> wizard = new NewStockWizard<A>(getSession(), securityClass);
				System.out.println(shell.isDisposed() + ", " + shell.getDisplay());
				WizardDialog dialog = new WizardDialog(shell, wizard);
				dialog.setPageSize(600, 300);
				int result = dialog.open();
				if (result == Window.OK) {
					/*
					 * Having created the new stock, set it as the
					 * selected stock in this control.
					 */
	    	        setSecurity(wizard.getNewStock());
				}
			}
        });

        // Important we use the field for the session and stockClass.  We do not use the parameters
        // (the parameters may be null, but fields should always have been set by
        // the time control gets focus).
        allSecurities = new Vector<A>();
        addSecurities("", getSession().getCommodityCollection(), listControl, SecurityControl.this.securityPropertySet.getImplementationClass());

//        shell.setSize(listControl.computeSize(SWT.DEFAULT, listControl.getItemHeight()*10));

        // Set the currently set security into the list control.
        listControl.select(allSecurities.indexOf(commodity.getValue()));

        listControl.addSelectionListener(
        		new SelectionAdapter() {
        		    @Override
					public void widgetSelected(SelectionEvent e) {
						int selectionIndex = listControl.getSelectionIndex();
						commodity.setValue(allSecurities.get(selectionIndex));
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
					 Starting at the currently selected security,
					 search for a security starting with these characters.
					 */
					int startIndex = listControl.getSelectionIndex();
					if (startIndex == -1) {
						startIndex = 0;
					}

					int match = -1;
					int i = startIndex;
					do {
						if (allSecurities.get(i).getName().toUpperCase().startsWith(pattern)) {
							match = i;
							break;
						}

						i++;
						if (i == allSecurities.size()) {
							i = 0;
						}
					} while (i != startIndex);

					if (match != -1) {
						commodity.setValue(allSecurities.get(match));
						listControl.select(match);
						listControl.setTopIndex(match);
					}

					e.doit = false;
				}
			}
		});

		shell.pack();

        /*
		 * Position the shell below the text box, unless the
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

        /*
         * We need to be sure to close the shell when it is no longer active.
         * Listening for this shell to be deactivated does not work because there
         * may be child controls which create child shells (third level shells).
         * We do not want this shell to close if a child shell has been created
         * and activated.  We want to close this shell only if the parent shell
         * have been activated.  Note that if a grandparent shell is activated then
         * we do not want to close this shell.  The parent will be closed anyway
         * which would automatically close this one.
         */
        final ShellListener parentActivationListener = new ShellAdapter() {
			@Override
        	public void shellActivated(ShellEvent e) {
				System.out.println("closing shell");
        		shell.close();
        	}
        };

        parentShell.addShellListener(parentActivationListener);

        shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
        		parentShell.removeShellListener(parentActivationListener);
			}
        });
	}

	private void addSecurities(String prefix, Collection<? extends Commodity> securities, List listControl, Class<A> securityClass) {
    	Vector<A> matchingSecurities = new Vector<A>();
        for (Commodity security: securities) {
        	if (securityClass.isAssignableFrom(security.getClass())) {
        		matchingSecurities.add(securityClass.cast(security));
        	}
        }

		// Sort the securities by name.
		Collections.sort(matchingSecurities, new Comparator<Security>() {
			@Override
			public int compare(Security security1, Security security2) {
				return security1.getName().compareTo(security2.getName());
			}
		});

		for (A matchingSecurity: matchingSecurities) {
    		allSecurities.add(matchingSecurity);
			listControl.add(prefix + matchingSecurity.getName());
		}

    }

    /**
	 * @param object
	 */
	public void setSecurity(A security) {
		commodity.setValue(security);
	}

	/**
	 * @return the security, or null if no security has been set in
	 * 				the control
	 */
	public A getSecurity() {
		return commodity.getValue();
	}

	public IObservableValue<A> commodity() {
		return commodity;
	}

	public Control getControl() {
		return this;
	}

	/**
	 * Normally the session is set through the constructor. However in some
	 * circumstances (i.e. in the custom cell editors) the session is not
	 * available at construction time and null will be set. This method must
	 * then be called to set the session before the control is used (i.e. before
	 * the control gets focus).
	 */
//	public void setSession(Session session, ExtendablePropertySet<A> securityPropertySet) {
//		this.session = session;
//		this.securityPropertySet = securityPropertySet;
//	}

	/**
	 * One would expect that the security class could more simply be passed as a
	 * parameter to the SecurityControl constructor. After all it never changes.
	 * However the CellEditor API uses an anti-pattern whereby the call to the
	 * abstract method CreateControl is made from the constructor. This means
	 * the CellEditor has not been constructed when CreateControl is called, and
	 * so CreateControl does not have available the fields that would have been
	 * used to store accountClass. This is a hack to get around the problem.
	 * 
	 * @return
	 */
	protected abstract Session getSession(); 
}