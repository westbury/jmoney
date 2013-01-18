package net.sf.jmoney.property.navigator;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.property.model.RealProperty;
import net.sf.jmoney.property.resources.Messages;
import net.sf.jmoney.property.views.RealPropertyTypeNode;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.navigator.IDescriptionProvider;

public class RealPropertyLabelProvider extends LabelProvider implements ILabelProvider, IDescriptionProvider {

	@Override	
	public Image getImage(Object element) {
		if (element instanceof ExtendableObject) {
			ExtendableObject extendableObject = (ExtendableObject)element;
			return PropertySet.getPropertySet(extendableObject.getClass()).getIconImage();
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof RealPropertyTypeNode) {
			return ((RealPropertyTypeNode)element).getLabel();
		}

		
		if (element instanceof ExtendableObject) {
			return ((ExtendableObject)element).toString();
		}
		
		// We should not get here.
		return "Unnamed Node"; //$NON-NLS-1$
	}

	public String getDescription(Object element) {
		if (element instanceof RealProperty) {
			return NLS.bind(Messages.NavigationTree_realPropertyDescription,((RealProperty)element).getName());
		}
		return "";
	}

}
