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

import org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSourceWizardPage;
import org.eclipse.swt.widgets.Composite;

public class DatasourceWizardPage extends DataSourceWizardPage
{
    private DatasourcePageHelper pageHelper;
    private Properties datasourceProperties;

    public DatasourceWizardPage(String pageName) {
        super(pageName);
        setMessage(DatasourcePageHelper.DEFAULT_MESSAGE);
        // page title is specified in extension manifest
        
    }

    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSourceWizardPage#createPageCustomControl(org.eclipse.swt.widgets.Composite)
     */
	@Override 
    public void createPageCustomControl( Composite parent )
    {
        if (pageHelper == null) {
            pageHelper = new DatasourcePageHelper(this);
        }
        pageHelper.createCustomControl( parent );
        pageHelper.initCustomControl( datasourceProperties );   // in case init was called before create 

        /* 
         * Hide the Test Connection button because data is fetched
         * internally from the current open session.
         */
        setPingButtonVisible(false);  
    }

    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSourceWizardPage#initPageCustomControl(java.util.Properties)
     */
	@Override 
    public void setInitialProperties(Properties dataSourceProps) {
        this.datasourceProperties = dataSourceProps;
        if (pageHelper == null) {
            return;     // ignore, wait till createPageCustomControl to initialize
        }
        pageHelper.initCustomControl(datasourceProperties);        
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSourceWizardPage#collectCustomProperties()
     */
	@Override 
    public Properties collectCustomProperties() {
        /* 
         * Optionally assign a custom designer state, for inclusion
         * in the ODA design session response, using
         * setResponseDesignerState( DesignerState customState ); 
         */
        
        if (pageHelper != null) { 
            return pageHelper.collectCustomProperties(datasourceProperties);
        }

        return (datasourceProperties != null)
        	? datasourceProperties 
        	: new Properties();
    }
}
