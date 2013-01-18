package net.sf.jmoney;

import net.sf.jmoney.isolation.TransactionManager;

/**
 * Many of the views in JMoney allow the user to edit the data without
 * restriction. This causes a problem because the user may edit data in such a
 * way that it is inconsistent with what a plug-in may expect.
 * <P>
 * For example, consider the plug-in that reconciles bank statements. Once a
 * user has reconciled a bank statement, it is marked as such. Now suppose the
 * user goes into the account entries view and edits the amount for an entry
 * that has been reconciled. The bank statement will be marked as reconciled but
 * in fact the balance will no longer match the bank's total.
 * <P>
 * The solution to this problem is to notify plug-ins of changes and allow them
 * to either give warnings to the user to veto the change.
 * <P>
 * A single call is made to each extension for each datastore transaction that
 * is committed. This allows the extension to consider the transaction as a
 * whole. For example, if one reconciled entry were deleted and two new
 * reconciled entries were inserted that add to the same amount then 
 * the reconciliation plug-in could decide to allow the change.
 * <P>
 * This validator may veto the change completely, or may give a warning and
 * allow the user to override or cancel the changes.  Many change
 * validators may be registered and all must be considered equal.  Therefore,
 * all validators are always called.  If any veto the change then a dialog box
 * showing a list of all the error messages (one from each validator that
 * vetoed the change) will be shown.  If none vetoed the change but one or
 * more gave a warning then a dialog box showing a list of all the warning messages
 * will be shown.
 *  
 * @author Nigel Westbury
 */
public interface IChangeValidator {
 
	interface IResponse {
		void warn(String message);
		void veto(String message);
	}
	
	void validate(TransactionManager transaction, IResponse response);
}
