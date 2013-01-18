/*
 * 
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004, 2009 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.ofx.wizards;

import java.io.File;
import java.text.MessageFormat;

import net.sf.jmoney.ofx.Activator;
import net.sf.jmoney.ofx.OfxImporter;

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
public class OfxImportWizard extends Wizard implements IImportWizard {
	private IWorkbenchWindow window;

	private OfxImportWizardPage mainPage;

	public OfxImportWizard() {
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("OfxImportWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("OfxImportWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.window = workbench.getActiveWorkbenchWindow();

		mainPage = new OfxImportWizardPage(window);
		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		String fileName = mainPage.getFileName();
		if (fileName != null) {
			File ofxFile = new File(fileName);
			
			OfxImporter importer = new OfxImporter(window);
			boolean allImported = importer.importFile(ofxFile);
			
			if (allImported && mainPage.IsDeleteFile()) {
				boolean isDeleted = ofxFile.delete();
				if (!isDeleted) {
					MessageDialog.openWarning(window.getShell(), "OFX file not deleted", 
							MessageFormat.format(
									"All entries in {0} have been imported and an attempt was made to delete the file.  However the file deletion failed.", 
									ofxFile.getName()));
				}
			}
		}

		return true;
	}
}