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

import net.sf.jmoney.navigator.JMoneyCommonNavigator;
import net.sf.jmoney.wizards.NewAccountWizard;
import net.sf.jmoney.wizards.NewIncomeExpenseAccountWizard;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * The one and only perspective for JMoney.
 * 
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class JMoneyPerspective implements IPerspectiveFactory {

    public static final String ID_PERSPECTIVE = "net.sf.jmoney.JMoneyPerspective"; //$NON-NLS-1$

    public static final String ERROR_LOG_VIEW_ID = "org.eclipse.pde.runtime.LogView"; //$NON-NLS-1$

    @Override
	public void createInitialLayout(IPageLayout layout) {
        IFolderLayout navigator = layout.createFolder("navigator", IPageLayout.LEFT, 0.2f, layout.getEditorArea()); //$NON-NLS-1$
        navigator.addView(JMoneyCommonNavigator.ID);

        layout.addView(ERROR_LOG_VIEW_ID, IPageLayout.BOTTOM, .75f, layout.getEditorArea());

        layout.addShowViewShortcut(JMoneyCommonNavigator.ID);
        layout.addShowViewShortcut("org.eclipse.ui.views.PropertySheet"); //$NON-NLS-1$
        layout.addShowViewShortcut(ERROR_LOG_VIEW_ID);

        layout.addNewWizardShortcut(NewAccountWizard.ID);
        layout.addNewWizardShortcut(NewIncomeExpenseAccountWizard.ID);
    }

}