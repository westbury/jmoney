/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2010 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.views.feedback;

import net.sf.jmoney.JMoneyPlugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;

class ExecuteAgainAction extends Action {
	private FeedbackView fView;
	
	public ExecuteAgainAction(FeedbackView view) {
		setToolTipText("Perform the Current Action Again"); 
		setImageDescriptor(JMoneyPlugin.imageDescriptorFromPlugin(JMoneyPlugin.PLUGIN_ID, "icons/elcl16/search_again.gif"));
		fView = view;	
	}

	@Override
	public void run() {
		/*
		 * This action should not be enabled if no results are being shown or if
		 * the action succeeded (albeit with warnings or info), or if the action
		 * cannot be re-executed.
		 */
		Feedback feedback = fView.getCurrentFeedback();
		IStatus results = feedback.executeAgain();
		fView.refreshResults(results);
	}
}
