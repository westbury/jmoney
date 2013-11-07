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

package net.sf.jmoney.fields;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;


/**
 * A control that shows a button that opens up a picture.
 */
public class ImageControl extends Composite {

	static private Image threeDotsImage = null;

	private Button button;

	private IBlob blob;

	/**
	 * @param parent
	 * @param style
	 */
	public ImageControl(final Composite parent) {
		super(parent, SWT.NULL);

		setBackgroundMode(SWT.INHERIT_FORCE);
		
		setLayout(new FillLayout());

		button = new Button(this, SWT.PUSH);
//		if (threeDotsImage == null) {
//			ImageDescriptor descriptor = JMoneyPlugin.createImageDescriptor("dots_button.gif"); //$NON-NLS-1$
//			threeDotsImage = descriptor.createImage();
//		}
//		button.setImage(threeDotsImage);

		if (blob == null) {
			button.setText("No Picture");
		} else {
			button.setText("View Picture");
		}

		button.setEnabled(blob != null);
		
		button.addSelectionListener(new SelectionAdapter() {
		    @Override	
			public void widgetSelected(SelectionEvent event) {
		    	new PictureDialog(parent.getShell(), blob).open();
		    	
// This code drops-down a shell to show the picture.  Not sure if this is as
// useful as a dialog...
		    	
//				final Shell shell = new Shell(parent.getShell(), SWT.ON_TOP);
//		        shell.setLayout(new RowLayout());
//			    Canvas canvas = new Canvas(shell, SWT.NONE);
//
//			    canvas.addPaintListener(new PaintListener() {
//			    	@Override
//			    	public void paintControl(PaintEvent e) {
//			    		try {
//			    			InputStream inputStream = blob.createStream();
//			    			
////			    			Image image = blob.getImage();
//			    			
//			    			Image image = new Image(getShell().getDisplay(), inputStream);
//				    		e.gc.drawImage(image, 5, 5);
//				    		image.dispose();
//			    		} catch (Exception e1) {
//			    			e1.printStackTrace();
//			    		}
//			    	}
//			    });
//
//    	        shell.pack();
//    	        
//    	        // Position the calendar shell below the date control,
//    	        // unless the date control is so near the bottom of the display that
//    	        // the calendar control would go off the bottom of the display,
//    	        // in which case position the calendar shell above the date control.
//    	        Display display = getDisplay();
//    	        Rectangle rect = display.map(parent, null, getBounds());
//    	        int calendarShellHeight = shell.getSize().y;
//    	        if (rect.y + rect.height + calendarShellHeight <= display.getBounds().height) {
//        	        shell.setLocation(rect.x, rect.y + rect.height);
//    	        } else {
//        	        shell.setLocation(rect.x, rect.y - calendarShellHeight);
//    	        }
//    	        
//    	        shell.open();
//    	        
//    	        shell.addShellListener(new ShellAdapter() {
//    	    	    @Override	
//    	        	public void shellDeactivated(ShellEvent e) {
//    	        		shell.close();
//    	        	}
//    	        });
			}
		});
	}

	/**
	 * @param blob
	 */
	public void setBlob(IBlob blob) {
    	this.blob = blob;

    	if (blob == null) {
			button.setText("None");
		} else {
			button.setText("View");
		}

		button.setEnabled(blob != null);
}

	/**
	 * @return the blob, or null if no image is set in
	 * 				the control
	 */
 	public IBlob getBlob() {
        return blob;
	}
}