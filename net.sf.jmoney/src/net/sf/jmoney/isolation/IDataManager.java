package net.sf.jmoney.isolation;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.swt.widgets.Control;

public interface IDataManager {

	void addChangeListener(SessionChangeListener listener);

	void removeChangeListener(SessionChangeListener listener);

	void addChangeListenerWeakly(SessionChangeListener listener);

	void addChangeListener(final SessionChangeListener listener, Control control);

	/**
	 * This method is called when a transaction is about to start.
	 * <P>
	 * If the datastore is kept in a transactional database then the code
	 * needed to start a transaction should be put in the implementation
	 * of this method.
	 * <P>
	 * The framework will always call this method, then make changes to
	 * the datastore, then call <code>commitTransaction</code> within
	 * a single function call.  The framework also ensures that no events
	 * are fired between the call to <code>startTransaction</code> and
	 * the call to <code>commitTransaction</code>.  The implementation of
	 * this method thus has no need to support or guard against nested
	 * transactions.
	 * 
	 * @see commitTransaction
	 */
	public abstract void startTransaction();

    /**
	 * Send change notifications to all listeners who are listening for changes
	 * to the version of the datastore as seen through this data manager.
	 * <P>
	 * In practice it is likely that the only listener will be the JMoneyPlugin
	 * object. Views should all listen to the JMoneyPlugin class for changes to
	 * the model. The JMoneyPlugin object will pass on events from this session
	 * object.
	 * <P>
	 * Listeners may register directly with a session object. However if they do
	 * so then they must re-register whenever the session object changes. If a
	 * viewer wants to listen for changes to a session even if that session is
	 * not the session currently shown in the workbench then it should register
	 * with the session object, but if the viewer wants to be told about changes
	 * to the current workbench window then it should register with the
	 * JMoneyPlugin object.
	 * 
	 * This method is public because the layer above this data manager is
	 * responsible for calling this method. Only the above layer (code outside
	 * the data manager) knows, for example, when objectInserted should be
	 * called.
	 */
    void fireEvent(ISessionChangeFirer firer);
    
	/**
	 * This method is called when a transaction is to be committed.
	 * <P>
	 * If the datastore is kept in a transactional database then the code
	 * needed to commit the transaction should be put in the implementation
	 * of this method.
	 * 
	 * @see startTransaction
	 */
	public abstract void commitTransaction();

	IUndoContext getUndoContext();

	ChangeManager getChangeManager();
}
