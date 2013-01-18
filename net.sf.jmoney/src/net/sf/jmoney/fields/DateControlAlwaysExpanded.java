package net.sf.jmoney.fields;

import java.util.Calendar;
import java.util.Date;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Text;

public class DateControlAlwaysExpanded extends DateComposite {
	protected DateTime swtcal;
	protected Text textControl;
	
    // TODO Listen to date format changes.
    private VerySimpleDateFormat fDateFormat = new VerySimpleDateFormat(JMoneyPlugin.getDefault().getDateFormat());

	public DateControlAlwaysExpanded(Composite parent) {
		super(parent, SWT.NONE);
		
		setLayout(new GridLayout(1, false));
		
        swtcal = new DateTime(this, SWT.CALENDAR);
		textControl = new Text(this, SWT.NONE);
		
        swtcal.addSelectionListener(
        		new SelectionListener() {
					public void widgetDefaultSelected(SelectionEvent e) {
						// TODO Auto-generated method stub
						
					}

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
        		});

	}

    @Override	
	public void setDate(Date date) {
		if (date == null) {
			textControl.setText(""); //$NON-NLS-1$
		} else {
			textControl.setText(fDateFormat.format(date));
			
   	        Calendar calendar = Calendar.getInstance();
	        calendar.setTime(date);
   	        swtcal.setYear(calendar.get(Calendar.YEAR));
   	        swtcal.setMonth(calendar.get(Calendar.MONTH));
   	        swtcal.setDay(calendar.get(Calendar.DAY_OF_MONTH));
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
}
