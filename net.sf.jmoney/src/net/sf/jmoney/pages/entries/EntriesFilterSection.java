/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Johann Gyger <jgyger@users.sf.net>
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

import java.util.Collection;

import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.resources.Messages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class EntriesFilterSection extends SectionPart {

    protected Combo fFilterCombo;
    protected Combo fFilterTypeCombo;
    protected Combo fOperationCombo;
    protected Text fFilterText;
	private EntriesFilter filter;
	private Collection<IndividualBlock> allEntryDataObjects;

	public EntriesFilterSection(Composite parent, EntriesFilter filter, Collection<IndividualBlock> allEntryDataObjects, FormToolkit toolkit) {
        super(parent, toolkit, Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE);
        this.filter = filter;
        this.allEntryDataObjects = allEntryDataObjects;
        
        getSection().addExpansionListener(new ExpansionAdapter() {
    	    @Override	
    		public void expansionStateChanged(ExpansionEvent e) {
    			// TODO warn the user if the section is being collapsed
    			// while a filter is in effect.  Alternatively, we can
    			// show some other indication that the entries list
    			// is filtered.  If we don't do anything, the user may
    			// get confused because entries are not being shown.
    		}
    	});
    	getSection().setText(Messages.EntriesFilterSection_Text);
        getSection().setDescription(Messages.EntriesFilterSection_Description);
		createClient(toolkit);
	}

	protected void createClient(FormToolkit toolkit) {
        Composite container = toolkit.createComposite(getSection());

        GridLayout layout = new GridLayout();
        layout.numColumns = 4;
        container.setLayout(layout);

        fFilterCombo = new Combo(container, toolkit.getBorderStyle() | SWT.READ_ONLY);
        toolkit.adapt(fFilterCombo, true, true);
        String[] fFilterComboItems = { Messages.EntriesFilterSection_ComboItemFilter,
                Messages.EntriesFilterSection_ComboItemClear};
        fFilterCombo.setItems(fFilterComboItems);
        fFilterCombo.select(0);
        fFilterCombo.addSelectionListener(new SelectionAdapter() {
    	    @Override	
			public void widgetSelected(SelectionEvent e) {
				if (fFilterCombo.getSelectionIndex() == 1) {
					fFilterText.setText(""); //$NON-NLS-1$
					filter.setPattern(""); //$NON-NLS-1$
					// always leave selection at 'filter' (this combo is acting as a push button)
					fFilterCombo.select(0);
				}
			}
        });

        // Build an array of the localized names of the properties
        // on which a filter may be based.
        String[] filterTypes = new String[allEntryDataObjects.size() + 1];
        int i = 0;
        filterTypes[i++] = Messages.EntriesFilterSection_Entry; 
        for (IndividualBlock entriesSectionProperty: allEntryDataObjects) {
            filterTypes[i++] = entriesSectionProperty.getText();
        }
        
        fFilterTypeCombo = new Combo(container, toolkit.getBorderStyle() | SWT.READ_ONLY);
        toolkit.adapt(fFilterTypeCombo, true, true);
        fFilterTypeCombo.setItems(filterTypes);
        fFilterTypeCombo.select(0);
        fFilterTypeCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				filter.setType(fFilterTypeCombo.getSelectionIndex());
			}
        });
        
        fOperationCombo = new Combo(container, toolkit.getBorderStyle() | SWT.READ_ONLY);
        toolkit.adapt(fOperationCombo, true, true);
        String[] fOperationComboItems = { Messages.EntriesFilterSection_ComboItemContains,};
        fOperationCombo.setItems(fOperationComboItems);
        fOperationCombo.select(0);

        fFilterText = toolkit.createText(container, ""); //$NON-NLS-1$
        fFilterText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        fFilterText.addFocusListener(new FocusAdapter() {
		    @Override	
			public void focusLost(FocusEvent e) {
				filter.setPattern(fFilterText.getText());
			}
        });

        getSection().setClient(container);
        toolkit.paintBordersFor(container);
        refresh();
    }
}
