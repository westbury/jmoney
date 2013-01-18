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
import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import net.sf.jmoney.serializeddatastore.Messages;

/**
 * The data model for an account.
 */
public class Account implements Category, Serializable {

	private static final long serialVersionUID = -204768756583878194L;

	/**
	 * The entries are ordered by their creation.
	 */
	public static final int CREATION_ORDER = 0;

	/**
	 * The entries are ordered by their date field.
	 */
	public static final int DATE_ORDER = 1;

	/**
	 * The entries are ordered by their check field.
	 */
	public static final int CHECK_ORDER = 2;

	/**
	 * The entries are ordered by their valuta field.
	 */
	public static final int VALUTA_ORDER = 3;

	protected static String defaultCurrencyCode = "USD"; //$NON-NLS-1$

	protected static String[] entryOrderNames;

	protected String name;

	protected String currencyCode;

	protected String bank = null;

	protected String accountNumber = null;

	protected long startBalance = 0;

	protected Long minBalance = null;

	protected String abbrevation = null;

	protected String comment = null;

	protected Vector entries = new Vector();

	protected CategoryNode categoryNode = new CategoryNode(this);

	protected transient PropertyChangeSupport changeSupport =
		new PropertyChangeSupport(this);

	/**
	 * @return The default currency code
	 */
	public static String getDefaultCurrencyCode() {
		return defaultCurrencyCode;
	}

	/**
	 * @param code the default currency code
	 */
	public static void setDefaultCurrencyCode(String code) {
		defaultCurrencyCode = code;
	}

	/**
	 * Used by XMLEncoder.
	 */
	public Account() {
	}

	/**
	 * Creates a new account with the provided name
	 * @param aName the name of the account
	 */
	public Account(String aName) {
		setCurrencyCode(getDefaultCurrencyCode());
		setName(aName);
	}

	/**
	 * @return the name of this account.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the locale of this account.
	 */
	public String getCurrencyCode() {
		return currencyCode;
	}

	public Currency getCurrency() {
		return Currency.getCurrencyForCode(getCurrencyCode());
	}

	/**
	 * @return the bank name of this account.
	 */
	public String getBank() {
		return bank;
	}

	/**
	 * @return the account number of this account.
	 */
	public String getAccountNumber() {
		return accountNumber;
	}

	/**
	 * @return the initial balance of this account.
	 */
	public long getStartBalance() {
		return startBalance;
	}

	/**
	 * @return the minimal balance of this account.
	 */
	public Long getMinBalance() {
		return minBalance;
	}

	/**
	 * @return the abbrevation of this account.
	 */
	public String getAbbrevation() {
		return abbrevation;
	}

	/**
	 * @return the comment of this account.
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @return the entries of this account.
	 */
	public Vector getEntries() {
		return entries;
	}

	@SuppressWarnings("unchecked")
	public void addEntry(Entry entry) {
		entries.addElement(entry);
	}

	public void removeEntry(Entry entry) {
		cleanupEntry(entry);
		entries.removeElement(entry);
	}

	private void cleanupEntry(Entry entry) {
		if (entry instanceof DoubleEntry)
			 ((DoubleEntry) entry).removeOther();
		else if (entry instanceof SplittedEntry)
			 ((SplittedEntry) entry).removeAllEntries();
	}

	private void initEntry(Entry entry) {
		if (entry instanceof DoubleEntry) {
			DoubleEntry de = (DoubleEntry) entry;
			de.addOther();
			de.getOther().setCategory(this);
		}
	}

	@SuppressWarnings("unchecked")
	public void replaceEntry(Entry oldEntry, Entry newEntry) {
		int index = entries.indexOf(oldEntry);
		cleanupEntry(oldEntry);
		initEntry(newEntry);
		entries.setElementAt(newEntry, index);
	}

	/**
	 * Parses the amount field.
	 * @param amountString amount that has to be parsed.
	 * @return amount
	 */
	public long parseAmount(String amountString) {
		Number amount = new Double(0);
		try {
			amount = getCurrency().getNumberFormat().parse(amountString);
		} catch (ParseException pex) {
		}
		return Math.round(
			amount.doubleValue() * getCurrency().getScaleFactor());
	}

	public String formatAmount(long amount) {
		return getCurrency().format(amount);
	}

	/**
	 * @param aName the name of this account.
	 */
	public void setName(String aName) {
		if (name != null && name.equals(aName))
			return;
		name = aName;
		changeSupport.firePropertyChange("name", null, name); //$NON-NLS-1$
	}

	/**
	 * @param theEntries the entries of this account.
	 */
	public void setEntries(Vector newEntries) {
		entries = newEntries;
		changeSupport.firePropertyChange("entries", null, entries); //$NON-NLS-1$
	}

	public void setCurrencyCode(String aCurrencyCode) {
		if (currencyCode != null && currencyCode.equals(aCurrencyCode))
			return;
		currencyCode = aCurrencyCode;
		changeSupport.firePropertyChange("currency", null, currencyCode); //$NON-NLS-1$
	}

	/**
	 * @param aBank the name of this account.
	 */
	public void setBank(String aBank) {
		if (bank != null && bank.equals(aBank))
			return;
		bank = aBank;
		changeSupport.firePropertyChange("bank", null, bank); //$NON-NLS-1$
	}

	/**
	 * Sets the account number of this account.
	 * @param anAccountNumber the account number
	 */
	public void setAccountNumber(String anAccountNumber) {
		if (accountNumber != null && accountNumber.equals(anAccountNumber))
			return;
		accountNumber = anAccountNumber;
		changeSupport.firePropertyChange("accountNumber", null, accountNumber); //$NON-NLS-1$
	}

	/**
	 * Sets the initial balance of this account.
	 * @param s the start balance
	 */
	public void setStartBalance(long s) {
		if (startBalance == s)
			return;
		startBalance = s;
		changeSupport.firePropertyChange("startBalance", null, new Long(s)); //$NON-NLS-1$
	}

	/**
	 * @param m the minimal balance which may be null.
	 */
	public void setMinBalance(Long m) {
		if (minBalance == m)
			return;
		minBalance = m;
		changeSupport.firePropertyChange("minBalance", null, m); //$NON-NLS-1$
	}

	/**
	 * @param anAbbrevation the abbrevation of this account.
	 */
	public void setAbbrevation(String anAbbrevation) {
		if (abbrevation != null && abbrevation.equals(anAbbrevation))
			return;
		abbrevation = anAbbrevation;
		changeSupport.firePropertyChange("abbrevation", null, abbrevation); //$NON-NLS-1$
	}

	/**
	 * @param aComment the comment of this account.
	 */
	public void setComment(String aComment) {
		if (comment != null && comment.equals(aComment))
			return;
		comment = aComment;
		changeSupport.firePropertyChange("comment", null, comment); //$NON-NLS-1$
	}

	/**
	 * Adds a PropertyChangeListener.
	 * @param pcl a property change listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		changeSupport.addPropertyChangeListener(pcl);
	}

	/**
	 * Removes a PropertyChangeListener.
	 * @param pcl a property change listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		changeSupport.removePropertyChangeListener(pcl);
	}

	/**
	 * Sort the entries.
	 */
	@SuppressWarnings("unchecked")
	public void sortEntries(Comparator c) {
		Collections.sort(entries, c);
	}

	@Override
	public String toString() {
		return name;
	}

	private void readObject(ObjectInputStream in)
		throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		changeSupport = new PropertyChangeSupport(this);
	}

	/**
	 * Category implementation.
	 * @return the category name.
	 */
	public String getCategoryName() {
		return name;
	}

	public String getFullCategoryName() {
		return Messages.Account_FullCategoryName 
			+ ":" //$NON-NLS-1$
			+ getCategoryName();
	}

	/**
	 * Category implementation.
	 * @return the category tree node.
	 */
	public CategoryNode getCategoryNode() {
		return categoryNode;
	}

	public void setCategoryNode(CategoryNode aCategoryNode) {
		categoryNode = aCategoryNode;
	}

	public int compareTo(Object o) {
		Account a = (Account) o;
		return getName().compareTo(a.getName());
	}

}
