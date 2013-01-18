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

import java.util.Calendar;
import java.util.Date;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


/**
 * A control for entering dates.  This control contains both a
 * text box and a button to the right that pops up a calendar.
 * <P>
 * This control uses the current date format set in the JMoney
 * preferences.
 * 
 * @author Nigel Westbury
 */
public class DateControl extends DateComposite {

    // TODO Listen to date format changes.
    private VerySimpleDateFormat fDateFormat = new VerySimpleDateFormat(JMoneyPlugin.getDefault().getDateFormat());

	/**
	 * The text box containing the date
	 */
	private Text textControl;

	/**
	 * The small button to the right of the date that brings up the date picker
	 */
	private Button button;

	static private Image threeDotsImage = null;

	private DateTime swtcal = null;
	
	/**
	 * @param parent
	 * @param style
	 */
	public DateControl(final Composite parent) {
		super(parent, SWT.NULL);

		setBackgroundMode(SWT.INHERIT_FORCE);
		
		setLayout(new DialogCellLayout());

		textControl = new Text(this, SWT.LEFT);
		
		textControl.addKeyListener(new KeyAdapter() {
		    @Override	
			public void keyPressed(KeyEvent e) {
				// CTRL + and CTRL - increment and decrement the date respectively.
				// It would be even easier for the user the CTRL did not have to be
				// pressed, but then it would not be possible to have a date format
				// that contains '-'.
				if (e.stateMask == SWT.CONTROL
						&& (e.character == '+' || e.character == '-')) {
					
		            Calendar calendar = Calendar.getInstance();
		            calendar.setTime(
		            		fDateFormat.parse(
		            				textControl.getText()
		            		)
		            );
	            	
					if (e.character == '+') {
						calendar.add(Calendar.DAY_OF_MONTH, 1);
					} else {
		            	calendar.add(Calendar.DAY_OF_MONTH, -1);
					}
					
					textControl.setText(
							fDateFormat.format(
									calendar.getTime()
							)
					);

					e.doit = false;
				}
			}
		});
		
		button = new Button(this, SWT.DOWN);
		if (threeDotsImage == null) {
			ImageDescriptor descriptor = JMoneyPlugin.createImageDescriptor("dots_button.gif"); //$NON-NLS-1$
			threeDotsImage = descriptor.createImage();
		}
		button.setImage(threeDotsImage);

		button.addSelectionListener(new SelectionAdapter() {
		    @Override	
			public void widgetSelected(SelectionEvent event) {
				final Shell shell = new Shell(parent.getShell(), SWT.ON_TOP);
		        shell.setLayout(new RowLayout());
    	        swtcal = new DateTime(shell, SWT.CALENDAR);
    	        
                // Set the currently set date into the calendar control
                // (If the parse method returned null then the text control did not
                // contain a valid date.  In this case no date is set into the
                // date picker).
    	        try {
        	        String t = textControl.getText();
    	        	Date date = fDateFormat.parse(t);
           	        Calendar calendar = Calendar.getInstance();
        	        calendar.setTime(date);
           	        swtcal.setYear(calendar.get(Calendar.YEAR));
           	        swtcal.setMonth(calendar.get(Calendar.MONTH));
           	        swtcal.setDay(calendar.get(Calendar.DAY_OF_MONTH));
    	        } catch (IllegalArgumentException e) {
    	        	// The date format was invalid.
    	        	// Ignore this error (the calendar control
    	        	// does not require a date to be set).
    	        }
                
    	        swtcal.addSelectionListener(
    	        		new SelectionAdapter() {
    	        			@Override
    	        			public void widgetSelected(SelectionEvent e) {
    	        				Calendar cal = Calendar.getInstance();

    	        				/*
    	        				 * First reset all fields. Otherwise differences in the time
    	        				 * part, even if the difference is only milliseconds, will cause
    	        				 * the date comparisons to fail.
    	        				 * 
    	        				 * Note also it is critical that whatever is done here is exactly the
    	        				 * same as that done in VerySimpleDateFormat, otherwise dates will not
    	        				 * match.  For example, if you replace clear() here with setTimeInMillis(0)
    	        				 * then we get a different object (because data other than the date and time
    	        				 * such as time zone information will be different).
    	        				 */ 
    	        				cal.clear();

    	        				cal.set(swtcal.getYear(), swtcal.getMonth(), swtcal.getDay());
    	        				Date date = cal.getTime();
    	        				textControl.setText(fDateFormat.format(date));
    	        			}
    	        		}
    	        );

    	        shell.pack();
    	        
    	        // Position the calendar shell below the date control,
    	        // unless the date control is so near the bottom of the display that
    	        // the calendar control would go off the bottom of the display,
    	        // in which case position the calendar shell above the date control.
    	        Display display = getDisplay();
    	        Rectangle rect = display.map(parent, null, getBounds());
    	        int calendarShellHeight = shell.getSize().y;
    	        if (rect.y + rect.height + calendarShellHeight <= display.getBounds().height) {
        	        shell.setLocation(rect.x, rect.y + rect.height);
    	        } else {
        	        shell.setLocation(rect.x, rect.y - calendarShellHeight);
    	        }
    	        
    	        shell.open();
    	        
    	        shell.addShellListener(new ShellAdapter() {
    	    	    @Override	
    	        	public void shellDeactivated(ShellEvent e) {
    	        		shell.close();
    	        		swtcal = null;
    	        	}
    	        });
			}
		});
	}

	/**
	 * Internal class for laying out the dialog.
	 */
	private class DialogCellLayout extends Layout {
	    @Override	
		public void layout(Composite editor, boolean force) {
			Rectangle bounds = editor.getClientArea();
			Point size = textControl.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
			textControl.setBounds(0, 0, bounds.width-size.y, bounds.height);
			button.setBounds(bounds.width-size.y, 0, size.y, bounds.height);
		}

	    @Override	
		public Point computeSize(Composite editor, int wHint, int hHint, boolean force) {
			if (JMoneyPlugin.DEBUG) System.out.println("wHint =" + wHint + ", " + hHint); //$NON-NLS-1$ //$NON-NLS-2$
			if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT)
				return new Point(wHint, hHint);
			Point contentsSize = textControl.computeSize(SWT.DEFAULT, SWT.DEFAULT, force); 
			Point buttonSize =  button.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
			if (JMoneyPlugin.DEBUG) System.out.println("contents =" + contentsSize.x + ", " + contentsSize.y); //$NON-NLS-1$ //$NON-NLS-2$
			if (JMoneyPlugin.DEBUG) System.out.println("contents =" + buttonSize.x + ", " + buttonSize.y); //$NON-NLS-1$ //$NON-NLS-2$
			// Just return the button width to ensure the button is not clipped
			// if the label is long.  Date text needs 60
			// The label will just use whatever extra width there is
			Point result = new Point(60 + buttonSize.x,
//							        Math.max(contentsSize.y, buttonSize.y));
				contentsSize.y);
			return result;			
		}
	}

	/**
	 * @param object
	 */
    @Override	
	public void setDate(Date date) {
		if (date == null) {
        textControl.setText(""); //$NON-NLS-1$
	} else {
        this.textControl.setText(fDateFormat.format(date));
	}
}

	/**
	 * @return the date, or null if a valid date is not set in
	 * 				the control
	 */
    @Override	
	public Date getDate() {
        String text = textControl.getText();
        try {
        	return fDateFormat.parse(text);
        } catch (IllegalArgumentException e) {
        	return null;
        }
	}

	/**
	 * @param listener
	 */
	public void addModifyListener(ModifyListener listener) {
		textControl.addModifyListener(listener);
	}

	/**
	 * @param listener
	 */
	public void removeModifyListener(ModifyListener listener) {
		textControl.removeModifyListener(listener);
	}
}