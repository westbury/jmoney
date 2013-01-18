package net.sf.jmoney.navigator;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.resources.Messages;
import net.sf.jmoney.views.AccountsNode;
import net.sf.jmoney.views.CategoriesNode;
import net.sf.jmoney.views.TreeNode;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.navigator.IDescriptionProvider;

public class AccountsLabelProvider extends LabelProvider implements
		ILabelProvider, IDescriptionProvider {

	@Override
	public Image getImage(Object element) {
		if (element instanceof TreeNode) {
			return ((TreeNode) element).getImage();
		} else if (element instanceof AccountsNode) {
			return JMoneyPlugin.createImage("icons/accounts.gif"); //$NON-NLS-1$
		} else if (element instanceof CategoriesNode) {
			return JMoneyPlugin.createImage("icons/category.gif"); //$NON-NLS-1$
		} else if (element instanceof ExtendableObject) {
			ExtendableObject extendableObject = (ExtendableObject) element;
			return PropertySet.getPropertySet(extendableObject.getClass())
					.getIconImage();
		} else {
			throw new RuntimeException(Messages.AccountsLabelProvider_Image);
		}
	}

	@Override
	public String getText(Object element) {
		if (element instanceof TreeNode) {
			return ((TreeNode) element).getLabel();
		} else if (element instanceof AccountsNode) {
			return Messages.NavigationTreeModel_accounts;
		} else if (element instanceof CategoriesNode) {
			return Messages.NavigationTreeModel_categories;
		} else if (element instanceof ExtendableObject) {
			return ((ExtendableObject) element).toString();
		}
		return Messages.AccountsLabelProvider_DefaultText;
	}

	@Override
	public String getDescription(Object element) {
		if (element instanceof TreeNode) {
			return Messages.AccountsLabelProvider_TreeNodeDescription;
		}
		if (element instanceof CapitalAccount) {
			return NLS.bind(
					Messages.AccountsLabelProvider_CapitalAccountDescription,
					((CapitalAccount) element).getName());
		}
		if (element instanceof IncomeExpenseAccount) {
			return NLS
					.bind(
							Messages.AccountsLabelProvider_IncomeExpenseAccountDescription,
							((IncomeExpenseAccount) element)
									.getFullAccountName());
		}
		if (element instanceof AccountsNode) {
			return Messages.NavigationTreeModel_accounts;
		}
		if (element instanceof CategoriesNode) {
			return Messages.NavigationTreeModel_categories;
		}
		return Messages.AccountsLabelProvider_DefaultDescription;
	}

}
