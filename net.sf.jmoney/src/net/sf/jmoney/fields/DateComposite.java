/**
 * 
 */
package net.sf.jmoney.fields;

import java.util.Date;

import org.eclipse.swt.widgets.Composite;

public abstract class DateComposite extends Composite {
	public DateComposite(Composite parent, int style) {
		super(parent, style);
	}

	public abstract Date getDate();
	public abstract void setDate(Date date);
}