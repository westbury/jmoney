/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2007 Nigel Westbury <westbury@users.sf.net>
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

package net.sf.jmoney.entrytable;

import java.util.Vector;

import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * This class represents columns containing data from the other
 * entries.  If there are multiple other entries (split entries)
 * then these values are displayed in a list.  This creates variable
 * height rows.
 * 
 * @author Nigel Westbury
 */
public class OtherEntriesPropertyBlock extends IndividualBlock<EntryData, EntryRowControl> {
	protected ScalarPropertyAccessor<?,Entry> accessor;
	private String id;
	
	public OtherEntriesPropertyBlock(ScalarPropertyAccessor<?,Entry> accessor) {
		super(
				accessor.getDisplayName(),
				accessor.getMinimumWidth(),
				accessor.getWeight()
		);

		this.accessor = accessor;
		this.id = "other." + accessor.getName(); //$NON-NLS-1$
	}

	/**
	 * This version of the constructor allows the display name to be overwritten.
	 *
	 * @param accessor
	 * @param displayName
	 */
	public OtherEntriesPropertyBlock(ScalarPropertyAccessor accessor, String displayName) {
		super(
				displayName,
				accessor.getMinimumWidth(),
				accessor.getWeight()
		);

		this.accessor = accessor;
		this.id = "other." + accessor.getName(); //$NON-NLS-1$
	}

	public String getId() {
		return id;
	}

    @Override	
	public IPropertyControl<EntryData> createCellControl(Composite parent, RowControl rowControl, EntryRowControl coordinator) {
		// Because this may be multi-valued, setup the container only.
		final Composite composite = new Composite(parent, SWT.NONE);
		
		composite.setBackgroundMode(SWT.INHERIT_FORCE);
		
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 1;
		layout.verticalSpacing = 1;
		composite.setLayout(layout);
		
		return new IPropertyControl<EntryData>() {

			private Vector<IPropertyControl> propertyControls = new Vector<IPropertyControl>();
			private FocusListener controlFocusListener;
			
			@Override
			public Control getControl() {
				return composite;
			}

			@Override
			@SuppressWarnings("unchecked")
			public void load(EntryData data) {
				for (Control child: composite.getChildren()) {
					child.dispose();
				}
				propertyControls.clear();
				
				for (Entry entry: data.getSplitEntries()) {
					IPropertyControl propertyControl = accessor.createPropertyControl(composite); 
					propertyControl.load(entry);
					propertyControls.add(propertyControl);

					propertyControl.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));					

					addFocusListenerRecursively(propertyControl.getControl(), controlFocusListener);
				}
			}

			@Override
			public void save() {
				for (IPropertyControl propertyControl: propertyControls) {
					propertyControl.save();
				}
			}

			private void addFocusListenerRecursively(Control control, FocusListener listener) {
				control.addFocusListener(listener);
				if (control instanceof Composite) {
					for (Control child: ((Composite)control).getChildren()) {
						addFocusListenerRecursively(child, listener);
					}
				}
			}
		};
	}

	public int compare(EntryData entryData1, EntryData entryData2) {
		if (entryData1.hasSplitEntries()) {
			if (entryData2.hasSplitEntries()) {
				return 0;
			} else {
				return 1;
			}
		} else {
			if (entryData2.hasSplitEntries()) {
				return -1;
			} else {
				Entry extendableObject1 = entryData1.getOtherEntry();
				Entry extendableObject2 = entryData2.getOtherEntry();
				return accessor.getComparator().compare(extendableObject1, extendableObject2);
			}
		}
	}
}