/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2012 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.fields;

import java.io.InputStream;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * An input dialog for soliciting a statement number or statement date from the user.
 * 
 * @author Nigel Westbury
 */
class PictureDialog extends Dialog {

	private IBlob blob;

	private Image image;
	
	/**
	 * Creates a dialog that displays the picture.
	 */
	public PictureDialog(Shell parentShell, IBlob blob) {
		super(parentShell);
		this.blob = blob;

		try {
			InputStream inputStream = blob.createStream();
			image = new Image(parentShell.getDisplay(), inputStream);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public boolean close() {
		boolean isNowClosed = super.close();
		if (isNowClosed) {
    		image.dispose();
			blob.close();
		}
		return isNowClosed;
	}
	
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Image");
		shell.setSize(image.getBounds().width + 10, image.getBounds().height + 10);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create Close button only
		createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.CLOSE_LABEL, true);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		composite.setLayout(new FillLayout());

	    Canvas canvas = new Canvas(composite, SWT.NONE);

	    canvas.addPaintListener(new PaintListener() {
	    	@Override
	    	public void paintControl(PaintEvent e) {
	    		e.gc.drawImage(image, 5, 5);
	    	}
	    });
		
		return composite;
	}
}
