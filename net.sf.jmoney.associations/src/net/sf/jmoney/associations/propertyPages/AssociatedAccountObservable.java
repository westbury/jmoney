package net.sf.jmoney.associations.propertyPages;

import net.sf.jmoney.associations.model.AccountAssociation;
import net.sf.jmoney.associations.model.AccountAssociationInfo;
import net.sf.jmoney.associations.model.AccountAssociationsInfo;
import net.sf.jmoney.isolation.IListPropertyAccessor;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.IScalarPropertyAccessor;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.isolation.SessionChangeAdapter;
import net.sf.jmoney.isolation.SessionChangeListener;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.TransactionManagerForAccounts;

import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.ValueDiff;
import org.eclipse.core.runtime.Assert;

final class AssociatedAccountObservable extends
		AbstractObservableValue<Account> {
	/**
	 * 
	 */
	private final String associationId;
	private final CapitalAccount source;
	private final SessionChangeListener listener;

	AssociatedAccountObservable(final String associationId, final CapitalAccount source) {
		this.associationId = associationId;
		this.source = source;

		Assert.isNotNull(associationId);
		if (associationId == null) {
			throw new RuntimeException();
		}
		
		listener = new SessionChangeAdapter() {

			@Override
			public void objectCreated(IModelObject newObject) {
				if (newObject instanceof AccountAssociation) {
					final AccountAssociation aa = (AccountAssociation)newObject;
					/*
					 * Note that model objects may be created property by
					 * property, so when associatedAccount object is created it
					 * will not initially have its id set.
					 */
					if (aa.getParentKey().getObject() == source
							&& associationId.equals(aa.getId())) {
						fireValueChange(new ValueDiff<Account>() {
							public Account getOldValue() {
								return null;
							}
							public Account getNewValue() {
								return aa.getAccount();
							}
						});
					}
				}
			}

			@Override
			public void objectDestroyed(IModelObject deletedObject) {
				if (deletedObject instanceof AccountAssociation) {
					final AccountAssociation aa = (AccountAssociation)deletedObject;
					if (aa.getParentKey().getObject() == source
							&& associationId.equals(aa.getId())) {
						fireValueChange(new ValueDiff<Account>() {
							public Account getOldValue() {
								return aa.getAccount();
							}
							public Account getNewValue() {
								return null;
							}
						});
					}
				}
			}

			@Override
			public void objectChanged(IModelObject changedObject,
					IScalarPropertyAccessor changedProperty, final Object oldValue,
					final Object newValue) {
				if (changedObject instanceof AccountAssociation) {
					AccountAssociation aa = (AccountAssociation)changedObject;
					/*
					 * Note that model objects may be created property by
					 * property, so when associatedAccount object is created it
					 * will not initially have its id set.
					 */
					if (aa.getParentKey().getObject() == source
							&& associationId.equals(aa.getId())) {
						fireValueChange(new ValueDiff<Account>() {
							public Account getOldValue() {
								return (Account)oldValue;
							}
							public Account getNewValue() {
								return (Account)newValue;
							}
						});
					}
				}
			}

			@Override
			public void objectMoved(IModelObject movedObject,
					IModelObject originalParent, IModelObject newParent,
					IListPropertyAccessor originalParentListProperty,
					IListPropertyAccessor newParentListProperty) {
				/*
				 * This does not happen and nor is it likely to happen.  However the plug-in development
				 * rules would allow someone to write a plug-in that moves an association so technically
				 * it should be supported.
				 */
			}
		};
		
		
		source.getDataManager().addChangeListener(listener);
		
	}

	@Override
	public Object getValueType() {
		return Account.class;
	}

	@Override
	public Class<Account> getValueClass() {
		return Account.class;
	}

	@Override
	protected Account doGetValue() {
		ObjectCollection<AccountAssociation> associations = AccountAssociationsInfo.getAssociationsAccessor().getElements(source);
		for (AccountAssociation association : associations) {
			if (associationId.equals(association.getId())) {
				return association.getAccount();
			}
		}
		return null;
	}

	protected void doSetValue(Account account) {
		ObjectCollection<AccountAssociation> associations = AccountAssociationsInfo.getAssociationsAccessor().getElements(source);
		AccountAssociation match = null;
		for (AccountAssociation association : associations) {
			if (association.getId().equals(associationId)) {
				match = association;
				break;
			}
		}
		
		if (account == null) {
			if (match == null) {
				throw new RuntimeException("Should not get here if no change in value");
			}
			try {
				associations.deleteElement(match);
			} catch (ReferenceViolationException e) {
				/*
				 * The only way we can get here is if an unknown plug-in were to extend the database
				 * and create a foreign key to the account associations.  This is really not likely.
				 */
				throw new RuntimeException(e);
			}
		} else {
			if (match == null) {
				/*
				 * Let's do this in a transaction so we don't risk leaving the data model
				 * in a bad state and others don't see incomplete transactions in the
				 * listeners.
				 */
				
				TransactionManagerForAccounts transactionManager = new TransactionManagerForAccounts(source.getDataManager());
				CapitalAccount source2 = transactionManager.getCopyInTransaction(source);
				Account account2 = transactionManager.getCopyInTransaction(account);
				
				ObjectCollection<AccountAssociation> associations2 = AccountAssociationsInfo.getAssociationsAccessor().getElements(source2);
				AccountAssociation match2 = associations2.createNewElement(AccountAssociationInfo.getPropertySet());
				match2.setId(associationId);
				match2.setAccount(account2);
				
				transactionManager.commit("Add New Account Association");
			} else {
				match.setAccount(account);
			}
		}
	}

	/**
	 * 
	 */
	public synchronized void dispose() {
		if (!isDisposed()) {
			source.getDataManager().removeChangeListener(listener);
			super.dispose();
		}
	}
}