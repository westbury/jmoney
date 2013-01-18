/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

import java.util.Date;

/**
 *
 * @author  Nigel
 *
 * To add fields and methods to an Entry object, one should
 * derive a class on EntryExtension.  This mechanism
 * allows multiple extensions to an Entry object to be added
 * and maintained at runtime.
 *
 */
public abstract class EntryExtension extends ExtensionObject {
	
	public EntryExtension(ExtendableObject extendedObject) {
		super(extendedObject);
	}
	
	/*
	 * All extensions implement the Entry interface.  This is for convenience
	 * so the comsumer can get a single object that supports both the base Entry
	 * methods and the extension methods.  All Entry interface methods are passed
	 * on to the base Entry object.
	 */
	
	/**
	 * Returns the category.
	 */
	public Account getAccount() {
		return getBaseObject().getAccount();
	}
	
	/**
	 * Returns the amount.
	 */
	public long getAmount() {
		return getBaseObject().getAmount();
	}
	
	/**
	 * Sets the category.
	 */
	public void setAccount(Account account) {
		getBaseObject().setAccount(account);
	}
	
	/**
	 * Sets the amount.
	 */
	public void setAmount(long anAmount) {
		getBaseObject().setAmount(anAmount);
	}
	
	public void setCommodity(Commodity commodity) {
		getBaseObject().setCommodity(commodity);
	}
	
	public Commodity getCommodity() {
		return getBaseObject().getCommodity();
	}
	
	public Transaction getTransaction() {
		return getBaseObject().getTransaction();
	}
	
	/**
	 * Returns the creation.
	 */
	public long getCreation() {
		return getBaseObject().getCreation();
	}
	
	/**
	 * Returns the check.
	 */
	public String getCheck() {
		return getBaseObject().getCheck();
	}
	
	/**
	 * Returns the valuta.
	 */
	public Date getValuta() {
		return getBaseObject().getValuta();
	}
	
	// TODO: should really be in a utility class.
	public String getFullAccountName() {
		return getBaseObject().getFullAccountName();
	}
	
	/**
	 * Returns the memo.
	 */
	public String getMemo() {
		return getBaseObject().getMemo();
	}
	
	/**
	 * Sets the creation.
	 */
	public void setCreation(long creation) {
		getBaseObject().setCreation(creation);
	}
	
	/**
	 * Sets the check.
	 */
	public void setCheck(String check) {
		getBaseObject().setCheck(check);
	}
	
	/**
	 * Sets the valuta.
	 */
	public void setValuta(Date valuta) {
		getBaseObject().setValuta(valuta);
	}
	
	/**
	 * Sets the memo.
	 */
	public void setMemo(String memo) {
		getBaseObject().setMemo(memo);
	}
	
	public Entry getBaseObject() {
		return (Entry)baseObject;
	}
}
