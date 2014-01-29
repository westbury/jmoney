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

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

public class PropertiesSection
extends AbstractPropertySection {

	private ExtendableObject extendableObject;

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
		if (input instanceof ExtendableObject) {
			this.extendableObject = (ExtendableObject) input;

			for (ScalarPropertyAccessor propertyAccessor : PropertySet.getPropertySet(extendableObject.getClass()).getScalarProperties3()) {

				getWidgetFactory().createCLabel(composite, propertyAccessor.getDisplayName() + ":"); //$NON-NLS-1$

				Control labelText = propertyAccessor.createPropertyControl(composite, extendableObject);
			}
		}
	}

	@Override
	public void refresh() {
//		for (ScalarPropertyAccessor association : labelTexts.keySet()) {
//			IPropertyControl labelText = labelTexts.get(association);
//
//			IPropertySource properties = (IPropertySource) extendableObject
//			.getAdapter(IPropertySource.class);
//			labelText.load(extendableObject);
//		}
	}
}

