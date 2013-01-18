/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002, 2004 Johann Gyger <jgyger@users.sf.net>
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

package net.sf.jmoney.pages.entries;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import net.sf.jmoney.entrytable.EntryData;

/**
 * TODO Move to non-UI plug-in
 * 
 * @author Johann Gyger
 */
public class EntriesFilter {

    /**
     * Filter type, defined as index of FILTER_TYPES.
     */
    protected int filterColumnIndex = 0;
	
	/**
     * Filter pattern.  An empty filter pattern indicates that filtering is off.
     */
    protected String pattern = ""; //$NON-NLS-1$

    protected transient PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    public EntriesFilter() {
    }
    
	/**
     * @return The filter pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Set the filter pattern. The pattern is not treated case-sensitive.
     * TODO Maybe support regular expressions
     * 
     * @param aPattern Filter pattern
     */
    public void setPattern(String aPattern) {
        String normalizedPattern = 
        	(aPattern == null)
        	? "" //$NON-NLS-1$
        			: aPattern.toLowerCase();
        if (!normalizedPattern.equals(pattern)) {
            pattern = aPattern;
            changeSupport.firePropertyChange("pattern", null, null); //$NON-NLS-1$
        }
    }

	/**
     * Get the filter type which is equivalent to the array index returned by
     * <code>#getFilterType()</code>.
     * 
     * @return Filter type
     */
    public int getFilterColumn() {
        return filterColumnIndex;
    }

	/**
     * Set the filter type.
     * 
     * @param type Filter type
     */
    public void setType(int aType) {
        if (filterColumnIndex == aType) return;
        filterColumnIndex = aType;
        changeSupport.firePropertyChange("type", null, null); //$NON-NLS-1$
    }

    /**
     * @return Is the filter pattern empty?
     */
    public boolean isEmpty() {
        return pattern.length() == 0;
    }

    /**
     * Filter the entry according to the provided filter criteria.
     * 
     * The filter matching is on the text as it is displayed in the table.
     * 
     * @param IDisplayableItem item representing the row in the table
     *			to be filered 			
     * @return True, if "entry" matches the filter criteria; false, else
     */
	public boolean filterEntry(EntryData data) {
		if (pattern.equals("")) { //$NON-NLS-1$
			// Filter is not active so all entries match
			return true;
		} else {
/* TODO: fix this.  Filter should be based on property, not IEntriesProperty.			
			if (filterColumnIndex == 0) {
				// 'Entry' selected.  Entry matches if any of the properties
				// match.
				for (IEntriesTableProperty entriesSectionProperty: fPage.allEntryDataObjects) {
					String text = entriesSectionProperty.getValueFormattedForTable(data);
					if (containsPattern(text)) {
						return true;
					}
				}
				return false;
			} else {
				IEntriesTableProperty entriesSectionProperty = (IEntriesTableProperty)fPage.allEntryDataObjects.get(filterColumnIndex-1);
				String text = entriesSectionProperty.getValueFormattedForTable(data);
				return containsPattern(text);
			}
*/
			return false;
		}
    }
	
	/**
	 * Add a PropertyChangeListener.
	 */
	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		changeSupport.addPropertyChangeListener(pcl);
	}

	/**
	 * Remove a PropertyChangeListener.
	 */
	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		changeSupport.removePropertyChangeListener(pcl);
	}

    /**
     * @param s String to check
     * @return Does "s" contain the pattern?
     */
    protected boolean containsPattern(String s) {
        if (isEmpty())
            return true;
        else
            return (s != null) && (s.toLowerCase().indexOf(pattern) >= 0);
    }

}
