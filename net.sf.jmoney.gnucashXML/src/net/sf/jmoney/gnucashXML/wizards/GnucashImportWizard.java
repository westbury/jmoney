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
package net.sf.jmoney.gnucashXML.wizards;

import java.io.File;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.gnucashXML.GnucashXML;
import net.sf.jmoney.gnucashXML.GnucashXMLPlugin;
import net.sf.jmoney.model2.Session;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * A wizard to import data from a QIF file.
 * 
 * Currently this wizard if a single page wizard that asks only for the file.
 * This feature is implemented as a wizard because the Eclipse workbench import
 * action requires all import implementations to be wizards.
 */
public class GnucashImportWizard extends Wizard implements IImportWizard
{
	private IWorkbenchWindow window;

	private GnucashImportWizardPage mainPage;

	private Session session;

	public GnucashImportWizard()
	{
		IDialogSettings workbenchSettings = GnucashXMLPlugin.getDefault()
				.getDialogSettings();
		IDialogSettings section = workbenchSettings
				.getSection("GnucashImportWizard");//$NON-NLS-1$
		if(section == null)
		{
			section = workbenchSettings.addNewSection("GnucashImportWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		this.window = workbench.getActiveWorkbenchWindow();

		this.session = JMoneyPlugin.getDefault().getSession();

		// Original JMoney disabled the import menu items when no
		// session was open. I don't know how to do that in Eclipse,
		// so we display a message instead.
		if(session == null)
		{
			MessageDialog waitDialog = new MessageDialog(
					window.getShell(),
					"Disabled Action Selected",
					null, // accept the default window icon
					"You cannot import data into an accounting session unless you have a session open.  You must first open a session or create a new session.",
					MessageDialog.INFORMATION,
					new String[] { IDialogConstants.OK_LABEL }, 0);
			waitDialog.open();
			return;
		}

		mainPage = new GnucashImportWizardPage(window);
		addPage(mainPage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish()
	{
		String fileName = mainPage.getFileName();
		if(fileName != null)
		{
			File gnucashFile = new File(fileName);
			GnucashXML importer = GnucashXML.getSingleton(window);
			importer.importFile(session, gnucashFile);
		}

		return true;
	}
}
