package net.sf.jmoney.associations.propertyPages;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.value.ValueProperty;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;


public class AssociatedAccountProperty extends ValueProperty<CapitalAccount, Account> {

	final String associationId;

	public AssociatedAccountProperty(String associationId) {
		this.associationId = associationId;
	}
	
	@Override
	public Object getValueType() {
		return Account.class;
	}

	@Override
	public IObservableValue<Account> observe(Realm realm, final CapitalAccount source) {
		
		IObservableValue<Account> observable = new AssociatedAccountObservable(associationId, source);
		return observable;
	}

}
