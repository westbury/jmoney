/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package net.sf.jmoney.propertyPages;

import java.util.HashMap;
import java.util.Map;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

public class PropertiesSection
extends AbstractPropertySection {

	private Map<ScalarPropertyAccessor, IPropertyControl> labelTexts;

	private ExtendableObject extendableObject;

	private ModifyListener listener = new ModifyListener() {

		@Override
		public void modifyText(ModifyEvent arg0) {
			IPropertySource properties = (IPropertySource) extendableObject
			.getAdapter(IPropertySource.class);
			//            properties.setPropertyValue(IPropertySource.PROPERTY_TEXT,
			//                labelText.getText());
		}
	};

	private Composite composite;

	@Override
	public void createControls(Composite parent,
			TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);
		composite = getWidgetFactory().createComposite(parent);
		composite.setLayout(new GridLayout(2, false));
	}

	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
		super.setInput(part, selection);
		Object input = ((IStructuredSelection) selection).getFirstElement();
		this.extendableObject = (ExtendableObject) input;

		labelTexts = new HashMap<ScalarPropertyAccessor, IPropertyControl>();

		for (ScalarPropertyAccessor propertyAccessor : PropertySet.getPropertySet(extendableObject.getClass()).getScalarProperties3()) {
			IPropertyControl labelText = propertyAccessor.createPropertyControl(composite);
			labelTexts.put(propertyAccessor, labelText);

//			labelText.getControl().addModifyListener(listener);

			CLabel labelLabel = getWidgetFactory()
			.createCLabel(composite, propertyAccessor.getDisplayName() + ":"); //$NON-NLS-1$
		}
	}

	@Override
	public void refresh() {
		for (ScalarPropertyAccessor association : labelTexts.keySet()) {
			IPropertyControl labelText = labelTexts.get(association);

//			labelText.removeModifyListener(listener);
			IPropertySource properties = (IPropertySource) extendableObject
			.getAdapter(IPropertySource.class);
			labelText.load(extendableObject);
//			labelText.addModifyListener(listener);
		}
	}
}

