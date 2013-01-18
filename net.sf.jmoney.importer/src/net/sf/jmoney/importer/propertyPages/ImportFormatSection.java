package net.sf.jmoney.importer.propertyPages;

import net.sf.jmoney.importer.model.ImportAccountInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertySource;
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

	private Account account;

	private ModifyListener listener = new ModifyListener() {

		public void modifyText(ModifyEvent arg0) {
			IPropertySource properties = (IPropertySource) account
			.getAdapter(IPropertySource.class);
			//            properties.setPropertyValue(IPropertySource.PROPERTY_TEXT,
			//                labelText.getText());
		}
	};

	private IPropertyControl<ExtendableObject> propertyControl;

	public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);
		Composite composite = getWidgetFactory()
		.createFlatFormComposite(parent);
		FormData data;

		propertyControl = ImportAccountInfo.getImportDataExtensionIdAccessor().createPropertyControl(composite);
		data = new FormData();
		data.left = new FormAttachment(0, STANDARD_LABEL_WIDTH);
		data.right = new FormAttachment(100, 0);
		data.top = new FormAttachment(0, ITabbedPropertyConstants.VSPACE);
		propertyControl.getControl().setLayoutData(data);
		((Combo)propertyControl.getControl()).addModifyListener(listener);

		CLabel label = getWidgetFactory()
		.createCLabel(composite, "Label:"); //$NON-NLS-1$
		data = new FormData();
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(propertyControl.getControl(),
				-ITabbedPropertyConstants.HSPACE);
		data.top = new FormAttachment(propertyControl.getControl(), 0, SWT.CENTER);
		label.setLayoutData(data);
	}

	public void setInput(IWorkbenchPart part, ISelection selection) {
		super.setInput(part, selection);
		Object input = ((IStructuredSelection) selection).getFirstElement();
		this.account = (Account) input;
	}

	public void refresh() {
		Combo control = (Combo)propertyControl.getControl();
		control.removeModifyListener(listener);
		propertyControl.load(account);
        control.addModifyListener(listener);
	}
}

