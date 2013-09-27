package net.sf.jmoney.importer.propertyPages;

import net.sf.jmoney.importer.model.ImportAccountInfo;
import net.sf.jmoney.model2.CapitalAccount;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertyConstants;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * A section in the properties tab that allows the user to select
 * the format of the file that is used when importing a file into
 * a given account.
 * 
 * The input to this section is always an account.  Typically this
 * section will be followed by a section that contains the extra
 * account properties needed by the type of import.  A filter is used
 * on the following sections so the user will see the section change
 * as the selection in this section is changed.
 */

public class ImportFormatSection extends AbstractPropertySection {

	private IObservableValue<CapitalAccount> account = new WritableValue<CapitalAccount>();

	public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);
		Composite composite = getWidgetFactory()
		.createFlatFormComposite(parent);
		FormData data;

		Control propertyControl = ImportAccountInfo.getImportDataExtensionIdAccessor().createPropertyControl2(composite, account);
		data = new FormData();
		data.left = new FormAttachment(0, STANDARD_LABEL_WIDTH);
		data.right = new FormAttachment(100, 0);
		data.top = new FormAttachment(0, ITabbedPropertyConstants.VSPACE);
		propertyControl.setLayoutData(data);

		CLabel label = getWidgetFactory()
		.createCLabel(composite, "Label:"); //$NON-NLS-1$
		data = new FormData();
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(propertyControl,
				-ITabbedPropertyConstants.HSPACE);
		data.top = new FormAttachment(propertyControl, 0, SWT.CENTER);
		label.setLayoutData(data);
	}

	public void setInput(IWorkbenchPart part, ISelection selection) {
		super.setInput(part, selection);
		Object input = ((IStructuredSelection) selection).getFirstElement();
		this.account.setValue((CapitalAccount) input);
	}

	public void refresh() {
		// What do we do here?
	}
}

