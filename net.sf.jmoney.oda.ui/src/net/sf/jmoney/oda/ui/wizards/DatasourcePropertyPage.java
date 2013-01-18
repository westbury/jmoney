/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2007 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.oda.ui.wizards;

import java.util.Properties;

import org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSourceEditorPage;
import org.eclipse.swt.widgets.Composite;

public class DatasourcePropertyPage extends DataSourceEditorPage
{
    private DatasourcePageHelper pageHelper;

    public DatasourcePropertyPage()
    {
        super();
    }

    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSourceEditorPage#collectCustomProperties(java.util.Properties)
     */
	@Override 
    public Properties collectCustomProperties( Properties profileProperties )
    {
        /* 
         * Optionally assigns a custom designer state, for inclusion
         * in the ODA design session response, using
         *      setResponseDesignerState( DesignerState customState ); 
         */

        if (pageHelper == null) {
            return profileProperties;
        }

        return pageHelper.collectCustomProperties(profileProperties);
    }

    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSourceEditorPage#createAndInitCustomControl(org.eclipse.swt.widgets.Composite, java.util.Properties)
     */
	@Override 
    protected void createAndInitCustomControl( Composite parent, Properties profileProps )
    {
        if (pageHelper == null) {
            pageHelper = new DatasourcePageHelper(this);
        }

        pageHelper.createCustomControl(parent);

        /* 
         * Optionally hides the Test Connection button, using
         *      setPingButtonVisible( false );  
         */

        /* 
         * Optionally restores the state of a previous design session.
         * Obtains designer state, using
         *      getInitializationDesignerState(); 
         */
        pageHelper.initCustomControl(profileProps);
        
        if(!isSessionEditable()) {
            getControl().setEnabled(false);
        }
    }
}
