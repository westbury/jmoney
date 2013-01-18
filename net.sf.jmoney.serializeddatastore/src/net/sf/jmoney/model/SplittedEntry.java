/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

package net.sf.jmoney.model;

import java.util.Enumeration;
import java.util.Vector;

public class SplittedEntry extends Entry {

	private static final long serialVersionUID = 2919524277629853452L;

	protected Vector entries = new Vector();

	public SplittedEntry() { }

	public SplittedEntry(Entry entry) {
		super(entry);
	}

	public Vector getEntries() { return entries; }

	public void setEntries(Vector newEntries) {
		entries = newEntries;
		changeSupport.firePropertyChange("entries", null, entries); //$NON-NLS-1$
	}

	@SuppressWarnings("unchecked")
	public void addEntry(Entry e) {
		e.setDate(getDate());
		entries.addElement(e);
	}

	public void removeEntryAt(int index) {
		Entry e = (Entry) entries.elementAt(index);
		if (e instanceof DoubleEntry) ((DoubleEntry) e).removeOther();
		entries.removeElementAt(index);
	}

	public void removeAllEntries() {
		for (Enumeration e = entries.elements(); e.hasMoreElements(); ) {
			Entry entry = (Entry) e.nextElement();
			if (entry instanceof DoubleEntry) ((DoubleEntry) entry).removeOther();
		}
		entries.removeAllElements();
	}

	@SuppressWarnings("unchecked")
	public void setEntryAt(Entry newEntry, int index) {
		Entry oldEntry = (Entry) entries.elementAt(index);
		if (oldEntry instanceof DoubleEntry) ((DoubleEntry) oldEntry).removeOther();
		entries.setElementAt(newEntry, index);
		if (newEntry instanceof DoubleEntry) ((DoubleEntry) newEntry).addOther();
	}

	@Override
	public SplittedEntry toSplittedEntry() {
		SplittedEntry se = new SplittedEntry(this);
		se.setEntries(getEntries());
		return se;
	}

}
