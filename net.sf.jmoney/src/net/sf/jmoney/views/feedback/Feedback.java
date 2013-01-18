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
import org.eclipse.jface.resource.ImageDescriptor;

public class Feedback {
	private String description;
	private IStatus rootStatus;

	public Feedback(String description, IStatus rootStatus) {
		this.description = description;
		this.rootStatus = rootStatus;
	}

	/**
	 * Returns the full description of the result messages.
	 */
	String getFullDescription() {
		return description;
	}

	/**
	 * Returns a short description of the action to which this feedback applies.
	 * Cuts off after 30 characters and adds ... The description set by the
	 * client where {0} will be replaced by the match count.
	 * 
	 * @return the short description
	 */
	String getShortDescription() {
		String text= getFullDescription();
		int separatorPos= text.indexOf(" - "); //$NON-NLS-1$
		if (separatorPos < 1)
			return text.substring(0, Math.min(50, text.length())) + "..."; // use first 50 characters //$NON-NLS-1$
		if (separatorPos < 30)
			return text;	// don't cut
		if (text.charAt(0) == '"')
			return text.substring(0, Math.min(30, text.length())) + "...\" - " + text.substring(Math.min(separatorPos + 3, text.length())); //$NON-NLS-1$
		return text.substring(0, Math.min(30, text.length())) + "... - " + text.substring(Math.min(separatorPos + 3, text.length())); //$NON-NLS-1$
	}

	public IStatus getRootStatus() {
		return rootStatus;
	}
	
	/** 
	 * Image used when feedback results are displayed in a list.
	 *  
	 * @return the image descriptor
	 */
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getLabel() {
		return getShortDescription();
	}

	public String getTooltip() {
		return getFullDescription();
	}

	/**
	 * This method indicates if the action can be re-executed.  If this one
	 * overrides this method to return true then you must also override
	 * executeAgain to perform the action.
	 * <P>
	 * This method is intended to be implemented in derived classes.  It is
	 * not required that this method is implemented in derived classes.
	 */
	protected boolean canExecuteAgain() {
		return false;
	}
	
	/**
	 * This method re-executes whatever was run to create these errors
	 * in the first place.
	 * <P>
	 * This method will never throw an exception.  Instead an IStatus object
	 * is set to indicate any errors.
	 * <P>
	 * This method is intended to be implemented in derived classes.  It is
	 * not required that this method is implemented in derived classes.
	 */
	protected IStatus executeAgain() {
		throw new RuntimeException("canExecuteAgain has been overridden to return true but no implementation of executeAgain was provided.");
	}
}

