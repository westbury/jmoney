package net.sf.jmoney.associations;

import net.sf.jmoney.model2.Account;

public interface IAssociatedAccountInfoProvider {

	/**
	 * 
	 * @param account
	 * @return
	 * @trackedGetter implementations should ideally get values that
	 * 		may change through an observable or a call to a tracked
	 * 		getter.  This ensures that the list of associated accounts
	 * 		shown to the user will be updated if anything changes.  
	 */
	AssociationMetadata[] getAssociationMetadata(Account account);

	/**
	 * A name for the set of account associations defined by this extension.
	 * This name is used for grouping associations in the properties tab.
	 * 
	 * @return a short user readable description, all major words in being
	 * 			capitalized 
	 */
	String getGroupName(Account account);
}
 