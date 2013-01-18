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

import java.util.List;

import net.sf.jmoney.JMoneyPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

class FeedbackHistoryDropDownAction extends Action implements IMenuCreator {

	private class ShowFeedbackFromHistoryAction extends Action {
		private Feedback feedback;

		public ShowFeedbackFromHistoryAction(Feedback feedback) {
	        super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			this.feedback= feedback;
			
			String label= escapeAmp(feedback.getLabel());

			// fix for bug 38049
			// TODO: Try searching for something containing '@' in memo and
			// see if this is necessary.
			if (label.indexOf('@') >= 0)
				label+= '@';
			setText(label);
			setImageDescriptor(feedback.getImageDescriptor());
			setToolTipText(feedback.getTooltip());
		}
		
		private String escapeAmp(String label) {
			StringBuffer buf= new StringBuffer();
			for (int i= 0; i < label.length(); i++) {
				char ch= label.charAt(i);
				buf.append(ch);
				if (ch == '&') {
					buf.append('&');
				}
			}
			return buf.toString();
		}
		
//		public void runWithEvent(Event event) {
//			InternalSearchUI.getInstance().showFeedbackResult(fFeedbackView, fFeedback, event.stateMask == SWT.CTRL);
//		}
		
		@Override
		public void run() {
			fFeedbackView.showResults(feedback);
		}
	}

	public static final int RESULTS_IN_DROP_DOWN = 10;

	private Menu fMenu;
	private FeedbackView fFeedbackView;
	
	public FeedbackHistoryDropDownAction(FeedbackView feedbackView) {
		setToolTipText("Show Feedback for Previous Actions"); 
		setImageDescriptor(JMoneyPlugin.imageDescriptorFromPlugin(JMoneyPlugin.PLUGIN_ID, "icons/elcl16/search_history.gif"));
		fFeedbackView= feedbackView;
		setMenuCreator(this);
	}
	
	public void updateEnablement() {
		boolean hasQueries = fFeedbackView.hasQueries();
		setEnabled(hasQueries);
	}

	@Override
	public void dispose() {
		disposeMenu();
	}

	void disposeMenu() {
		if (fMenu != null)
			fMenu.dispose();
	}

	@Override
	public Menu getMenu(Menu parent) {
		return null;
	}

	@Override
	public Menu getMenu(Control parent) {
		Feedback currentFeedback = fFeedbackView.getCurrentFeedback();
		disposeMenu();
		
		fMenu= new Menu(parent);
				
		List<Feedback> feedbackList = fFeedbackView.getQueries();
		if (feedbackList.size() > 0) {			
			for (Feedback feedback : feedbackList) {
				ShowFeedbackFromHistoryAction action= new ShowFeedbackFromHistoryAction(feedback);
				action.setChecked(feedback.equals(currentFeedback));
				addActionToMenu(fMenu, action);
			}
			new MenuItem(fMenu, SWT.SEPARATOR);
			addActionToMenu(fMenu, new ShowFeedbackHistoryDialogAction(fFeedbackView));
			addActionToMenu(fMenu, new RemoveAllFeedbackAction(fFeedbackView));
		}
		return fMenu;
	}

	protected void addActionToMenu(Menu parent, Action action) {
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(parent, -1);
	}
	
	@Override
	public void run() {
		new ShowFeedbackHistoryDialogAction(fFeedbackView).run();
	}
}
