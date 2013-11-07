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

package net.sf.jmoney.model2;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.jface.databinding.swt.SWTObservables;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


/**
 * A control for selecting a commodity.
 *
 * This control contains both a text box and a list box that appears when the
 * text box gains focus.
 *
 * @author Nigel Westbury
 */
public class CommodityControl<A extends Commodity> extends Composite {

	Text textControl;

	private Session session;
	private Class<A> commodityClass;

    /**
     * List of all acceptable commodities.
     */
    private Vector<A> allCommodities;

	/**
	 * Currently selected commodity, or null if no commodity selected
	 */
	public final WritableValue<A> commodity = new WritableValue<A>();

	/**
	 * @param parent
	 * @param style
	 */
	public CommodityControl(final Composite parent, Session session, final Class<A> commodityClass) {
		super(parent, SWT.NONE);
		this.session = session;
		this.commodityClass = commodityClass;

		setBackgroundMode(SWT.INHERIT_FORCE);

		setLayout(new FillLayout(SWT.VERTICAL));

		textControl = new Text(this, SWT.LEFT);

		IConverter<A,String> commodityToTextConverter = new Converter<A,String>(Commodity.class, String.class) {
			@Override
			public String convert(A commodity) {
				return (commodity == null) ? "" : commodity.getName();
			}
		};
		Bind.oneWay(commodity)
			.convert(commodityToTextConverter)
			.to(SWTObservables.observeText(textControl, SWT.FocusOut));

		textControl.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					openDropdownShell(parent, commodityClass);
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
//				System.out.println("commodity text control has gained focus");
//				openDropdownCommodityShell(parent, commodityClass);
//			}
//
//		});
	}

	private void openDropdownShell(final Composite parent,
			final Class<A> commodityClass) {
		final Shell parentShell = parent.getShell();

		final Shell shell = new Shell(parent.getShell(), SWT.ON_TOP);
        shell.setLayout(new RowLayout(SWT.VERTICAL));
		System.out.println(shell.isDisposed() + ": " + shell.getDisplay());

        final List listControl = new List(shell, SWT.SINGLE | SWT.V_SCROLL);
        listControl.setLayoutData(new RowData(SWT.DEFAULT, 100));

        // Important we use the field for the session and commodityClass.  We do not use the parameters
        // (the parameters may be null, but fields should always have been set by
        // the time control gets focus).
        allCommodities = new Vector<A>();
        addCommodities("", CommodityControl.this.session.getCommodityCollection(), listControl, CommodityControl.this.commodityClass);

//        shell.setSize(listControl.computeSize(SWT.DEFAULT, listControl.getItemHeight()*10));

        // Set the currently set commodity into the list control.
        listControl.select(allCommodities.indexOf(commodity));

        listControl.addSelectionListener(
        		new SelectionAdapter() {
        		    @Override
					public void widgetSelected(SelectionEvent e) {
						int selectionIndex = listControl.getSelectionIndex();
						commodity.setValue(allCommodities.get(selectionIndex));
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
					 Starting at the currently selected commodity,
					 search for a commodity starting with these characters.
					 */
					int startIndex = listControl.getSelectionIndex();
					if (startIndex == -1) {
						startIndex = 0;
					}

					int match = -1;
					int i = startIndex;
					do {
						if (allCommodities.get(i).getName().toUpperCase().startsWith(pattern)) {
							match = i;
							break;
						}

						i++;
						if (i == allCommodities.size()) {
							i = 0;
						}
					} while (i != startIndex);

					if (match != -1) {
						commodity.setValue(allCommodities.get(match));
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

	private void addCommodities(String prefix, Collection<? extends Commodity> commodities, List listControl, Class<A> commodityClass) {
    	Vector<A> matchingCommodities = new Vector<A>();
        for (Commodity commodity: commodities) {
        	if (commodityClass.isAssignableFrom(commodity.getClass())) {
        		matchingCommodities.add(commodityClass.cast(commodity));
        	}
        }

		// Sort the commodities by name.
		Collections.sort(matchingCommodities, new Comparator<Commodity>() {
			@Override
			public int compare(Commodity commodity1, Commodity commodity2) {
				return commodity1.getName().compareTo(commodity2.getName());
			}
		});

		for (A matchingCommodity : matchingCommodities) {
    		allCommodities.add(matchingCommodity);
			listControl.add(prefix + matchingCommodity.getName());
		}

    }

    /**
	 * @param object
	 */
	public void setCommodity(A commodity) {
		this.commodity.setValue(commodity);
	}

	/**
	 * @return the commodity, or null if no commodity has been set in
	 * 				the control
	 */
	public A getCommodity() {
		return commodity.getValue();
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
	public void setSession(Session session, Class<A> commodityClass) {
		this.session = session;
		this.commodityClass = commodityClass;
	}
}