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

import org.eclipse.jface.action.Action;

class RemoveAllFeedbackAction extends Action {

	private FeedbackView fFeedbackView;

	public RemoveAllFeedbackAction(FeedbackView feedbackView) {
		super("Clear History"); 
		setImageDescriptor(JMoneyPlugin.imageDescriptorFromPlugin(JMoneyPlugin.PLUGIN_ID, "icons/elcl16/search_remall.gif"));
		fFeedbackView= feedbackView;
	}
	
	@Override
	public void run() {
		fFeedbackView.removeAllFeedbackResults();
	}
}
