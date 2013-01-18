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

package net.sf.jmoney.property.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.property.wizards.NewPropertyWizard;

import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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


/**
 * A control for selecting a real property (such as a house).
 * <P>
 * This control contains both a text box and a list box that appears when the
 * text box gains focus.
 * 
 * @author Nigel Westbury
 */
public class RealPropertyControl<A extends RealProperty> extends Composite {

	Text textControl;
	
	private Session session;
	private Class<A> realPropertyClass;
	
    /**
     * List of securities put into security list.
     */
    private Vector<A> allProperties;
    
	/**
	 * Currently selected property, or null if no property selected
	 */
	private A property;
	
	private Vector<SelectionListener> listeners = new Vector<SelectionListener>();
	
	/**
	 * @param parent
	 * @param style
	 */
	public RealPropertyControl(final Composite parent, Session session, final Class<A> realPropertyClass) {
		super(parent, SWT.NONE);
		this.session = session;
		this.realPropertyClass = realPropertyClass;

		setBackgroundMode(SWT.INHERIT_FORCE);
		
		setLayout(new FillLayout(SWT.VERTICAL));
		
		textControl = new Text(this, SWT.LEFT);

		textControl.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					openRealPropertiesShell(parent, realPropertyClass);
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

	private void openRealPropertiesShell(final Composite parent,
			final Class<A> securityClass) {
		final Shell parentShell = parent.getShell();

		final Shell shell = new Shell(parent.getShell(), SWT.ON_TOP);
        shell.setLayout(new RowLayout(SWT.VERTICAL));
		System.out.println(shell.isDisposed() + ": " + shell.getDisplay());

        final List listControl = new List(shell, SWT.SINGLE | SWT.V_SCROLL);
        listControl.setLayoutData(new RowData(SWT.DEFAULT, 100));

        Button addPropertyButton = new Button(shell, SWT.PUSH);
        addPropertyButton.setText("Add New Property...");
        addPropertyButton.addSelectionListener(new SelectionAdapter() {
        	@Override
			public void widgetSelected(SelectionEvent e) {
				NewPropertyWizard wizard = new NewPropertyWizard(RealPropertyControl.this.session);
				System.out.println(shell.isDisposed() + ", " + shell.getDisplay());
				WizardDialog dialog = new WizardDialog(shell, wizard);
				dialog.setPageSize(600, 300);
				int result = dialog.open();
				if (result == Window.OK) {
					/*
					 * Having created the new property, set it as the
					 * selected property in this control.
					 */
	    	        setSecurity(securityClass.cast(wizard.getNewAsset()));
				}
			}
        });
        
        // Important we use the field for the session and stockClass.  We do not use the parameters
        // (the parameters may be null, but fields should always have been set by
        // the time control gets focus).
        allProperties = new Vector<A>();
        addSecurities("", RealPropertyControl.this.session.getCommodityCollection(), listControl, RealPropertyControl.this.realPropertyClass);
        
//        shell.setSize(listControl.computeSize(SWT.DEFAULT, listControl.getItemHeight()*10));
        
        // Set the currently set security into the list control.
        listControl.select(allProperties.indexOf(property));
        
        listControl.addSelectionListener(
        		new SelectionAdapter() {
        		    @Override	
					public void widgetSelected(SelectionEvent e) {
						int selectionIndex = listControl.getSelectionIndex();
						property = allProperties.get(selectionIndex);
						textControl.setText(property.getName());
						fireSecurityChangeEvent();
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
						if (allProperties.get(i).getName().toUpperCase().startsWith(pattern)) {
							match = i;
							break;
						}
						
						i++;
						if (i == allProperties.size()) {
							i = 0;
						}
					} while (i != startIndex);
					
					if (match != -1) {
						property = allProperties.get(match);
						listControl.select(match);
						listControl.setTopIndex(match);
						textControl.setText(property.getName());
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

	private void fireSecurityChangeEvent() {
		for (SelectionListener listener: listeners) {
			listener.widgetSelected(null);
		}
	}
	
	private void addSecurities(String prefix, Collection<? extends Commodity> securities, List listControl, Class<A> securityClass) {
    	Vector<A> matchingSecurities = new Vector<A>();
        for (Commodity security: securities) {
        	if (securityClass.isAssignableFrom(security.getClass())) {
        		matchingSecurities.add(securityClass.cast(security));
        	}
        }
		
		// Sort the securities by name.
		Collections.sort(matchingSecurities, new Comparator<RealProperty>() {
			public int compare(RealProperty security1, RealProperty security2) {
				return security1.getName().compareTo(security2.getName());
			}
		});
		
		for (A matchingSecurity: matchingSecurities) {
    		allProperties.add(matchingSecurity);
			listControl.add(prefix + matchingSecurity.getName());
		}
        
    }

    /**
	 * @param object
	 */
	public void setSecurity(A security) {
		this.property = security;

		if (security == null) {
			textControl.setText("");
		} else {
			textControl.setText(security.getName());
		}
	}

	/**
	 * @return the security, or null if no security has been set in
	 * 				the control
	 */
	public A getSecurity() {
		return property;
	}

	/**
	 * @param listener
	 */
	public void addSelectionListener(SelectionListener listener) {
		listeners.add(listener);
	}

	/**
	 * @param listener
	 */
	public void removeSelectionListener(SelectionListener listener) {
		listeners.remove(listener);
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
	public void setSession(Session session, Class<A> securityClass) {
		this.session = session;
		this.realPropertyClass = securityClass;
	}
}