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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.IMemento;


public class StatusFilter extends ViewerFilter {
	private static final String TAG_ENABLED = "enabled"; //$NON-NLS-1$
	private static final String TAG_SELECT_BY_SEVERITY = "selectBySeverity"; //$NON-NLS-1$
	private static final String TAG_SEVERITY = "severity"; //$NON-NLS-1$

	private static final String TAG_DIALOG_SECTION = "problemViewFilterDialog"; //$NON-NLS-1$

	protected boolean enabled;
	private boolean selectBySeverity;
	private int severityMask;

	public StatusFilter() {
		resetState();
	}
	
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		IStatus status = (IStatus)element;
		
		return !isEnabled() 
		|| selectBySeverity(status);
	}
	
	/**
	 * @return
	 * <ul>
	 * <li><code>true</code> if the filter is enabled.</li>
	 * <li><code>false</code> if the filter is not enabled.</li>
	 * </ul>
	 */
	boolean isEnabled() {
		return enabled;
	}

	/**
	 * Sets the enablement state of the filter.
	 */
	void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	private boolean selectBySeverity(IStatus statusElement) {
		if (selectBySeverity) {
			return statusElement.matches(severityMask);
		}
		
		return true;
	}
	
	public boolean getSelectBySeverity() {
		return selectBySeverity;
	}

	public int getSeverity() {
		return severityMask;
	}

	public void setSelectBySeverity(boolean selectBySeverity) {
		this.selectBySeverity = selectBySeverity;
	}

	public void setSeverity(int severity) {
		this.severityMask = severity;
	}

	public void resetState() {
		enabled = true;
	
		selectBySeverity = false;
		severityMask = 0;		
	}

	public void init(IMemento memento) {
		if (memento != null) {
			String enabledString = memento.getString(TAG_ENABLED);
			if (enabledString != null) {
				enabled = Boolean.parseBoolean(enabledString);
			}
			
			String selectBySeverityString = memento.getString(TAG_SELECT_BY_SEVERITY);
			if (selectBySeverityString != null) {
				selectBySeverity = Boolean.parseBoolean(selectBySeverityString);
			}
			
			Integer severityInteger = memento.getInteger(TAG_SEVERITY);
			if (severityInteger != null) {
				severityMask = severityInteger;		
			}
		}
	}
	
	public void saveState(IMemento memento) {
		memento.putString(TAG_ENABLED, Boolean.toString(enabled));

		memento.putString(TAG_SELECT_BY_SEVERITY, Boolean.toString(selectBySeverity));
		memento.putInteger(TAG_SEVERITY, severityMask);
	}
	
	public void restoreState(IDialogSettings dialogSettings) {		
		resetState();		
		IDialogSettings settings = dialogSettings.getSection(TAG_DIALOG_SECTION);
		
		if (settings != null) {
			selectBySeverity = settings.getBoolean(TAG_ENABLED);
			selectBySeverity = settings.getBoolean(TAG_SELECT_BY_SEVERITY);
			severityMask = settings.getInt(TAG_SEVERITY);
		}
	}
/*
	public void saveState(IDialogSettings dialogSettings) {
		if (dialogSettings != null) {
			IDialogSettings settings = dialogSettings.getSection(TAG_DIALOG_SECTION);

			if (settings == null)
				settings = dialogSettings.addNewSection(TAG_DIALOG_SECTION);

			settings.put(TAG_ENABLED, enabled);

			String markerTypeIds = ""; //$NON-NLS-1$
		
			for (int i = 0; i < selectedTypes.size(); i++) {
				ProblemCategory markerType = (ProblemCategory) selectedTypes.get(i);
				markerTypeIds += markerType.toString() + ":";
			}
		
			settings.put(TAG_SELECTED_TYPES, markerTypeIds);
		
			settings.put(TAG_SELECT_BY_SEVERITY, selectBySeverity);
			settings.put(TAG_SEVERITY, severity);
		}
	}
*/	
}
