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

package net.sf.jmoney.preferences;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
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
	public static final String P_PATH = "pathPreference"; //$NON-NLS-1$

	public PreferencePage() {
		super(GRID);
		setPreferenceStore(JMoneyPlugin.getDefault().getPreferenceStore());
		setDescription("JMoney preferences"); //$NON-NLS-1$
		initializeDefaults();
		
		// The title of this page is picked up and used by the
		// preference dialog as the text in the preferences
		// navigation tree.
		setTitle("core JMoney preferences"); //$NON-NLS-1$
	}
/**
 * Sets the default values of the preferences.
 */
	private void initializeDefaults() {
		IPreferenceStore store = getPreferenceStore();
		store.setDefault("booleanPreference", true); //$NON-NLS-1$
		store.setDefault("dateFormat", "yyyy-MM-dd"); //$NON-NLS-1$ //$NON-NLS-2$
		store.setDefault("stringPreference", "Default value"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	@Override	
	public void createFieldEditors() {
		addField(new DirectoryFieldEditor(P_PATH, 
				"&Directory preference:", getFieldEditorParent())); //$NON-NLS-1$
		addField(
			new BooleanFieldEditor(
					"booleanPreference", //$NON-NLS-1$
				"&An example of a boolean preference", //$NON-NLS-1$
				getFieldEditorParent()));

		
		String dateOptions[] = VerySimpleDateFormat.DATE_PATTERNS;
		String dateOptions2[][] = new String[dateOptions.length][];
		for (int i = 0; i < dateOptions.length; i++) {
			dateOptions2[i] = 
				new String[] { dateOptions[i], dateOptions[i] }; 
		}
		
		addField(new RadioGroupFieldEditor(
			"dateFormat", //$NON-NLS-1$
			"Date Format", //$NON-NLS-1$
			1,
			dateOptions2,
			getFieldEditorParent()));
		
		addField(
			new StringFieldEditor("stringPreference", "A &text preference:", getFieldEditorParent())); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	@Override
	public void init(IWorkbench workbench) {
	}
}