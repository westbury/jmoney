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

package net.sf.jmoney.fields;

import java.util.Comparator;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.swt.widgets.Composite;

/**
 * A control factory to edit images.  Well, we don't allow the users to actually
 * edit an image, this is an accounting program after all.  But we do allow users
 * to select images.  In practice images generally are fetched when accounting information
 * is imported from a site such as Paypal or Amazon.  Few users are going to take pictures
 * of their purchases and manually attach pictures, so this is really only here for
 * completeness.
 * 
 * @author Nigel Westbury
 */
public class ImageControlFactory implements IPropertyControlFactory<IBlob> {

    @Override
	public IPropertyControl createPropertyControl(Composite parent, ScalarPropertyAccessor<IBlob,?> propertyAccessor) {
        return new ImageEditor(parent, propertyAccessor);
    }

    @Override
	public String formatValueForMessage(ExtendableObject extendableObject,
            ScalarPropertyAccessor<? extends IBlob,?> propertyAccessor) {
        // What do we do here?
        return "picture";
    }

    @Override
	public String formatValueForTable(ExtendableObject extendableObject,
            ScalarPropertyAccessor<? extends IBlob,?> propertyAccessor) {
        // What do we do here?
        return "picture";
    }

	@Override
	public IBlob getDefaultValue() {
		return null;
	}

    @Override
	public boolean isEditable() {
        return false;
    }

	@Override
	public Comparator<IBlob> getComparator() {
		return null;
	}
}