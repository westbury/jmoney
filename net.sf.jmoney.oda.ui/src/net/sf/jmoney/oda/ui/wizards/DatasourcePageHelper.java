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

import net.sf.jmoney.oda.ui.Messages;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class DatasourcePageHelper
{
    private WizardPage wizardPage;
    private PreferencePage propertyPage;

    static String DEFAULT_MESSAGE = Messages.getString("wizard.defaultMessage.pressFinish");
    
    DatasourcePageHelper(WizardPage wizardPage) {
        this.wizardPage = wizardPage;
    }

    DatasourcePageHelper(PreferencePage propertyPage) {
        this.propertyPage = propertyPage;
    }

    void createCustomControl(Composite parent) {
        Composite content = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout(3, false);
        content.setLayout(layout);
        
        Label label = new Label(content, SWT.NONE);
        label.setText( Messages.getString("label.noPropertiesMessage")); //$NON-NLS-1$

        setMessage(DEFAULT_MESSAGE, IMessageProvider.NONE); //$NON-NLS-1$
        setPageComplete(true);
    }
    
    Properties collectCustomProperties(Properties properties) {
    	Properties result = properties;

    	if (properties == null) {
    		result = new Properties();
    	}
        
        // Normally the properties would be set from the controls here,
        // but this datasource has no properties.

        return result;
    }
    
    void initCustomControl(Properties profileProperties) {
    	// There are no properties to set.
    }

    private void setPageComplete(boolean complete)
    {
        if (wizardPage != null) {
            wizardPage.setPageComplete(complete);
        } else if (propertyPage != null) {
            propertyPage.setValid(complete);
        }
    }
    
    private void setMessage(String newMessage, int newType) {
        if (wizardPage != null) {
            wizardPage.setMessage(newMessage, newType);
        } else if (propertyPage != null) {
            propertyPage.setMessage(newMessage, newType);
        }
    }
}
