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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import net.sf.jmoney.serializeddatastore.Messages;

/**
 * The data model for an entry.
 */
public class Entry implements Serializable {

	private static final long serialVersionUID = 1860094708090075118L;

	/**
	 * Entry is uncleared.
	 */
	public static final int UNCLEARED = 0;

	/**
	 * Entry is reconciling.
	 */
	public static final int RECONCILING = 1;

	/**
	 * Entry is cleared.
	 */
	public static final int CLEARED = 2;

	/**
	 * This entry is a prototype if the check field points to this string.
	 */
	public static final Entry PROTOTYPE = new Entry();

	protected long creation = Calendar.getInstance().getTime().getTime();

	protected String check = null;

	protected Date date = null;

	protected Date valuta = null;

	protected String description = null;

	protected Category category = null;

	protected long amount = 0;

	protected int status = 0;

	protected String memo = null;

	protected transient PropertyChangeSupport changeSupport =
		new PropertyChangeSupport(this);

	/**
	 * Creates a new entry.
	 */
	public Entry() {
	}

	public Entry(Entry entry) {
		copyValues(entry);
	}

	/**
	 * Returns the names of the states.
	 */
	public static String[] statusNames() {
		String[] text = new String[3];
		text[UNCLEARED] = Messages.Entry_UnclearedName;
		text[RECONCILING] = Messages.Entry_ReconcilingName;
		text[CLEARED] = Messages.Entry_ClearedName;
		return text;
	}

	public Entry toEntry() {
		return new Entry(this);
	}

	public DoubleEntry toDoubleEntry() {
		return new DoubleEntry(this);
	}

	public SplittedEntry toSplittedEntry() {
		return new SplittedEntry(this);
	}

	protected void copyValues(Entry newEntry) {
		amount = newEntry.getAmount();
		category = newEntry.getCategory();
		check = newEntry.getCheck();
		creation = newEntry.getCreation();
		date = newEntry.getDate();
		description = newEntry.getDescription();
		memo = newEntry.getMemo();
		status = newEntry.getStatus();
		valuta = newEntry.getValuta();
	}

	/**
	 * Returns the creation.
	 */
	public long getCreation() {
		return creation;
	}

	/**
	 * Returns the check.
	 */
	public String getCheck() {
		return check;
	}

	/**
	 * Returns the date.
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * Returns the valuta.
	 */
	public Date getValuta() {
		return valuta;
	}

	/**
	 * Returns the description.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the category.
	 */
	public Category getCategory() {
		return category;
	}

	public String getFullCategoryName() {
		return category == null ? null : category.getFullCategoryName();
	}

	/**
	 * Returns the amount.
	 */
	public long getAmount() {
		return amount;
	}

	/**
	 * Returns the status.
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Returns the memo.
	 */
	public String getMemo() {
		return memo;
	}

	/**
	 * Returns a short String representing the status.
	 */
	public String getStatusString() {
		String[] text = new String[3];
		text[UNCLEARED] = Messages.Entry_UnclearedStatus;
		text[RECONCILING] =
			Messages.Entry_ReconcilingStatus;
		text[CLEARED] = Messages.Entry_ClearedStatus;
		return text[getStatus()];
	}

	/**
	 * Sets the creation.
	 */
	public void setCreation(long aCreation) {
		creation = aCreation;
	}

	/**
	 * Sets the check.
	 */
	public void setCheck(String aCheck) {
		if (check != null && check.equals(aCheck))
			return;
		check = aCheck.length() == 0 ? null : aCheck;
		changeSupport.firePropertyChange("check", null, check); //$NON-NLS-1$
	}

	/**
	 * Sets the date.
	 */
	public void setDate(Date aDate) {
		if (date != null && date.equals(aDate))
			return;
		date = aDate;
		changeSupport.firePropertyChange("date", null, date); //$NON-NLS-1$
	}

	/**
	 * Sets the valuta.
	 */
	public void setValuta(Date aValuta) {
		if (valuta != null && valuta.equals(aValuta))
			return;
		valuta = aValuta;
		changeSupport.firePropertyChange("valuta", null, valuta); //$NON-NLS-1$
	}

	/**
	 * Sets the description.
	 */
	public void setDescription(String aDescription) {
		if (description != null && description.equals(aDescription))
			return;
		description = aDescription.length() == 0 ? null : aDescription;
		changeSupport.firePropertyChange("description", null, description); //$NON-NLS-1$
	}

	/**
	 * Sets the category.
	 */
	public void setCategory(Category aCategory) {
		if (category != null && category.equals(aCategory))
			return;
		category = aCategory;
		changeSupport.firePropertyChange("category", null, category); //$NON-NLS-1$
	}

	/**
	 * Sets the amount.
	 */
	public void setAmount(long anAmount) {
		if (amount == anAmount)
			return;
		amount = anAmount;
		changeSupport.firePropertyChange("amount", null, new Double(amount)); //$NON-NLS-1$
	}

	/**
	 * Sets the check. Either UNCLEARED, RECONCILING or CLEARED.
	 */
	public void setStatus(int aStatus) {
		if (status == aStatus)
			return;
		status = aStatus;
		changeSupport.firePropertyChange("status", 0, status); //$NON-NLS-1$
	}

	/**
	 * Sets the memo.
	 */
	public void setMemo(String aMemo) {
		if (memo != null && memo.equals(aMemo))
			return;
		memo = aMemo.length() == 0 ? null : aMemo;
		changeSupport.firePropertyChange("memo", null, memo); //$NON-NLS-1$
	}

	/**
	 * Adds a PropertyChangeListener.
	 */
	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		changeSupport.addPropertyChangeListener(pcl);
	}

	/**
	 * Removes a PropertyChangeListener.
	 */
	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		changeSupport.removePropertyChangeListener(pcl);
	}

	private void readObject(ObjectInputStream in)
		throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		changeSupport = new PropertyChangeSupport(this);
	}

}
