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

package net.sf.jmoney.jdbcdatastore;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */


public class PreferencePage
extends FieldEditorPreferencePage
implements IWorkbenchPreferencePage {

	StringFieldEditor driverField;
	StringFieldEditor subProtocolField;
	StringFieldEditor subProtocolDataField;
	StringFieldEditor userField;
	StringFieldEditor passwordField;
	
	public PreferencePage() {
		super(GRID);
		setPreferenceStore(JDBCDatastorePlugin.getDefault().getPreferenceStore());

		// Set text displayed at the top of the preference page,
		// underneath the title bar.
		setDescription("Settings used to connect to a JDBC database containing JMoney accounting data.");

		// The title of this page is picked up and used by the
		// preference dialog as the text in the preferences
		// navigation tree.
		setTitle(JDBCDatastorePlugin.getResourceString("preferencePageTitle"));
	}

	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	@Override
	public void createFieldEditors() {
		/* TODO: Add support for these radio buttons so users do not
		 * have to manually enter the values for known drivers.
		addField(new RadioGroupFieldEditor(
				"driverOption",
				"JDBC Driver Selection",
				1,
				new String[][] { 
						{ "HSQLDB", "&HSQL driver for HSQL database" },
						{ "JTDS", "&JTDS driver for Microsoft SQL Server" },
						{ "Other", "&Other JDBC Driver" }
				}, 
				getFieldEditorParent()));
		 */		
		driverField = new StringFieldEditor("driver", "Driver:", getFieldEditorParent());
		addField(driverField);
		subProtocolField = new StringFieldEditor("subProtocol", "Sub-Protocol:", getFieldEditorParent());
		addField(subProtocolField);
		subProtocolDataField = new StringFieldEditor("subProtocolData", "Sub-Protocol Data:", getFieldEditorParent());
		addField(subProtocolDataField);
		userField = new StringFieldEditor("user", "User:", getFieldEditorParent());
		addField(userField);
		passwordField = new StringFieldEditor("password", "Password:", getFieldEditorParent());
		addField(passwordField);

		addField(
				new BooleanFieldEditor(
						"promptEachTime",
						"Always &prompt for connection details each open",
						getFieldEditorParent()));
		
        // Add buttons to set hard-coded values
	    Composite parent = getFieldEditorParent();
        createSetDefaultsButtons(parent).setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());
	}
	
	private Control createSetDefaultsButtons(Composite parent) {
        Composite buttonComposite = new Composite(parent, SWT.NONE);
        buttonComposite.setLayout(new RowLayout(SWT.HORIZONTAL));

        Button setDerbyButton = new Button(buttonComposite, SWT.PUSH);
        setDerbyButton.setText("Set Derby Values");
        setDerbyButton.addListener(SWT.Selection, e -> setDerbyValues());

        Button setLocalPostgresButton = new Button(buttonComposite, SWT.PUSH);
        setLocalPostgresButton.setText("Set local Postgresql Values");
        setLocalPostgresButton.addListener(SWT.Selection, e -> setLocalPostgresValues());

        Button setNeonButton = new Button(buttonComposite, SWT.PUSH);
        setNeonButton.setText("Set neon.tech Values");
        setNeonButton.addListener(SWT.Selection, e -> setNeonValues());
        
        return buttonComposite;
    }

    private void setDerbyValues() {
        getPreferenceStore().setValue("driver", "org.apache.derby.jdbc.EmbeddedDriver");
        getPreferenceStore().setValue("subProtocol", "derby");
        getPreferenceStore().setValue("subProtocolData", "/Users/nigel/JMoneyAccounts-test");
        getPreferenceStore().setValue("user", "sa");
        getPreferenceStore().setValue("password", "");

        // Refresh the fields to show updated values
        refreshFieldEditors();
    }

  
    private void setLocalPostgresValues() {
        getPreferenceStore().setValue("driver", "org.postgresql.Driver");
        getPreferenceStore().setValue("subProtocol", "postgresql");
        getPreferenceStore().setValue("subProtocolData", "//localhost:5432/accounts");
        getPreferenceStore().setValue("user", "<<<username>>>");
        getPreferenceStore().setValue("password", "");

        // Refresh the fields to show updated values
        refreshFieldEditors();
    }

    private void setNeonValues() {
        getPreferenceStore().setValue("driver", "org.postgresql.Driver");
        getPreferenceStore().setValue("subProtocol", "postgresql");
        getPreferenceStore().setValue("subProtocolData", "//<<<aws-host>>>.aws.neon.tech/Accounts?sslmode=require");
        getPreferenceStore().setValue("user", "Accounts_owner");
        getPreferenceStore().setValue("password", "<<<password>>>");

        // Refresh the fields to show updated values
        refreshFieldEditors();
    }

    private void refreshFieldEditors() {
       	driverField.load();
       	subProtocolField.load();
       	subProtocolDataField.load();
       	userField.load();
       	passwordField.load();
    }

	public void init(IWorkbench workbench) {
	}
}