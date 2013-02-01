package net.sf.jmoney.navigator;

import net.sf.jmoney.isolation.IListPropertyAccessor;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.SessionChangeAdapter;
import net.sf.jmoney.isolation.SessionChangeListener;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.AccountInfo;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.IDatastoreManager;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.views.AccountsNode;
import net.sf.jmoney.views.CategoriesNode;
import net.sf.jmoney.views.IDynamicTreeNode;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

public class AccountsContentProvider implements ITreeContentProvider {

	private IDataManagerForAccounts sessionManager;
	
	private Object accountsTreeNode;
	private Object categoriesTreeNode;

	private SessionChangeListener listener = null;
	
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IDatastoreManager) {
			return new Object[] {
					accountsTreeNode,
					categoriesTreeNode
			};
		}
		if (parentElement instanceof IDynamicTreeNode) {
			return ((IDynamicTreeNode)parentElement).getChildren().toArray();
		}
		if (parentElement instanceof Account) {
			return ((Account)parentElement).getSubAccountCollection().toArray();
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof IDynamicTreeNode) {
			return sessionManager;
		} else if (element instanceof Account) {
			Account parentAccount = ((Account)element).getParent();
			if (parentAccount == null) {
				return (element instanceof CapitalAccount) ? accountsTreeNode : categoriesTreeNode;
			} else {
				return parentAccount;
			}
		}
		return null;  // Should never happen
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof IDatastoreManager) {
			return true;
		}
		if (element instanceof IDynamicTreeNode) {
			return ((IDynamicTreeNode)element).hasChildren();
		}
		if (element instanceof CapitalAccount) {
			return !((CapitalAccount)element).getSubAccountCollection().isEmpty();
		}
		if (element instanceof IncomeExpenseAccount) {
			return !((IncomeExpenseAccount)element).getSubAccountCollection().isEmpty();
		}
		return false;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public void dispose() {
		if (listener != null) {
			sessionManager.removeChangeListener(listener);
		}
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		/*
		 * The input never changes because the input to this viewer is the same as the input
		 * to the workbench page and the input to the workbench page can't change (we replace
		 * the page if the session changes).  Therefore no cleanup of the old input is needed.
		 */
		sessionManager = (IDataManagerForAccounts)newInput;
		if (sessionManager != null) {
			// Create the two nodes that contain the accounts and categories in this session.
			accountsTreeNode = new AccountsNode(sessionManager); 
			categoriesTreeNode = new CategoriesNode(sessionManager);
			
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
			if (newObject instanceof Account) {
				Object parentElement = getParent(newObject);
				viewer.insert(parentElement, newObject, 0);
			}
		}

		@Override	
		public void objectRemoved(final IModelObject deletedObject) {
			if (deletedObject instanceof Account) {
				/*
				 * This listener method is called before the object is deleted.
				 * This allows listener methods to access the deleted object and
				 * its position in the model.  However, this is too early to
				 * refresh views.  Therefore we must delay the refresh of the view
				 * until after the object is deleted.
				 */
				Display.getCurrent().asyncExec(new Runnable() {
					@Override
					public void run() {
						viewer.remove(deletedObject);
					}
				});
			}
		}

		@Override	
		public void objectChanged(IModelObject changedObject, IScalarPropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
			if (changedObject instanceof Account
					&& propertyAccessor == AccountInfo.getNameAccessor()) {
				viewer.update(changedObject, null);
			}
		}

		@Override	
		public void objectMoved(IModelObject movedObject,
				IModelObject originalParent, IModelObject newParent,
				IListPropertyAccessor originalParentListProperty,
				IListPropertyAccessor newParentListProperty) {
			if (originalParent instanceof Session || newParent instanceof Session) {
				if (movedObject instanceof CapitalAccount) {
					viewer.refresh(accountsTreeNode, false);
				} else if (movedObject instanceof IncomeExpenseAccount) {
					viewer.refresh(categoriesTreeNode, false);
				}
			}
			if (originalParent instanceof Account) {
				viewer.refresh(originalParent, false);
			}
			if (newParent instanceof Account) {
				viewer.refresh(newParent, false);
			}
		}
	}

}
