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

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.MultiTextControlFactory;
import net.sf.jmoney.fields.TextControlFactory;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.resources.Messages;

/**
 * This class is a listener class to the net.sf.jmoney.fields
 * extension point.  It implements an extension.
 * <P>
 * This extension registers the CapitalAccount properties.  By registering
 * the properties, every one can know how to display, edit, and store
 * the properties.
 * <P>
 * These properties are supported in the JMoney base code, so everyone
 * including plug-ins will know about these properties.  However, to
 * follow the Eclipse paradigm (every one should be treated equal,
 * including oneself), these are registered through the same extension
 * point that plug-ins must also use to register their properties.
 * 
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class CapitalAccountInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<CapitalAccount> propertySet = PropertySet.addDerivedAbstractPropertySet(CapitalAccount.class, Messages.CapitalAccountInfo_Description, AccountInfo.getPropertySet());

	private static ListPropertyAccessor<CapitalAccount,CapitalAccount> subAccountAccessor = null;
	private static ScalarPropertyAccessor<String,CapitalAccount> abbreviationAccessor = null;
	private static ScalarPropertyAccessor<String,CapitalAccount> commentAccessor = null;

    @Override
	public PropertySet registerProperties() {
		IPropertyControlFactory<CapitalAccount,String> textControlFactory = new TextControlFactory<CapitalAccount>();
		IPropertyControlFactory<CapitalAccount,String> commentControlFactory = new MultiTextControlFactory<CapitalAccount>();
		
		IListGetter<CapitalAccount, CapitalAccount> accountGetter = new IListGetter<CapitalAccount, CapitalAccount>() {
			@Override
			public ObjectCollection<CapitalAccount> getList(CapitalAccount parentObject) {
				return parentObject.getSubAccountCollection();
			}
		};
		
		subAccountAccessor = propertySet.addPropertyList("subAccount", Messages.CapitalAccountInfo_subAccount, CapitalAccountInfo.getPropertySet(), accountGetter); //$NON-NLS-1$
		abbreviationAccessor = propertySet.addProperty("abbreviation", Messages.CapitalAccountInfo_Abbreviation, String.class, 5, 70, textControlFactory, null); //$NON-NLS-1$
		commentAccessor      = propertySet.addProperty("comment", Messages.CapitalAccountInfo_Comment, String.class, 5, 150, commentControlFactory, null); //$NON-NLS-1$
		
		propertySet.setIcon(JMoneyPlugin.createImageDescriptor("account.gif")); //$NON-NLS-1$
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<CapitalAccount> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ListPropertyAccessor<CapitalAccount,CapitalAccount> getSubAccountAccessor() {
		return subAccountAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,CapitalAccount> getAbbreviationAccessor() {
		return abbreviationAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String,CapitalAccount> getCommentAccessor() {
		return commentAccessor;
	}	
}
