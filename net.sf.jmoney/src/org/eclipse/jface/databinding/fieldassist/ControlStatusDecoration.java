/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jface.databinding.fieldassist;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @since 3.2
 *
 */
public class ControlStatusDecoration {

	private ControlDecoration decoration;

	/**
	 * @param control
	 */
	public ControlStatusDecoration(Control control) {
		this(control, SWT.LEFT | SWT.TOP);
	}

	/**
	 * @param control
	 * @param position
	 */
	public ControlStatusDecoration(Control control, int position) {
		this(control, position, null);
	}

	/**
	 * @param control
	 * @param position
	 * @param constrainingComposite
	 */
	public ControlStatusDecoration(Control control, int position,
			Composite constrainingComposite) {
		decoration = new ControlDecoration(control, position,
				constrainingComposite);
	}

	/**
	 * Updates the visibility, image, and description text of this control
	 * decoration to represent the given status.
	 * 
	 * @param status
	 *            the status to be displayed by the decoration
	 */
	public void update(IStatus status) {
		if (status == null || status.isOK()) {
			decoration.hide();
		} else {
			decoration.setImage(getImage(status));
			decoration.setDescriptionText(getDescriptionText(status));
			decoration.show();
		}
	}

	/**
	 * Returns the description text to show in a ControlDecoration for the given
	 * status. The default implementation of this method returns
	 * status.getMessage().
	 * 
	 * @param status
	 *            the status object.
	 * @return the description text to show in a ControlDecoration for the given
	 *         status.
	 */
	protected String getDescriptionText(IStatus status) {
		return status == null ? "" : status.getMessage(); //$NON-NLS-1$
	}

	/**
	 * Returns an image to display in a ControlDecoration which is appropriate
	 * for the given status. The default implementation of this method returns
	 * an image according to <code>status.getSeverity()</code>:
	 * <ul>
	 * <li>IStatus.OK => No image
	 * <li>IStatus.INFO => FieldDecorationRegistry.DEC_INFORMATION
	 * <li>IStatus.WARNING => FieldDecorationRegistry.DEC_WARNING
	 * <li>IStatus.ERROR => FieldDecorationRegistry.DEC_ERROR
	 * <li>IStatus.CANCEL => FieldDecorationRegistry.DEC_ERROR
	 * <li>Other => No image
	 * </ul>
	 * 
	 * @param status
	 *            the status object.
	 * @return an image to display in a ControlDecoration which is appropriate
	 *         for the given status.
	 */
	protected Image getImage(IStatus status) {
		if (status == null)
			return null;

		String fieldDecorationID = null;
		switch (status.getSeverity()) {
		case IStatus.INFO:
			fieldDecorationID = FieldDecorationRegistry.DEC_INFORMATION;
			break;
		case IStatus.WARNING:
			fieldDecorationID = FieldDecorationRegistry.DEC_WARNING;
			break;
		case IStatus.ERROR:
		case IStatus.CANCEL:
			fieldDecorationID = FieldDecorationRegistry.DEC_ERROR;
			break;
		}

		FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault()
				.getFieldDecoration(fieldDecorationID);
		return fieldDecoration == null ? null : fieldDecoration.getImage();
	}
}
