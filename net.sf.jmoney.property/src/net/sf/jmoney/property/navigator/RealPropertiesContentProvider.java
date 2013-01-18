package net.sf.jmoney.property.navigator;

import java.util.HashMap;
import java.util.Map;

import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.SessionChangeAdapter;
import net.sf.jmoney.isolation.SessionChangeListener;
import net.sf.jmoney.model2.CommodityInfo;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.property.model.RealProperty;
import net.sf.jmoney.property.model.RealPropertyInfo;
import net.sf.jmoney.property.views.RealPropertyTypeNode;
import net.sf.jmoney.views.IDynamicTreeNode;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

public class RealPropertiesContentProvider implements ITreeContentProvider {

	private DatastoreManager sessionManager;
	
	private Map<ExtendablePropertySet<? extends RealProperty>, Object> realPropertyTypeTreeNodes = new HashMap<ExtendablePropertySet<? extends RealProperty>, Object>();

	private SessionChangeListener listener = null;
	
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof DatastoreManager) {
			return realPropertyTypeTreeNodes.values().toArray();
		}
		if (parentElement instanceof IDynamicTreeNode) {
			return ((IDynamicTreeNode)parentElement).getChildren().toArray();
		}
		return new Object[0];
	}

	public Object getParent(Object element) {
		if (element instanceof IDynamicTreeNode) {
			return sessionManager;
		} else if (element instanceof RealProperty) {
			RealProperty securityElement = (RealProperty)element;
			ExtendablePropertySet propertySet = PropertySet.getPropertySet(securityElement.getClass());
			return realPropertyTypeTreeNodes.get(propertySet);
		}
		return null;  // Should never happen
	}

	public boolean hasChildren(Object element) {
		if (element instanceof DatastoreManager) {
			return true;
		}
		if (element instanceof IDynamicTreeNode) {
			return ((IDynamicTreeNode)element).hasChildren();
		}
		return false;
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {
		if (listener != null) {
			sessionManager.removeChangeListener(listener);
		}
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		/*
		 * The input never changes because the input to this viewer is the same as the input
		 * to the workbench page and the input to the workbench page can't change (we replace
		 * the page if the session changes).  Therefore no cleanup of the old input is needed.
		 */
		sessionManager = (DatastoreManager)newInput;
		if (sessionManager != null) {
			/*
			 * Create the nodes that contain the real property, one node for each
			 * class of real property.
			 */
			realPropertyTypeTreeNodes.clear();
			for (ExtendablePropertySet<? extends RealProperty> propertySet : RealPropertyInfo.getPropertySet().getDerivedPropertySets()) {
				realPropertyTypeTreeNodes.put(propertySet, new RealPropertyTypeNode(sessionManager, propertySet)); 
			}
		
			listener = new MyCurrentSessionChangeListener((TreeViewer)viewer);
			sessionManager.addChangeListener(listener);
		}
	}

	private class MyCurrentSessionChangeListener extends SessionChangeAdapter {
		private TreeViewer viewer;
		
		MyCurrentSessionChangeListener(TreeViewer viewer) {
			this.viewer = viewer;
		}
		
		@Override	
		public void objectInserted(IModelObject newObject) {
			if (newObject instanceof RealProperty) {
				RealProperty securityElement = (RealProperty)newObject;
				ExtendablePropertySet propertySet = PropertySet.getPropertySet(securityElement.getClass());
				Object securitiesTypeTreeNode = realPropertyTypeTreeNodes.get(propertySet);
				viewer.insert(securitiesTypeTreeNode, newObject, 0);
			}
		}

		@Override	
		public void objectRemoved(final IModelObject deletedObject) {
			if (deletedObject instanceof RealProperty) {
				/*
				 * This listener method is called before the object is deleted.
				 * This allows listener methods to access the deleted object and
				 * its position in the model.  However, this is too early to
				 * refresh views.  Therefore we must delay the refresh of the view
				 * until after the object is deleted.
				 */
				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {
						viewer.remove(deletedObject);
					}
				});
			}
		}

		@Override	
		public void objectChanged(IModelObject changedObject, IScalarPropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
			if (changedObject instanceof RealProperty
					&& propertyAccessor == CommodityInfo.getNameAccessor()) {
				viewer.update(changedObject, null);
			}
		}
	}

}
