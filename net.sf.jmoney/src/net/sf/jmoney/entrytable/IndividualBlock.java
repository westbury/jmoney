/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2007 Nigel Westbury <westbury@users.sf.net>
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

package net.sf.jmoney.entrytable;

import java.util.Comparator;

import net.sf.jmoney.resources.Messages;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

/**
 * Represents a column of data that can be displayed in the entries table,
 * edited by the user, sorted, or used in a filter.
 * <P>
 * All columns are managed by an object of this class. Special implementations
 * exist for the credit, debit, and balance columns. More generic
 * implementations exist for the other properties.
 */
public abstract class IndividualBlock<T, R> extends CellBlock<T, R> {

	/**
	 * The localized text to be shown in the header.
	 */
	private String text;

	public IndividualBlock(String text, int minimumWidth, int weight) {
		super(minimumWidth, weight);
		this.text = text;
	}

	@Override
	public void createHeaderControls(Composite parent, T entryData) {
		Label label = new Label(parent, SWT.NULL);
		label.setText(text);
		label
				.setBackground(Display.getCurrent().getSystemColor(
						SWT.COLOR_GRAY));

		label.setMenu(buildPopupMenu(parent.getShell()));
	}

	@Override
	void layout(int width) {
		this.width = width;
	}

	/**
	 * @return a comparator to be used for sorting rows based on the values in
	 *         this column, or null if this column is not suitable for sorting
	 */
	public Comparator<T> getComparator() {
		return null;
	}

	private Menu buildPopupMenu(Shell shell) {
		// Bring up a pop-up menu.
		// It would be a more consistent interface if this menu were
		// linked to the column header. However, TableColumn has
		// no setMenu method, nor does the column header respond to
		// SWT.MenuDetect nor any other event when right clicked.
		// This code works but does not follow the popup-menu conventions
		// on even one platform!

		Menu popupMenu = new Menu(shell, SWT.POP_UP);

		MenuItem removeColItem = new MenuItem(popupMenu, SWT.NONE);

		MenuItem shiftColLeftItem = new MenuItem(popupMenu, SWT.NONE);

		MenuItem shiftColRightItem = new MenuItem(popupMenu, SWT.NONE);

		Object[] messageArgs = new Object[] { text };

		removeColItem.setText(NLS.bind(Messages.IndividualBlock_RemoveColumn,
				messageArgs));
		shiftColLeftItem.setText(NLS.bind(Messages.IndividualBlock_MoveLeft,
				messageArgs));
		shiftColRightItem.setText(NLS.bind(Messages.IndividualBlock_MoveRight,
				messageArgs));

		removeColItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// TODO:
			}
		});

		shiftColLeftItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// TODO: shift left if we can
			}
		});

		shiftColRightItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// TODO: shift right if we can
			}
		});

		/*
		 * TODO: complete implementation of this. We need to allow the user to
		 * add columns?????
		 * 
		 * new MenuItem(popupMenu, SWT.SEPARATOR);
		 * 
		 * for (final IEntriesTableProperty entriesSectionProperty:
		 * entriesContent.getAllEntryDataObjects()) { boolean found = false; for
		 * (int index = 0; index < fTable.getColumnCount(); index++) {
		 * IEntriesTableProperty entryData2 = (IEntriesTableProperty) (fTable
		 * .getColumn(index).getData()); if (entryData2 ==
		 * entriesSectionProperty) { found = true; break; } }
		 * 
		 * if (!found) { Object[] messageArgs2 = new Object[] {
		 * entriesSectionProperty .getText() };
		 * 
		 * MenuItem addColItem = new MenuItem(popupMenu, SWT.NONE);
		 * addColItem.setText(new java.text.MessageFormat( "Add {0} column",
		 * java.util.Locale.US) .format(messageArgs2));
		 * 
		 * addColItem .addSelectionListener(new SelectionAdapter() { public void
		 * widgetSelected( SelectionEvent e) { addColumn(entriesSectionProperty,
		 * Math.max(1, column)); } }); } }
		 */
		// popupMenu.setVisible(true);
		return popupMenu;
	}

	public String getText() {
		return text;
	}
}
