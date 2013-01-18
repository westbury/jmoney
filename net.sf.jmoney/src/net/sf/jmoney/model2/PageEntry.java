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

import net.sf.jmoney.IBookkeepingPageFactory;
import net.sf.jmoney.JMoneyPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

/**
 * This class contains the information needed to construct a page
 * in an editor.  This class does not do much except to keep a reference
 * to the IConfigurationElement object that is used to load the class that
 * implements the extension to the page extension point.  If and when the
 * page factory is actually needed, this class loads the extension class.
 * By doing this, we follow the Eclipse design guidelines and delay loading
 * plug-ins until we need them.  
 * 
 * @author Nigel Westbury
 */
public class PageEntry {
	String pageId;
	IConfigurationElement pageElement;
	IBookkeepingPageFactory pageFactory = null;
	int position;

	public PageEntry(String pageId, IConfigurationElement pageElement, int position) {
	    this.pageId = pageId;
        this.pageElement = pageElement;
        this.position = position;
	}

	public String getPageId() {
	    return pageId;
	}

	public IBookkeepingPageFactory getPageFactory() {
		if (pageFactory == null) {
			try {
				pageFactory = (IBookkeepingPageFactory)pageElement.createExecutableExtension("class"); //$NON-NLS-1$
			} catch (CoreException e) {
                JMoneyPlugin.log(e);
			}
		}
	    return pageFactory;
	}

	public int getPosition() {
		return position;
	}

}

