package net.sf.jmoney.importer.matcher;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.jface.internal.databinding.provisional.swt.UpdatingComposite;
import org.eclipse.swt.widgets.Control;

import net.sf.jmoney.fields.AccountControl;
import net.sf.jmoney.importer.model.MemoPattern;
import net.sf.jmoney.importer.model.MemoPatternInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Session;

public class TransactionParamMetadataAccount extends TransactionParamMetadata {

	Class<? extends Account> classOfAccount;
	
	public TransactionParamMetadataAccount(String id, String name, Class<? extends Account> classOfAccount) {
		super(id, name);
		this.classOfAccount = classOfAccount;
	}

	@Override
	public Control createControl(UpdatingComposite parent, final IObservableValue<MemoPattern> memoPattern, IObservableValue<String[]> args) {
		final AccountControl textbox = new AccountControl(parent, classOfAccount) {
			@Override
			protected Session getSession() {
				return memoPattern.getValue().getSession();
			}
		};

		/*
		 * We can access observables here because getter tracking is turned off.  However we
		 * can't create computed values.
		 */
		IObservableValue<Account> parameterValueProperty = MemoPatternInfo.getAccountAccessor().observeDetail(memoPattern);
		Bind.twoWay(parameterValueProperty).to(textbox.account);

		return textbox;
	}

	public Account obtainAccount(MemoPattern pattern) {
		// TODO Support multiple accounts in a pattern?
		// Currently we're not looking at the id but just returning the account
		// in the account field for the pattern.
		return pattern.getAccount();
	}

	@Override
	public String getResolvedValueAsString(PatternMatch match) {
		Account account = obtainAccount(match.pattern);
		return account == null ? "" : account.getName();
	}
}
