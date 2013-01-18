/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2000-2003 Johann Gyger <jgyger@users.sf.net>
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
import java.util.Vector;

/**
 * Holds the fields that will be saved in a file.
 */
public class Session implements Serializable {

	private static final long serialVersionUID = 3821656883337202055L;

	protected Vector accounts = new Vector();

    protected CategoryTreeModel categories = new CategoryTreeModel();

    protected transient boolean modified = false;

    protected transient PropertyChangeSupport changeSupport =
        new PropertyChangeSupport(this);

    public Session() {
    }

    public Session(int dummy) {
        categories = new CategoryTreeModel(0);
    }

    public void setAccounts(Vector newAccounts) {
        accounts = newAccounts;
    }

    public void setCategories(CategoryTreeModel newCategories) {
        categories = newCategories;
    }

    public Vector getAccounts() {
        return accounts;
    }

    public CategoryTreeModel getCategories() {
        return categories;
    }

    public Account getAccountByNumber(String accountNumber) {
        Vector accounts = getAccounts();
        for (int i = 0; i < accounts.size(); i++) {
            Account account = (Account) accounts.get(i);
            if (account.getAccountNumber() != null
                && account.getAccountNumber().equals(accountNumber)) {
                return account;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
	public Account getNewAccount(String name) {
        Account account = new Account(name);
        getAccounts().addElement(account);
        getCategories().insertNodeInto(
            account.getCategoryNode(),
            getCategories().getTransferNode(),
            0);
        modified();
        return account;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean m) {
        if (modified == m)
            return;
        modified = m;
        changeSupport.firePropertyChange("modified", !m, m); //$NON-NLS-1$
    }

    public void modified() {
        setModified(true);
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
        modified = false;
        changeSupport = new PropertyChangeSupport(this);
    }

}
