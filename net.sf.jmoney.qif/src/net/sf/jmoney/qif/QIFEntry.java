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

package net.sf.jmoney.qif;

import net.sf.jmoney.model2.EntryExtension;
import net.sf.jmoney.model2.ExtendableObject;

/**
 * Property set implementation class for the properties added
 * to each Entry object by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class QIFEntry extends EntryExtension {
	
	private char reconcilingState = ' ';
	private String address = null;
	
	/**
	 * A default constructor is mandatory for all extension objects.
	 * The default constructor sets the extension properties to
	 * appropriate default values.
	 */
	public QIFEntry(ExtendableObject extendedObject) {
		super(extendedObject);
	}
	
	/**
	 * A Full constructor is mandatory for all extension objects.
	 * This constructor is called by the datastore to construct
	 * the extension objects when loading data.
	 * 
	 * @param reconcilingState
	 */
	public QIFEntry(
			ExtendableObject extendedObject,
			char reconcilingState,
			String address) {
		super(extendedObject);
		this.reconcilingState = reconcilingState;
		this.address = address;
	}
	
	/**
	 * Gets the status of the entry.
	 * 
	 * @return The status of the entry.
	 * <LI>
	 * <UL>' ' - the entry not not been reconciled</UL>
	 * <UL>'*' - the entry is being reconciled</UL>
	 * <UL>'X' - the entry has cleared and shows on the statement</UL>
	 * </LI>
	 */
	public char getReconcilingState() {
		return reconcilingState;
	}
	
	/**
	 * Gets the address of the payee for the bank account debit represented by
	 * this entry.
	 * 
	 * @return The address, with each line separated by a '/n' character. QIF
	 *         files usually support up to five lines of address, but there may
	 *         be a sixth line for an optional message. Null will be returned if
	 *         no address is set.
	 */
	public String getAddress() {
		return address;
	}
	
	/**
	 * Sets the status of the entry.
	 * 
	 * @param reconcilingState the new status of the entry.
	 * <LI>
	 * <UL>' ' - the entry not not been reconciled</UL>
	 * <UL>'*' - the entry is being reconciled</UL>
	 * <UL>'X' - the entry has cleared and shows on the statement</UL>
	 * </LI>
	 */
	public void setReconcilingState(char reconcilingState) {
		char oldReconcilingState = this.reconcilingState;
		this.reconcilingState = reconcilingState;
		processPropertyChange(QIFEntryInfo.getReconcilingStateAccessor(), oldReconcilingState, reconcilingState);
	}

	/**
	 * Sets the address.
	 * 
	 * @param address
	 *            The address, with each line separated by a '/n' character. QIF
	 *            files usually support up to five lines of address, but there
	 *            may be a sixth line for an optional message. Null may be set
	 *            to indicate no address.
	 */
	public void setAddress(String address) {
		String oldAddress = this.address;
		this.address = address;
		processPropertyChange(QIFEntryInfo.getAddressAccessor(), oldAddress, address);
	}
}
