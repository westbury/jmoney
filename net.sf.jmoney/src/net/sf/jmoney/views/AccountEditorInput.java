/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Johann Gyger <jgyger@users.sf.net>
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

package net.sf.jmoney.views;

import java.util.Vector;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.PageEntry;
import net.sf.jmoney.model2.PropertySet;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class AccountEditorInput implements IEditorInput, IPersistableElement {

	private String fullAccountName;
	private String accountName;
	private ExtendablePropertySet<? extends Account> propertySet;
    private IMemento memento;
    
    /**
     * Create a new editor input.
     */
    public AccountEditorInput(String fullAccountName, String accountName, ExtendablePropertySet<? extends Account> propertySet, IMemento memento) {
		this.fullAccountName = fullAccountName;
		this.accountName = accountName;
		this.propertySet = propertySet;
		this.memento = memento;
	}

    /**
     * This is a convenience constructor to create an input when we
     * have an account object.  The input does not contain a reference
     * to the account, it contains only information from which it can
     * look up the account, together with information required to determine
     * tool-tips, images etc.
     * 
     * @param account
     */
	public AccountEditorInput(Account account) {
		this(account.getFullAccountName(), account.getName(), PropertySet.getPropertySet(account.getClass()), null);
	}

	/* (non-Javadoc)
     * @see org.eclipse.ui.IEditorInput#exists()
     */
    @Override
	public boolean exists() {
    	// TODO return false if the account does not exist in the session???
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
     */
    @Override
	public ImageDescriptor getImageDescriptor() {
    	// This method is never called.
    	// TODO: figure out when this method is supposed to be called
    	// and what we should return here.
    	return propertySet.getIconImageDescriptor();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IEditorInput#getName()
     */
    @Override
	public String getName() {
    	return accountName;
    }
    
    /*
     * Returns the image for the account.  This is a cached image and must not
     * be disposed.
     */
    public Image getImage() {
    	return propertySet.getIconImage();
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.IEditorInput#getPersistable()
     */
    @Override
	public IPersistableElement getPersistable() {
        // This class implements the IPersistableElement
    	// methods, so return a pointer to this object.
        return this;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IEditorInput#getToolTipText()
     */
    @Override
	public String getToolTipText() {
    	return fullAccountName;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    @Override
	public Object getAdapter(Class adapter) {
        return null;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override	
    public boolean equals(Object other) {
        if (other instanceof AccountEditorInput) {
            AccountEditorInput otherInput = (AccountEditorInput) other;
            return fullAccountName.equals(otherInput.fullAccountName);
        }

        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override	
    public int hashCode() {
        return fullAccountName.hashCode();
    }

    public Vector<PageEntry> getPageListeners() {
    	return propertySet.getPageFactories();
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPersistableElement#getFactoryId()
	 */
	@Override
	public String getFactoryId() {
		return AccountEditorInputFactory.ID;
	}

	/**
	 * @param memento
	 */
	@Override
	public void saveState(IMemento memento) {
		memento.putString("account", fullAccountName); //$NON-NLS-1$
		memento.putString("label", accountName); //$NON-NLS-1$
		memento.putString("propertySet", propertySet.getId()); //$NON-NLS-1$
	}

	/**
	 * @return
	 */
	public IMemento getMemento() {
		return memento;
	}

	public String getFullAccountName() {
		return fullAccountName;
	}
}
