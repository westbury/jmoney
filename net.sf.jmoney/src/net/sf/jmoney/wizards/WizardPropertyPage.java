/**
 * 
 */
package net.sf.jmoney.wizards;

import java.text.MessageFormat;
import java.util.Set;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * A wizard page that contains edit controls for all the properties
 * of a given extendable object.
 * 
 * @author Nigel Westbury
 */
public class WizardPropertyPage<S extends ExtendableObject> extends WizardPage {
	private S extendableObject;
	private ExtendablePropertySet<S> propertySet;
	private ScalarPropertyAccessor<String,?> namePropertyAccessor;
	private String defaultName;
	
	/**
	 * List of the IPropertyControl objects for the
	 * properties that can be edited in this panel.
	 */
//	private Map<ScalarPropertyAccessor, IPropertyControl> propertyControlList = new HashMap<ScalarPropertyAccessor, IPropertyControl>();

	Text nameTextbox;
	
	private Set<ScalarPropertyAccessor<?,? extends S>> excludedProperties;
	
	public WizardPropertyPage(
			String pageName, 
			String title, 
			String message, 
			S extendableObject, 
			ExtendablePropertySet<S> propertySet,
			ScalarPropertyAccessor<String,? super S> namePropertyAccessor, 
			Set<ScalarPropertyAccessor<?,? extends S>> excludedProperties) {
		super(pageName);
		this.extendableObject = extendableObject;
		this.propertySet = propertySet;
		this.namePropertyAccessor = namePropertyAccessor;
		this.excludedProperties = excludedProperties;
		
		setTitle(title);
		setMessage(message);
	}
	
//	@SuppressWarnings("unchecked")  // Don't suppress this one, the warning is legitimate
	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		
		// Create the controls to edit the properties.
		
		// Add the properties for the Account objects.
		for (final ScalarPropertyAccessor<?,? super S> propertyAccessor: propertySet.getScalarProperties3()) {
			if (!excludedProperties.contains(propertyAccessor)) {
				Label propertyLabel = new Label(container, SWT.NONE);
				propertyLabel.setText(propertyAccessor.getDisplayName() + ':');
				Control propertyControl = propertyAccessor.createPropertyControl(container, extendableObject);

				// Bit of a kludge.  We have special processing for the account
				// name, so save this one.
				if (propertyAccessor == namePropertyAccessor) {
					nameTextbox = (Text)propertyControl;
				}

				/*
				 * If the control factory set up grid data then leave it
				 * alone. Otherwise set up the grid data based on the
				 * properties minimum sizes and expansion weights. <P> The
				 * control widths are set to the minimum width plus 10 times
				 * the expansion weight. (As we are not short of space, we
				 * make them a little bigger than their minimum sizes). A
				 * minimum of 100 pixels is then applied because this makes
				 * the right sides of the smaller controls line up, which
				 * looks a little more tidy.
				 */  
				if (propertyControl.getLayoutData() == null) {
					GridData gridData = new GridData();
					gridData.minimumWidth = propertyAccessor.getMinimumWidth();
					gridData.widthHint = Math.max(propertyAccessor.getMinimumWidth() + 10 * propertyAccessor.getWeight(), 100);
					propertyControl.setLayoutData(gridData);
				}

				propertyControl.addFocusListener(
						new FocusAdapter() {
							@Override	
							public void focusLost(FocusEvent e) {
								// TODO: Verify this is needed.  Clean it up?
//								if (extendableObject.getDataManager().isSessionFiring()) {
//									return;
//								}

//								propertyControl.save();
							}
						});

				// Add to our list of controls.
//				propertyControlList.put(propertyAccessor, propertyControl);

				Bind.oneWay(new ComputedValue<Boolean>() {
					@Override
					protected Boolean calculate() {
						return propertyAccessor.isPropertyApplicable(extendableObject);
					}
				}).to(PojoProperties.value("enabled", Boolean.class), propertyControl);
			}
		
		}
		// Set the values from the object into the control fields.
//		for (IPropertyControl propertyControl: propertyControlList.values()) {
//			propertyControl.load(extendableObject);
//		}
		//After the load i update the defaultName
		defaultName = nameTextbox.getText();
//		setApplicability();
//		
//		// This listener code assumes that the applicability of account properties depends only
//		// on property changes???????
//		extendableObject.getDataManager().addChangeListener(new SessionChangeAdapter() {
//			@Override
//			public void objectChanged(IModelObject changedObject, IScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
//				setApplicability();
//			}
//		}, parent);



		
		
		setPageComplete(false);
		nameTextbox.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setPageComplete(nameTextbox.getText().length() != 0);
			}
		});
		
		nameTextbox.setFocus();
		
		setControl(container);
		
	}
	
//	private void setApplicability() {
//		for (ScalarPropertyAccessor propertyAccessor: propertyControlList.keySet()) {
//			IPropertyControl propertyControl = propertyControlList.get(propertyAccessor);
//			propertyControl.getControl().setEnabled(propertyAccessor.isPropertyApplicable(extendableObject));
//		}
//	}

	@Override
	public boolean canFlipToNextPage() {
		/*
		 * This method controls whether the 'Next' button is enabled.
		 */
		boolean result = true;
		if(defaultName.equalsIgnoreCase(nameTextbox.getText())){
			setErrorMessage(MessageFormat.format(Messages.WizardPropertyPage_ErrorSameAsDefaultName,defaultName));
			result = false;
		}else if(nameTextbox.getText().length() == 0){
			setErrorMessage(Messages.WizardPropertyPage_ErrorNameEmpty);
			result = false;
		}
		if(result){
			setErrorMessage(null);
		}
		return result;
	}
	
	@Override
	public void performHelp() {
		PlatformUI.getWorkbench().getHelpSystem().displayHelp("net.sf.jmoney.newAccountWizardId"); //$NON-NLS-1$
	}
}