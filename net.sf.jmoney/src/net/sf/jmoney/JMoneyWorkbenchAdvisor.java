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

package net.sf.jmoney;

import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

/**
 * The workbench advisor for the JMoney application.
 * 
 * @author Nigel Westbury
 */
public class JMoneyWorkbenchAdvisor extends WorkbenchAdvisor {

    @Override	
	public String getInitialWindowPerspectiveId() {
		return JMoneyPerspective.ID_PERSPECTIVE;
	}

    /* (non-Javadoc)
     * @see org.eclipse.ui.application.WorkbenchAdvisor#createWorkbenchWindowAdvisor(org.eclipse.ui.application.IWorkbenchWindowConfigurer)
     */
    @Override	
    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        return  new JMoneyWorkbenchWindowAdvisor(configurer);
    }
    
    @Override	
    public void initialize(IWorkbenchConfigurer configurer) {
        super.initialize(configurer);

        // Turn on support for the saving and restoring of
        // view states through IMemento interfaces.
        configurer.setSaveAndRestore(true);
    }

    @Override	
    public boolean preWindowShellClose(IWorkbenchWindowConfigurer configurer) {
        // If a session is open, ensure we have all the information we
        // need to close it. Some datastores need additional information
        // to save the session. For example the serialized XML datastore
        // requires a file name which will not have been requested from the
        // user if the datastore has not yet been saved.

        // This call must be done here for two reasons.
        // 1. It ensures that the session data can be saved.
        // 2. The navigation view saves, as part of its state,
        // the datastore which was open when the workbench was
        // last shut down, allowing it to open the same session
        // when the workbench is next opened. In order to do this,
        // the navigation view saves the session details.

        return JMoneyPlugin.getDefault().saveOldSession(configurer.getWindow());
    }

}
