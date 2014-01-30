package net.sf.jmoney.stocks;

import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.stocks.model.Security;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
public class MergeDuplicatedSecurityDialog<S extends Security> extends Dialog {

	private ExtendablePropertySet<S> propertySet;

	private final S security1;

	private final S security2;

	protected MergeDuplicatedSecurityDialog(Shell parentShell, ExtendablePropertySet<S> propertySet, S security1, S security2) {
		super(parentShell);
		this.propertySet = propertySet;
		this.security1 = security1;
		this.security2 = security2;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			/*
			 * Merge list properties.  This is a very simple implementation because securities don't
			 * currently have lists except in an experimental plug-in that keeps price history.
			 * The caller gives an error if the same property has a non-empty list in both security
			 * objects.  This code here just uses whichever list is not empty.
			 */
			for (ListPropertyAccessor<?,? super S> property : propertySet.getListProperties3()) {
				mergeChildList(property);
			}
		}

		super.buttonPressed(buttonId);
	}

	private <C extends ExtendableObject> void mergeChildList(ListPropertyAccessor<C,? super S> property) {
		ObjectCollection<C> objectCollection = property.getElements(security1);
		if (objectCollection.isEmpty()) {
			for (C child : property.getElements(security2)) {
				objectCollection.moveElement(child);
			}
		}
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Merge Two Securities");
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		Label label = new Label(composite, SWT.WRAP);
		label.setText("Select the property values:");

		GridData messageData = new GridData();
		Rectangle rect = getShell().getMonitor().getClientArea();
		messageData.widthHint = rect.width/4;
		label.setLayoutData(messageData);

		createPropertiesArea(composite);

		/*
		 * Set values in Security1 to initial values
		 */
		for (ScalarPropertyAccessor<?, ? super S> accessor : propertySet.getScalarProperties3()) {
			setDefaultValue(accessor);
		}

		applyDialogFont(composite);
		return composite;
	}

	/**
	 * For the given property, sets the default value of the merged security.
	 *
	 * @param propertyAccessor
	 */
	private <V> void setDefaultValue(
			ScalarPropertyAccessor<V, ? super S> propertyAccessor) {
		V value1 = propertyAccessor.getValue(security1);
		V value2 = propertyAccessor.getValue(security2);
		if (value2 != null) {
			if (value1 == null) {
				propertyAccessor.setValue(security1, value2);
			} else if (!value1.equals(value2)) {
				propertyAccessor.setValue(security1, null);
			}
		}
	}

	private Control createPropertiesArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(4, false));

		for (ScalarPropertyAccessor<?, ? super S> accessor : propertySet.getScalarProperties3()) {
			Label label = new Label(composite, SWT.LEFT);
			label.setText(accessor.getDisplayName() + ":"); //$NON-NLS-1$

			Control propertyControl = accessor.createPropertyControl(composite, security1);
			propertyControl.setLayoutData(new GridData(accessor.getMinimumWidth() + accessor.getWeight() * 50, SWT.DEFAULT));

			Label value1Label = new Label(composite, SWT.LEFT);
			value1Label.setText(accessor.formatValueForTable(security1));

			Label value2Label = new Label(composite, SWT.LEFT);
			value2Label.setText(accessor.formatValueForTable(security2));
		}

		return composite;
	}

	protected void setErrorMessage(String string) {
		// TODO:

	}
}
